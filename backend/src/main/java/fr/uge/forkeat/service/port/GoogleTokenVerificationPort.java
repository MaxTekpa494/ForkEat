package fr.uge.forkeat.service.port;

import fr.uge.forkeat.service.model.user.GoogleUserInfo;

public interface GoogleTokenVerificationPort {
    GoogleUserInfo verify(String idToken);
}
