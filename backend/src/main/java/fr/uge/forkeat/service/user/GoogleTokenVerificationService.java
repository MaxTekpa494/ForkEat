package fr.uge.forkeat.service.user;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import fr.uge.forkeat.service.exception.GoogleTokenVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
// Cette classe ne respecte pas l'architecture hexa, il y a des dépendences
// directe avec google
@Service
public class GoogleTokenVerificationService {

	private final GoogleIdTokenVerifier verifier;

	public GoogleTokenVerificationService(
			@Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId) {
		this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
				.setAudience(List.of(clientId))
				.build();
	}

	public GoogleIdToken verify(String idTokenString){
        try {
            return verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException e) {
            throw new GoogleTokenVerificationException("Something went wrong with Google token verification", e);
        }
    }
}
