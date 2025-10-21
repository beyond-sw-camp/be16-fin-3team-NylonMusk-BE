package com.beyond.MKX.domain.account.corporation.service;

import com.beyond.MKX.domain.account.corporation.dto.AccountIdResDTO;
import com.beyond.MKX.domain.account.corporation.entity.CorporationAccount;
import com.beyond.MKX.domain.account.corporation.repository.CorporationAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CorporationAccountQueryService {
    private final CorporationAccountRepository repository;

    public AccountIdResDTO getAccountIdByCorpId(UUID corpId) {
        CorporationAccount corporationAccount = repository.findByCorporationId(corpId)
                .orElseThrow(() -> new IllegalArgumentException("기업 계좌가 존재하지 않습니다."));
        return new AccountIdResDTO(corporationAccount.getId(), corporationAccount.getAccountNumber());
    }
}
