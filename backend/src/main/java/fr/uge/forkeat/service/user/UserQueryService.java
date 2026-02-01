package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.User;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserQueryService {
    private final UserPersistence userPersistence;

    UserQueryService(UserPersistence userPersistence){
        this.userPersistence = userPersistence;
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) throws ResourceNotFoundException {
        return userPersistence.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    @Transactional(readOnly = true)
    public User getUserById(UUID id) throws ResourceNotFoundException {
        return userPersistence.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) throws ResourceNotFoundException {
        return userPersistence.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé : " + username));
    }
}
