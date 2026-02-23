package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.mapper.BankInfoEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.BankInfoEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.BankInfoRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.user.BankInfo;
import fr.uge.forkeat.service.persistence.BankInfoPersistence;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static fr.uge.forkeat.infrastructure.persistence.mapper.BankInfoEntityMapper.toDomain;

@Component
public class BankInfoPersistenceAdapter implements BankInfoPersistence {

    private final BankInfoRepository bankInfoRepository;
    private final UserRepository userRepository;

    public BankInfoPersistenceAdapter(BankInfoRepository bankInfoRepository, UserRepository userRepository) {
        this.bankInfoRepository = Objects.requireNonNull(bankInfoRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Override
    public Optional<BankInfo> findByUserId(UUID userId) {
        Objects.requireNonNull(userId);
        return bankInfoRepository.findByUserId(userId)
                .map(BankInfoEntityMapper::toDomain);
    }

    @Override
    public BankInfo saveBankInfo(BankInfo bankInfo, UUID userId) {
        Objects.requireNonNull(bankInfo);
        Objects.requireNonNull(userId);

        BankInfoEntity entity = bankInfoRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // If no existing BankInfo for user, create a new one
                    BankInfoEntity newEntity = new BankInfoEntity();
                    newEntity.setId(UUID.randomUUID()); // Assign new ID
                    newEntity.setUser(userRepository.findById(userId) // est ce qu'on a vraiment besoin de vérifier si le user exist ?
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId)));
                    return newEntity;
                });

        entity.setBankName(bankInfo.bankName());
        entity.setExternalAccountId(bankInfo.externalAccountId());

        BankInfoEntity savedEntity = bankInfoRepository.save(entity);
        return BankInfoEntityMapper.toDomain(savedEntity);
    }

    @Override
    public void deleteBankInfo(UUID bankInfoId) {
        Objects.requireNonNull(bankInfoId);
        bankInfoRepository.deleteById(bankInfoId);
    }
}