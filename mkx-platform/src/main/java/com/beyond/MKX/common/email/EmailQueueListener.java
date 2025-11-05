package com.beyond.MKX.common.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * EMAIL_QUEUE_LISTENER: Redis 큐에서 이메일 발송 작업을 처리하는 리스너
 * - 주기적으로 Redis 큐를 확인하여 배치로 이메일 발송
 * - 병렬 처리로 성능 개선
 */
@Slf4j
@Component
public class EmailQueueListener {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    public EmailQueueListener(
        @Qualifier("emailQueue") RedisTemplate<String, Object> redisTemplate,
        JavaMailSender mailSender,
        ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
    }

    /** EMAIL_QUEUE: Redis 이메일 큐 키 */
    private static final String EMAIL_QUEUE_KEY = "email:queue";
    /** EMAIL_QUEUE_RETRY: 재시도 큐 키 */
    private static final String EMAIL_QUEUE_RETRY_KEY = "email:queue:retry";
    /** BATCH_SIZE: 한 번에 처리할 이메일 수 (최대 스레드 수와 동일하게 설정) */
    private static final int BATCH_SIZE = 5;
    /** MAX_RETRY: 최대 재시도 횟수 */
    private static final int MAX_RETRY = 3;

    /**
     * PROCESS_EMAIL_QUEUE: Redis 큐에서 이메일 발송 작업 처리 (배치 처리)
     * - 3초마다 큐를 확인하여 최대 5개씩 배치로 처리
     * - 병렬 처리로 성능 개선 (Gmail SMTP 동시 연결 제한 고려)
     */
    @Scheduled(fixedDelay = 3000) // 3초마다 실행
    public void processEmailQueue() {
        try {
            // 배치로 이메일 데이터 가져오기 (최대 5개)
            List<Object> emailDataList = new ArrayList<>();
            for (int i = 0; i < BATCH_SIZE; i++) {
                Object emailDataObj = redisTemplate.opsForList().leftPop(EMAIL_QUEUE_KEY);
                if (emailDataObj == null) {
                    break;
                }
                emailDataList.add(emailDataObj);
            }
            
            if (emailDataList.isEmpty()) {
                return;
            }
            
            log.debug("이메일 큐에서 {}개 항목 처리 시작", emailDataList.size());
            
            // 병렬로 이메일 발송
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Object emailDataObj : emailDataList) {
                CompletableFuture<Void> future = processEmailAsync(emailDataObj.toString());
                futures.add(future);
            }
            
            // 모든 작업 완료 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            log.debug("이메일 큐 처리 완료: {}개 발송", emailDataList.size());
        } catch (Exception e) {
            log.error("이메일 큐 처리 중 오류 발생", e);
            // 오류 발생 시에도 계속 처리하도록 예외를 잡아서 처리
        }
    }

    /**
     * PROCESS_EMAIL_ASYNC: 비동기로 이메일 발송 처리
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Void> processEmailAsync(String emailJson) {
        try {
            // JSON을 Map으로 파싱
            @SuppressWarnings("unchecked")
            Map<String, String> emailMap = objectMapper.readValue(emailJson, 
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
            
            String toEmail = emailMap.get("to");
            String subject = emailMap.get("subject");
            String htmlContent = emailMap.get("htmlContent");
            String textContent = emailMap.get("textContent");
            String fromEmail = emailMap.get("from");
            
            // 재시도 횟수 확인
            int retryCount = Integer.parseInt(emailMap.getOrDefault("retryCount", "0"));
            
            // 이메일 발송 (HTML 지원)
            boolean success = sendEmailDirect(toEmail, subject, htmlContent, textContent, fromEmail);
            
            if (success) {
                log.info("이메일 발송 완료: {}", toEmail);
            } else {
                // 실패 시 재시도 큐에 추가
                handleEmailFailure(emailJson, retryCount);
            }
        } catch (Exception e) {
            log.error("이메일 처리 중 오류 발생: {}", emailJson, e);
            // 재시도 큐에 추가
            handleEmailFailure(emailJson, 0);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * SEND_EMAIL_DIRECT: 실제 이메일 발송 (HTML 지원)
     * @param toEmail 수신자 이메일
     * @param subject 제목
     * @param htmlContent HTML 내용
     * @param textContent 텍스트 내용 (HTML을 지원하지 않는 클라이언트용)
     * @param fromEmail 발신자 이메일
     * @return 발송 성공 여부
     */
    private boolean sendEmailDirect(String toEmail, String subject, String htmlContent, String textContent, String fromEmail) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            
            // HTML 콘텐츠 설정 (텍스트 대체본 포함)
            if (htmlContent != null && !htmlContent.isEmpty()) {
                helper.setText(textContent != null ? textContent : "", htmlContent);
            } else {
                helper.setText(textContent != null ? textContent : "");
            }
            
            mailSender.send(mimeMessage);
            return true;
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", toEmail, e);
            return false;
        }
    }

    /**
     * HANDLE_EMAIL_FAILURE: 이메일 발송 실패 시 재시도 처리
     * @param emailJson 이메일 데이터 JSON
     * @param retryCount 현재 재시도 횟수
     */
    private void handleEmailFailure(String emailJson, int retryCount) {
        try {
            if (retryCount < MAX_RETRY) {
                // 재시도 횟수 증가
                @SuppressWarnings("unchecked")
                Map<String, String> emailMap = objectMapper.readValue(emailJson, 
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
                emailMap.put("retryCount", String.valueOf(retryCount + 1));
                
                String retryEmailJson = objectMapper.writeValueAsString(emailMap);
                redisTemplate.opsForList().rightPush(EMAIL_QUEUE_RETRY_KEY, retryEmailJson);
                log.warn("이메일 발송 실패, 재시도 큐에 추가 (재시도 횟수: {}/{}): {}", 
                    retryCount + 1, MAX_RETRY, emailMap.get("to"));
            } else {
                log.error("이메일 발송 최대 재시도 횟수 초과: {}", emailJson);
                // 최대 재시도 횟수 초과 시 로그만 남기고 버림
            }
        } catch (Exception e) {
            log.error("이메일 재시도 처리 중 오류 발생", e);
        }
    }

    /**
     * PROCESS_RETRY_QUEUE: 재시도 큐 처리
     * - 30초마다 재시도 큐를 확인하여 다시 발송 시도
     */
    @Scheduled(fixedDelay = 30000) // 30초마다 실행
    public void processRetryQueue() {
        try {
            // 재시도 큐에서 원래 큐로 이동 (최대 5개)
            for (int i = 0; i < 5; i++) {
                Object emailDataObj = redisTemplate.opsForList().leftPop(EMAIL_QUEUE_RETRY_KEY);
                if (emailDataObj == null) {
                    break;
                }
                redisTemplate.opsForList().rightPush(EMAIL_QUEUE_KEY, emailDataObj);
            }
        } catch (Exception e) {
            log.error("재시도 큐 처리 중 오류 발생", e);
        }
    }
}

