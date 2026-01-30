package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.mapper.UserMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserJpaRepository;
import fr.uge.forkeat.service.model.User;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserPersistence {

    private final UserJpaRepository userRepository;
    private final UserMapper userMapper;

    public UserPersistenceAdapter(UserJpaRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public User saveUser(User user) {
        var entity = userMapper.toEntity(user);
        var saved = userRepository.save(entity);
        return userMapper.toModel(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id).map(userMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(userMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username).map(userMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}