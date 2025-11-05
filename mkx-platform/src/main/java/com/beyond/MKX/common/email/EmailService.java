package com.beyond.MKX.common.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * EMAIL_SERVICE: 이메일 발송 서비스
 * - 회원가입 이메일 인증 링크 발송
 * - Redis 큐를 통해 비동기로 이메일 발송 처리
 */
@Slf4j
@Service
public class EmailService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public EmailService(
        @Qualifier("emailQueue") RedisTemplate<String, Object> redisTemplate,
        ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${app.email.verification.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /** EMAIL_QUEUE: Redis 이메일 큐 키 */
    private static final String EMAIL_QUEUE_KEY = "email:queue";

    /**
     * QUEUE_VERIFICATION_EMAIL: 이메일 인증 링크를 Redis 큐에 추가
     * @param toEmail 수신자 이메일
     * @param token 인증 토큰
     * @param name 회원 이름
     */
    public void sendVerificationEmail(String toEmail, String token, String name) {
        try {
            String verificationUrl = baseUrl + "/verify-email?token=" + token;
            
            // HTML 이메일 템플릿 생성
            String htmlContent = buildVerificationEmailHtml(name, verificationUrl);
            
            // 텍스트 버전 (HTML을 지원하지 않는 클라이언트용)
            String textContent = String.format(
                "안녕하세요 %s님,\n\n" +
                "MKX 회원가입을 환영합니다!\n\n" +
                "아래 링크를 클릭하여 이메일 인증을 완료해주세요:\n\n" +
                "%s\n\n" +
                "이 링크는 24시간 동안 유효합니다.\n\n" +
                "만약 회원가입을 요청하지 않으셨다면, 이 이메일을 무시하셔도 됩니다.\n\n" +
                "감사합니다.\n" +
                "MKX 팀",
                name, verificationUrl
            );

            // 이메일 데이터를 Map으로 구성
            Map<String, String> emailData = new HashMap<>();
            emailData.put("to", toEmail);
            emailData.put("subject", "[MKX] 이메일 인증을 완료해주세요");
            emailData.put("htmlContent", htmlContent);
            emailData.put("textContent", textContent);
            emailData.put("from", fromEmail);

            // JSON으로 변환하여 Redis 큐에 추가
            String emailJson = objectMapper.writeValueAsString(emailData);
            redisTemplate.opsForList().rightPush(EMAIL_QUEUE_KEY, emailJson);
            
            log.info("이메일 발송 작업을 Redis 큐에 추가: {}", toEmail);
        } catch (Exception e) {
            log.error("이메일 큐 추가 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 발송 요청에 실패했습니다.", e);
        }
    }

    /**
     * BUILD_VERIFICATION_EMAIL_HTML: 이메일 인증용 HTML 템플릿 생성
     * @param name 회원 이름
     * @param verificationUrl 인증 URL
     * @return HTML 콘텐츠
     */
    private String buildVerificationEmailHtml(String name, String verificationUrl) {
        String template = """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>MKX 이메일 인증</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%" style="background-color: #f5f5f5;">
                    <tr>
                        <td align="center" style="padding: 40px 20px;">
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="600" style="background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                                <!-- Logo Section -->
                                <tr>
                                    <td align="center" style="padding: 40px 20px 20px 20px;">
                                        <div style="background: linear-gradient(135deg, #F0B90B, #FFC933); width: 80px; height: 80px; border-radius: 12px; display: inline-flex; align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(240, 185, 11, 0.3);">
                                            <span style="font-size: 36px; font-weight: bold; color: #0E1723;">M</span>
                                        </div>
                                        <h1 style="margin: 20px 0 0 0; font-size: 32px; font-weight: bold; color: #0E1723; letter-spacing: -0.5px;">MKX</h1>
                                    </td>
                                </tr>
                                
                                <!-- Content Section -->
                                <tr>
                                    <td style="padding: 20px 40px;">
                                        <h2 style="margin: 0 0 20px 0; font-size: 24px; font-weight: 600; color: #0E1723; text-align: center;">
                                            이메일 인증을 완료해주세요
                                        </h2>
                                        <p style="margin: 0 0 20px 0; font-size: 16px; line-height: 1.6; color: #666666;">
                                            안녕하세요 <strong style="color: #0E1723;">%s님</strong>,
                                        </p>
                                        <p style="margin: 0 0 30px 0; font-size: 16px; line-height: 1.6; color: #666666;">
                                            MKX 회원가입을 환영합니다!<br>
                                            아래 버튼을 클릭하여 이메일 인증을 완료해주세요.
                                        </p>
                                        
                                        <!-- Verification Button -->
                                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" width="100%%">
                                            <tr>
                                                <td align="center" style="padding: 0 0 30px 0;">
                                                    <a href="%s" style="display: inline-block; padding: 16px 48px; background: linear-gradient(135deg, #F0B90B, #FFC933); color: #0E1723; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px; box-shadow: 0 4px 12px rgba(240, 185, 11, 0.3);">
                                                        이메일 인증 완료하기
                                                    </a>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <!-- Link Fallback -->
                                        <p style="margin: 0 0 30px 0; font-size: 14px; line-height: 1.6; color: #999999; text-align: center;">
                                            버튼이 작동하지 않는다면 아래 링크를 복사하여 브라우저에 붙여넣어주세요:<br>
                                            <a href="%s" style="color: #F0B90B; word-break: break-all;">%s</a>
                                        </p>
                                        
                                        <!-- Notice -->
                                        <div style="background-color: #f9f9f9; border-left: 4px solid #F0B90B; padding: 16px; margin: 0 0 30px 0; border-radius: 4px;">
                                            <p style="margin: 0; font-size: 14px; line-height: 1.6; color: #666666;">
                                                <strong style="color: #0E1723;"> 유효 기간:</strong> 이 링크는 24시간 동안 유효합니다.<br>
                                                <strong style="color: #0E1723;">️ 보안 안내:</strong> 회원가입을 요청하지 않으셨다면, 이 이메일을 무시하셔도 됩니다.
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="padding: 30px 40px; background-color: #f9f9f9; border-top: 1px solid #e0e0e0; border-radius: 0 0 8px 8px;">
                                        <p style="margin: 0 0 10px 0; font-size: 14px; color: #999999; text-align: center;">
                                            감사합니다.
                                        </p>
                                        <p style="margin: 0; font-size: 14px; font-weight: 600; color: #0E1723; text-align: center;">
                                            MKX 팀
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;
        return String.format(template, name, verificationUrl, verificationUrl, verificationUrl);
    }
}

