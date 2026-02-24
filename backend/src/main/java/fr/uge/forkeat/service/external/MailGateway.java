package fr.uge.forkeat.service.external;

public interface MailGateway {
    void send(String to, String subject, String text);
}