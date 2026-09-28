package fr.uge.forkeat.infrastructure.mail.adapter;

import fr.uge.forkeat.service.external.MailGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class JavaMailSenderAdapter implements MailGateway {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mail;

    public JavaMailSenderAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String to, String subject, String text) {
        Objects.requireNonNull(to);
        Objects.requireNonNull(subject);
        Objects.requireNonNull(text);

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setSubject(subject);
        mailMessage.setFrom(mail);
        mailMessage.setTo(to);
        mailMessage.setText(text);
        mailSender.send(mailMessage);
    }
}