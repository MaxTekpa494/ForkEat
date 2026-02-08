package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.user.UserRegistrationService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserRegistrationService userRegistrationService;

	public CustomOAuth2UserService(UserRegistrationService userRegistrationService) {
		this.userRegistrationService = userRegistrationService;
	}

	/**
	 * Gère l'authentification OAuth2 Google.
	 *
	 * registerUserFromOAuth2 gère les deux cas :
	 * - User existant (LOCAL ou GOOGLE) : retourne le user tel quel
	 * - Nouvel user : l'inscrit avec authMode=GOOGLE (password=null)
	 *
	 * On n'utilise PAS getUserByEmail car son @Transactional marquerait
	 * la transaction rollback-only en cas de ResourceNotFoundException.
	 */
	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ, timeout = 15, rollbackFor = Exception.class)
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		var oauth2User = super.loadUser(userRequest);

		Map<String, Object> attributes = oauth2User.getAttributes();
		var email = (String) attributes.get("email");
		var givenName = (String) attributes.get("given_name");
		var familyName = (String) attributes.get("family_name");

		User user = userRegistrationService.registerUserFromOAuth2(givenName, familyName, email, AuthMode.GOOGLE);

		return new CustomOAuth2User(oauth2User, user);
	}
}
