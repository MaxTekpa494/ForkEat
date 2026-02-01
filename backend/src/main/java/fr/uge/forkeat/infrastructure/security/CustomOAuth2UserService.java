package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.User;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.user.UserQueryService;
import fr.uge.forkeat.service.user.UserRegistrationService;
import fr.uge.forkeat.service.user.UserUpdateService;
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

    private final UserQueryService userQueryService;
    private final UserRegistrationService userRegistrationService;
    private final UserUpdateService userUpdateService;

    public CustomOAuth2UserService(UserQueryService userQueryService,
                                   UserRegistrationService userRegistrationService,
                                   UserUpdateService userUpdateService) {
        this.userQueryService = userQueryService;
        this.userRegistrationService = userRegistrationService;
        this.userUpdateService = userUpdateService;
    }

    /**
     * Gère l'authentification OAuth2.
     * 
     * ISOLATION: REPEATABLE_READ
     * - Vérification d'existence du user par email
     * - Création potentielle du user + wallet
     */
    @Override
    @Transactional(
        isolation = Isolation.REPEATABLE_READ,
        timeout = 15,
        rollbackFor = Exception.class
    )
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        var oauth2User = super.loadUser(userRequest);
        
        Map<String, Object> attributes = oauth2User.getAttributes();
        var email = (String) attributes.get("email");
        var givenName = (String) attributes.get("given_name");
        var familyName = (String) attributes.get("family_name");
        var picture = (String) attributes.get("picture");
        
        User user;
        try {
            user = userQueryService.getUserByEmail(email);
            
            if (user.authentificationMode() == AuthMode.LOCAL) {
                user = userUpdateService.migrateToOAuth2(user.id(), AuthMode.GOOGLE);
            }
            
        } catch (ResourceNotFoundException e) {
            try {
                user = userRegistrationService.registerUserFromOAuth2(
                    givenName,
                    familyName,
                    email,
                    AuthMode.GOOGLE,
                    picture  // Photo de profil Google
                );
            } catch (ResourceNotFoundException ex) {
                throw new OAuth2AuthenticationException("Erreur lors de la création du compte");
            }
        }
        
        return new CustomOAuth2User(oauth2User, user);
    }
}