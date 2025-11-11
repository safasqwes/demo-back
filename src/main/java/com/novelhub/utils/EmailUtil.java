package com.novelhub.utils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Email Utility
 * Provides email sending functionality
 */
@Slf4j
@Component
public class EmailUtil {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.properties.mail.smtp.from:NovelHub}")
    private String fromName;

    public void sendVerificationEmail(String toEmail, String username, String verificationToken, String baseUrl) {
        if (!StringUtils.hasText(toEmail) || !StringUtils.hasText(verificationToken)) {
            log.warn("skip send verification: missing email or token");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("邮箱验证 - NovelHub");
            String link = baseUrl + "/api/auth/verify-email?token=" + verificationToken;
            String html = "<p>Hi, " + username + ", 点击验证邮箱：</p><p><a href='" + link + "'>" + link + "</a></p>";
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("send verification email failed", e);
        }
    }
}

