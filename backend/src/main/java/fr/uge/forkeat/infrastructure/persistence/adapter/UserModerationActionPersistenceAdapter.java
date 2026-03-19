package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.UserModerationActionEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserModerationActionRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserReportRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.user.UserModerationAction;
import fr.uge.forkeat.service.model.user.UserModerationActionType;
import fr.uge.forkeat.service.persistence.UserModerationActionPersistence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class UserModerationActionPersistenceAdapter implements UserModerationActionPersistence {
  private final UserModerationActionRepository userModerationActionRepository;
  private final UserRepository userRepository;
  private final UserReportRepository userReportRepository;

  public UserModerationActionPersistenceAdapter(UserModerationActionRepository userModerationActionRepository,
                                                UserRepository userRepository,
                                                UserReportRepository userReportRepository) {
    this.userModerationActionRepository = userModerationActionRepository;
    this.userRepository = userRepository;
    this.userReportRepository = userReportRepository;
  }




  @Override
  public UserModerationAction save(UserModerationAction action) {
    Objects.requireNonNull(action);

    var user = userRepository.getReferenceById(action.userId());
    var moderator = userRepository.getReferenceById(action.moderatorId());
    var report = userReportRepository.getReferenceById(action.relatedReportId());

    var entity = UserModerationActionEntityMapper.toEntity(action, user, moderator, report);
    var saved = userModerationActionRepository.save(entity);
    return UserModerationActionEntityMapper.toDomain(saved);
  }

  @Override
  public List<UserModerationAction> findByUserId(UUID userId) {
    Objects.requireNonNull(userId);
    return userModerationActionRepository.findByUserId(userId).stream()
            .map(UserModerationActionEntityMapper::toDomain)
            .toList();
  }

  @Override
  public List<UserModerationAction> findByActionType(UserModerationActionType actionType) {
    Objects.requireNonNull(actionType);
    return userModerationActionRepository.findByModerationActionType(actionType).stream()
            .map(UserModerationActionEntityMapper::toDomain)
            .toList();
  }
}

