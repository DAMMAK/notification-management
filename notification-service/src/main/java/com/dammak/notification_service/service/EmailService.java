package com.dammak.notification_service.service;


import com.dammak.notification_service.model.NotificationRequest;
import com.dammak.notification_service.model.NotificationStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import io.micrometer.core.instrument.Counter;


import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final Counter emailSuccessCounter;
    private final Counter emailFailureCounter;
    private final FailedNotificationService failedNotificationService;

    @CircuitBreaker(name = "emailService", fallbackMethod = "fallbackEmail")
    @Retry(name = "emailService")
    public NotificationStatus sendEmail(NotificationRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(request.getRecipient());
            helper.setSubject(request.getSubject());

            String content;
            if (request.getTemplateName() != null && !request.getTemplateName().isEmpty()) {
                Context context = new Context();
                if (request.getTemplateData() != null) {
                    request.getTemplateData().forEach(context::setVariable);
                }
                content = templateEngine.process(request.getTemplateName(), context);
            } else {
                content = request.getContent();
            }

            helper.setText(content, true);
            mailSender.send(message);

            emailSuccessCounter.increment();
            log.info("Email sent successfully to {}", request.getRecipient());
            return NotificationStatus.getSuccess(request, null);
        } catch (Exception e) {
            emailFailureCounter.increment();
            log.error("Failed to send email to {}: {}", request.getRecipient(), e.getMessage());
            failedNotificationService.saveFailedNotification(request, e.getMessage());
            return NotificationStatus.getError(request, null);

        }
    }

    public NotificationStatus fallbackEmail(NotificationRequest request, Exception e) {
        log.error("Circuit breaker triggered for email to {}: {}", request.getRecipient(), e.getMessage());
        failedNotificationService.saveFailedNotification(request, "Circuit breaker triggered: " + e.getMessage());
        return NotificationStatus.getError(request, "Email service temporarily unavailable");

    }

//    @Bean
//    public JavaMailSender getJavaMailSender() {
//        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
//        mailSender.setHost("smtp.gmail.com");
//        mailSender.setPort(587);
//
//        mailSender.setUsername();
//        mailSender.setPassword("dqxsnyxsckbklkbt");
//
//        Properties props = mailSender.getJavaMailProperties();
//        props.put("mail.transport.protocol", "smtp");
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", "true");
//        props.put("mail.debug", "true");
//
//        return mailSender;
//    }
}