package fr.uge.forkeat.infrastructure.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import fr.uge.forkeat.service.exception.AuthenticationTokenException;
import fr.uge.forkeat.service.exception.GoogleTokenVerificationException;
import fr.uge.forkeat.service.model.user.GoogleUserInfo;
import fr.uge.forkeat.service.port.GoogleTokenVerificationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Component
public class GoogleTokenVerificationAdapter implements GoogleTokenVerificationPort {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerificationAdapter(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId))
                .build();
    }

    @Override
    public GoogleUserInfo verify(String idToken) {
        try {
            var googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new AuthenticationTokenException("Invalid Google ID token");
            }
            var payload = googleIdToken.getPayload();
            var givenName = (String) payload.get("given_name");
            var familyName = (String) payload.get("family_name");
            return new GoogleUserInfo(
                    payload.getEmail(),
                    givenName != null ? givenName : "",
                    familyName != null ? familyName : ""
            );
        } catch (AuthenticationTokenException e) {
            throw e;
        } catch (GeneralSecurityException | IOException e) {
            throw new GoogleTokenVerificationException("Something went wrong with Google token verification", e);
        }
    }
}
