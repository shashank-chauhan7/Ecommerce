package com.ecommerce.notification.bridge;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Slf4j
@RequiredArgsConstructor
public class EmailChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    @Override
    public void send(NotificationMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.body(), message.isHtml());

            mailSender.send(mimeMessage);
            log.info("Email sent successfully to={}, subject={}", message.to(), message.subject());
        } catch (MessagingException e) {
            log.error("Failed to send email to={}, subject={}: {}", message.to(), message.subject(), e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
