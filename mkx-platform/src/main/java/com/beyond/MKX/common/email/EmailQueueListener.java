package com.beyond.MKX.common.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * EMAIL_QUEUE_LISTENER: Redis 큐에서 이메일 발송 작업을 처리하는 리스너
 * - 주기적으로 Redis 큐를 확인하여 이메일 발송
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

    /**
     * PROCESS_EMAIL_QUEUE: Redis 큐에서 이메일 발송 작업 처리
     * - 5초마다 큐를 확인하여 이메일 발송
     */
    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    public void processEmailQueue() {
        try {
            // 큐에서 이메일 데이터 가져오기
            Object emailDataObj = redisTemplate.opsForList().leftPop(EMAIL_QUEUE_KEY);
            
            if (emailDataObj != null) {
                String emailJson = emailDataObj.toString();
                
                // JSON을 Map으로 파싱
                @SuppressWarnings("unchecked")
                Map<String, String> emailMap = objectMapper.readValue(emailJson, 
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
                
                String toEmail = emailMap.get("to");
                String subject = emailMap.get("subject");
                String htmlContent = emailMap.get("htmlContent");
                String textContent = emailMap.get("textContent");
                String fromEmail = emailMap.get("from");
                
                // 이메일 발송 (HTML 지원)
                sendEmailDirect(toEmail, subject, htmlContent, textContent, fromEmail);
                
                log.info("큐에서 이메일 발송 완료: {}", toEmail);
            }
        } catch (Exception e) {
            log.error("이메일 큐 처리 중 오류 발생", e);
            // 오류 발생 시에도 계속 처리하도록 예외를 잡아서 처리
        }
    }

    /**
     * SEND_EMAIL_DIRECT: 실제 이메일 발송 (HTML 지원)
     * @param toEmail 수신자 이메일
     * @param subject 제목
     * @param htmlContent HTML 내용
     * @param textContent 텍스트 내용 (HTML을 지원하지 않는 클라이언트용)
     * @param fromEmail 발신자 이메일
     */
    private void sendEmailDirect(String toEmail, String subject, String htmlContent, String textContent, String fromEmail) {
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
            log.info("이메일 발송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", toEmail, e);
            // 발송 실패 시에도 예외를 던지지 않고 로그만 남김 (큐 처리가 중단되지 않도록)
        }
    }
}

