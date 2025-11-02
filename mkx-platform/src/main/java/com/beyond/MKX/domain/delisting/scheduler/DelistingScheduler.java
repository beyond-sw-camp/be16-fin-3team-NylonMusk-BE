package com.beyond.MKX.domain.delisting.scheduler;

import com.beyond.MKX.domain.delisting.repository.DelistingViolationRepository;
import com.beyond.MKX.domain.delisting.repository.QuarterlySubmissionRepository;
import com.beyond.MKX.domain.delisting.service.DelistingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 상장폐지 자동화 스케줄러
 * 
 * 정기적으로 다음 작업들을 수행합니다:
 * 1. 기준 위반 감지
 * 2. 상장폐지 진행 체크
 * 3. 제출 서류 지연 체크
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DelistingScheduler {

    private final DelistingService delistingService;
    private final DelistingViolationRepository violationRepo;
    private final QuarterlySubmissionRepository submissionRepo;

    /**
     * 매일 오전 9시에 기준 위반 감지 및 상장폐지 진행 체크
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void dailyDelistingCheck() {
        log.info("일일 상장폐지 체크 시작");

        try {
            // 해결되지 않은 위반이 있는 주식들에 대해 진행 체크
            List<UUID> stockIdsWithViolations = violationRepo.findUnresolvedViolations().stream()
                    .map(violation -> violation.getStockId())
                    .distinct()
                    .toList();

            for (UUID stockId : stockIdsWithViolations) {
                delistingService.checkDelistingProgress(stockId);
            }

            log.info("일일 상장폐지 체크 완료: {} 개 주식 검사", stockIdsWithViolations.size());
        } catch (Exception e) {
            log.error("일일 상장폐지 체크 중 오류 발생", e);
        }
    }
    
    /**
     * 매 분마다 상장폐지 자동 진행 체크
     * - DELISTING_RISK: 10분 후 자동으로 DELISTING_NOTICE (예고 발행)
     * - DELISTING_NOTICE: 관리자가 수동으로 executeDelisting 실행 (환불 처리)
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void autoDelistingProcessCheck() {
        log.debug("상장폐지 자동 진행 체크 시작");
        
        try {
            // DELISTING_RISK → DELISTING_NOTICE 전환 (10분 후)
            delistingService.processAutoDelisting();
            
            // DELISTING_NOTICE 이후는 관리자가 수동으로 처리
            
        } catch (Exception e) {
            log.error("상장폐지 자동 진행 체크 중 오류 발생", e);
        }
    }

    /**
     * 매주 월요일 오전 10시에 제출 서류 지연 체크
     */
    @Scheduled(cron = "0 0 10 * * MON")
    public void weeklySubmissionCheck() {
        log.info("주간 제출 서류 체크 시작");

        try {
            // 지연된 제출 목록 조회
            List<com.beyond.MKX.domain.delisting.entity.QuarterlySubmission> overdueSubmissions = 
                    submissionRepo.findOverdueSubmissions();

            for (com.beyond.MKX.domain.delisting.entity.QuarterlySubmission submission : overdueSubmissions) {
                // 제출 지연으로 인한 상장폐지 진행 체크
                delistingService.checkDelistingProgress(submission.getStockId());
            }

            log.info("주간 제출 서류 체크 완료: {} 개 지연 제출", overdueSubmissions.size());
        } catch (Exception e) {
            log.error("주간 제출 서류 체크 중 오류 발생", e);
        }
    }

    /**
     * 매일 오후 6시에 마감일 임박 알림 체크
     */
    @Scheduled(cron = "0 0 18 * * *")
    public void dailyDeadlineCheck() {
        log.info("일일 마감일 체크 시작");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threeDaysLater = now.plusDays(3);

            // 마감일 임박 제출 목록 조회
            List<com.beyond.MKX.domain.delisting.entity.QuarterlySubmission> upcomingDeadlines = 
                    submissionRepo.findUpcomingDeadlines(now, threeDaysLater);

            // TODO: 실제로는 알림 서비스로 알림 발송
            log.info("마감일 임박 제출: {} 개", upcomingDeadlines.size());

            log.info("일일 마감일 체크 완료");
        } catch (Exception e) {
            log.error("일일 마감일 체크 중 오류 발생", e);
        }
    }

    /**
     * 매월 1일 오전 8시에 월간 리포트 생성
     */
    @Scheduled(cron = "0 0 8 1 * *")
    public void monthlyReport() {
        log.info("월간 상장폐지 리포트 생성 시작");

        try {
            // 해결되지 않은 위반 통계
            long unresolvedViolations = violationRepo.findUnresolvedViolations().size();
            
            // 지연된 제출 통계
            long overdueSubmissions = submissionRepo.findOverdueSubmissions().size();

            log.info("월간 상장폐지 리포트: 해결되지 않은 위반 {} 개, 지연된 제출 {} 개", 
                    unresolvedViolations, overdueSubmissions);

            // TODO: 실제로는 리포트 서비스로 리포트 생성 및 발송

            log.info("월간 상장폐지 리포트 생성 완료");
        } catch (Exception e) {
            log.error("월간 상장폐지 리포트 생성 중 오류 발생", e);
        }
    }
}
