package com.appointflow.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:no-reply@appointflow.com}")
    private String fromAddress;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @PostConstruct
    void init() {
        if (!enabled) {
            log.info("MailService: SMTP devre disi (app.mail.enabled=false). Mail'ler log'a yazilacak.");
        }
    }

    public void send(String to, String subject, String body) {
        if (!enabled || mailSenderProvider.getIfAvailable() == null) {
            log.info("[MAIL-LOG] to={} subject={}\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSenderProvider.getObject().send(msg);
        } catch (Exception e) {
            log.error("Mail gonderilemedi (to={}, subject={}): {}", to, subject, e.getMessage());
        }
    }
}
