package fr.uge.forkeat.service.port;

public interface PasswordHasher {
    String hash(String plainPassword);
    boolean matches(String plainPassword, String hash);
}