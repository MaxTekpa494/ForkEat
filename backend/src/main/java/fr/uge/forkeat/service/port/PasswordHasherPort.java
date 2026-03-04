package fr.uge.forkeat.service.port;

public interface PasswordHasherPort {
    String hash(String plainPassword);
    boolean matches(String plainPassword, String hash);
}