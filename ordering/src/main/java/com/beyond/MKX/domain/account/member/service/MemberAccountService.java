package com.beyond.MKX.domain.account.member.service;

import com.beyond.MKX.domain.account.member.client.AccountListClient;
import com.beyond.MKX.domain.account.member.dto.AccountListRegisterReq;
import com.beyond.MKX.domain.account.member.dto.AccountStatusUpdateReq;
import com.beyond.MKX.domain.account.member.util.MemberAccountNumberGenerator;
import com.beyond.MKX.domain.assets.entity.AccountStatus;
import com.beyond.MKX.domain.assets.entity.MemberAccount;
import com.beyond.MKX.domain.assets.repository.MemberAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * 회원 계좌 서비스
 * <p>
 * 책임
 * - 회원 계좌 생성/상태 변경/조회/입출금
 * - 생성 후 mkx-platform(account_list)에 MEMBER 유형으로 메타 등록 요청(Feign)
 * - 계좌번호 충돌 회피(여러 번 시도)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberAccountService {

    private final MemberAccountRepository repository;
    private final AccountListClient accountListClient;
    private final TransactionEventPublisher eventPublisher;

    /**
     * 수동 생성(계좌번호 지정)
     * 생성 성공 시 플랫폼 account_list에 메타 등록
     */
    public MemberAccount create(UUID memberId, UUID brokerageId, String accountNumber) {
        MemberAccount acc = repository.save(new MemberAccount(memberId, brokerageId, accountNumber));

        // mkx-platform 에 계좌 메타 등록 요청
        accountListClient.registerAccount(new AccountListRegisterReq(accountNumber, "MEMBER"));
        return acc;
    }

    /**
     * 계좌 상태 변경
     */
    public void updateStatus(UUID accountId, String action) {
        MemberAccount acc = repository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));

        switch (action.toUpperCase()) {
            case "SUSPEND" -> acc.suspend();
            case "DELETE" -> acc.delete();
            case "ACTIVATE" -> acc.activate();
            default -> throw new IllegalArgumentException("지원되지 않는 상태 변경 요청: " + action);
        }

        //  platform(account_list)에도 상태 반영
        String mappedStatus = mapToPlatformStatus(acc.getStatus());
        try {
            accountListClient.updateAccountStatus(
                    acc.getNumber(),
                    new AccountStatusUpdateReq(mappedStatus)
            );
        } catch (Exception e) {
            System.err.println("[WARN] account_list 상태 동기화 실패: " + e.getMessage());
        }
    }

    private String mapToPlatformStatus(AccountStatus status) {
        return switch (status) {
            case ACTIVE -> "APPROVED";
            case SUSPENDED -> "SUSPENDED";
            case DELETED -> "REJECTED";
        };
    }

    /**
     * 자동 생성(계좌번호 자동 부여)
     * - 동일 회원×증권사 조합이 이미 존재하면 예외(컨트롤러 멱등 처리와 결합)
     * - 번호 충돌 시 최대 10회 재시도
     * - 생성 성공 시 플랫폼 account_list에 메타 등록
     */
    public MemberAccount createAuto(UUID memberId, UUID brokerageId) {
        if (repository.existsByMemberIdAndBrokerageId(memberId, brokerageId)) {
            throw new IllegalStateException("이미 해당 증권사 계좌가 존재합니다.");
        }

        for (int i = 0; i < 10; i++) {
            String candidate = MemberAccountNumberGenerator.generate(brokerageId, i);
            if (repository.findByNumber(candidate).isPresent()) continue;

            // 충돌 이력에 대비해 번호를 바꿔가며 저장 시도
            MemberAccount acc = repository.save(new MemberAccount(memberId, brokerageId, candidate));
            accountListClient.registerAccount(new AccountListRegisterReq(candidate, "MEMBER"));
            return acc;
        }
        throw new IllegalStateException("계좌번호 생성 실패: 충돌 과다");
    }

    public Optional<MemberAccount> findByMemberAndBrokerage(UUID memberId, UUID brokerageId) {
        return repository.findByMemberIdAndBrokerageId(memberId, brokerageId);
    }

    /**
     * 단건 조회
     */
    public MemberAccount getByAccountNumber(String accountNumber) {
        return repository.findByNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("개인 계좌를 찾을 수 없습니다."));
    }

    /**
     * 증권사 소속 개인 계좌 목록
     */
    public List<MemberAccount> listByBrokerage(UUID brokerageId) {
        return repository.findAllByBrokerageId(brokerageId);
    }

    /**
     * 입금: 계좌 상태가 ACTIVE일 때만 허용
     */
    public Long deposit(String accountNumber, Long amount) {
        MemberAccount acc = getByAccountNumber(accountNumber);
        if (acc.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("계좌 상태가 활성(ACTIVE)이 아닙니다.");
        }
        acc.deposit(amount);
        
        // Kafka 이벤트 발행
        eventPublisher.publishDepositEvent(accountNumber, acc.getId(), amount, "BANK_TRANSFER");
        
        return acc.getBalance();
    }

    /**
     * 출금: 계좌 상태가 ACTIVE일 때만 허용
     */
    public Long withdraw(String accountNumber, Long amount) {
        MemberAccount acc = getByAccountNumber(accountNumber);
        if (acc.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("계좌 상태가 활성(ACTIVE)이 아닙니다.");
        }
        acc.withdraw(amount);
        
        // Kafka 이벤트 발행
        eventPublisher.publishWithdrawalEvent(accountNumber, acc.getId(), amount, "BANK_TRANSFER");
        
        return acc.getBalance();
    }

    /**
     * 계좌 이체: MEMBER 계좌 간 이체
     * - 송금인 계좌에서 출금
     * - 수취인 계좌로 입금
     * - 트랜잭션으로 묶어서 처리
     */
    public void transfer(String fromAccountNumber, String toAccountNumber, Long amount) {
        // 송금인 계좌 검증 및 출금
        MemberAccount fromAcc = getByAccountNumber(fromAccountNumber);
        if (fromAcc.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("송금인 계좌 상태가 활성(ACTIVE)이 아닙니다.");
        }
        
        // 수취인 계좌 검증
        MemberAccount toAcc = getByAccountNumber(toAccountNumber);
        if (toAcc.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("수취인 계좌 상태가 활성(ACTIVE)이 아닙니다.");
        }
        
        // 송금인 출금
        fromAcc.withdraw(amount);
        
        // 수취인 입금
        toAcc.deposit(amount);
        
        // Kafka 이벤트 발행 (출금/입금 각각)
        eventPublisher.publishWithdrawalEvent(fromAccountNumber, fromAcc.getId(), amount, "TRANSFER");
        eventPublisher.publishDepositEvent(toAccountNumber, toAcc.getId(), amount, "TRANSFER");
    }
}
