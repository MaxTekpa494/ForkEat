package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.TransactionRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.WalletRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.TransactionType;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.user.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletPersistenceAdapterTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletPersistenceAdapter adapter;

    private UserEntity createUserEntity(UUID userId) {
        var user = new UserEntity();
        user.setId(userId);
        user.setUsername("testuser");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("test@example.com");
        user.setPassword("hashed");
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    private WalletEntity createWalletEntity(UUID walletId, UUID userId, long balance) {
        var user = createUserEntity(userId);
        var wallet = new WalletEntity(balance, user);
        wallet.setId(walletId);
        wallet.setUpdatedAt(Instant.now());
        return wallet;
    }

    private TransactionEntity createTransactionEntity(UUID id, String stripeId) {
        var entity = new TransactionEntity();
        entity.setId(id);
        entity.setStripeTransactionID(stripeId);
        entity.setAmount(1000L);
        entity.setTransactionType(TransactionType.RECHARGE);
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    @Test
    void loadWalletWithLock_ShouldReturnWallet_WhenFound() {
        // Given
        var userId = UUID.randomUUID();
        var walletId = UUID.randomUUID();
        var walletEntity = createWalletEntity(walletId, userId, 1000L);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(walletEntity));

        // When
        var result = adapter.loadWalletWithLock(userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(walletId, result.get().id());
        assertEquals(userId, result.get().userId());
        assertEquals(1000L, result.get().balance());
        verify(walletRepository).findByUserId(userId);
    }

    @Test
    void loadWalletWithLock_ShouldReturnEmpty_WhenNotFound() {
        // Given
        var userId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        var result = adapter.loadWalletWithLock(userId);

        // Then
        assertTrue(result.isEmpty());
        verify(walletRepository).findByUserId(userId);
    }

    @Test
    void saveWallet_ShouldCreateNewWallet_WhenNotExists() {
        // Given
        var walletId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var userEntity = createUserEntity(userId);
        var walletDomain = new Wallet(walletId, userId, 1000L, Instant.now());

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var result = adapter.saveWallet(walletDomain);

        // Then
        assertNotNull(result);
        assertEquals(walletId, result.id());
        assertEquals(userId, result.userId());
        assertEquals(1000L, result.balance());

        verify(walletRepository).save(argThat(entity ->
                entity.getId().equals(walletId) &&
                        entity.getUser().getId().equals(userId) &&
                        entity.getBalance() == 1000L
        ));
    }

    @Test
    void saveWallet_ShouldUpdateWallet_WhenExists() {
        // Given
        var walletId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var existingEntity = createWalletEntity(walletId, userId, 500L);
        var updatedWallet = new Wallet(walletId, userId, 2000L, Instant.now());

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(existingEntity));
        when(walletRepository.save(existingEntity)).thenReturn(existingEntity);

        // When
        var result = adapter.saveWallet(updatedWallet);

        // Then
        assertNotNull(result);
        assertEquals(walletId, result.id());
        assertEquals(2000L, result.balance());
        assertEquals(2000L, existingEntity.getBalance());
        verify(walletRepository).save(existingEntity);
    }

    @Test
    void saveWallet_ShouldThrowException_WhenUserNotFound() {
        // Given
        var walletId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var walletDomain = new Wallet(walletId, userId, 1000L, Instant.now());

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalStateException.class, () -> adapter.saveWallet(walletDomain));
    }

    @Test
    void getWalletById_ShouldReturnWallet_WhenFound() {
        // Given
        var walletId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var walletEntity = createWalletEntity(walletId, userId, 1000L);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(walletEntity));

        // When
        var result = adapter.getWalletById(walletId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(walletId, result.get().id());
        assertEquals(userId, result.get().userId());
        verify(walletRepository).findById(walletId);
    }

    @Test
    void getWalletById_ShouldReturnEmpty_WhenNotFound() {
        // Given
        var walletId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // When
        var result = adapter.getWalletById(walletId);

        // Then
        assertTrue(result.isEmpty());
        verify(walletRepository).findById(walletId);
    }

    @Test
    void transactionExists_ShouldReturnTrue_WhenExists() {
        // Given
        var externalId = "stripe_123";
        when(transactionRepository.existsByStripeTransactionID(externalId)).thenReturn(true);

        // When
        boolean result = adapter.transactionExists(externalId);

        // Then
        assertTrue(result);
        verify(transactionRepository).existsByStripeTransactionID(externalId);
    }

    @Test
    void transactionExists_ShouldReturnFalse_WhenNotExists() {
        // Given
        var externalId = "stripe_456";
        when(transactionRepository.existsByStripeTransactionID(externalId)).thenReturn(false);

        // When
        boolean result = adapter.transactionExists(externalId);

        // Then
        assertFalse(result);
        verify(transactionRepository).existsByStripeTransactionID(externalId);
    }

    @Test
    void saveTransaction_ShouldLinkBothWallets_WhenBothIdsPresent() {
        // Given
        var transactionId = UUID.randomUUID();
        var sourceWalletId = UUID.randomUUID();
        var destWalletId = UUID.randomUUID();
        var sourceUserId = UUID.randomUUID();
        var destUserId = UUID.randomUUID();

        var transaction = new Transaction(
                sourceWalletId,
                destWalletId,
                1000L,
                TransactionType.REDISTRIBUTION,
                Instant.now(),
                "stripe_123"
                );

        var sourceWallet = createWalletEntity(sourceWalletId, sourceUserId, 5000L);
        var destWallet = createWalletEntity(destWalletId, destUserId, 2000L);
        var savedEntity = createTransactionEntity(transactionId, "stripe_123");

        when(walletRepository.getReferenceById(sourceWalletId)).thenReturn(sourceWallet);
        when(walletRepository.getReferenceById(destWalletId)).thenReturn(destWallet);
        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(savedEntity);

        // When
        var result = adapter.saveTransaction(transaction);

        // Then
        assertNotNull(result);
        verify(walletRepository).getReferenceById(sourceWalletId);
        verify(walletRepository).getReferenceById(destWalletId);
        verify(transactionRepository).save(argThat(entity ->
                entity.getSourceWallet() != null &&
                        entity.getDestinationWallet() != null &&
                        entity.getSourceWallet().getId().equals(sourceWalletId) &&
                        entity.getDestinationWallet().getId().equals(destWalletId)
        ));
    }

    @Test
    void saveTransaction_ShouldNotLinkWallets_WhenBothIdsNull() {
        // Given
        var transactionId = UUID.randomUUID();
        var transaction = new Transaction(
                null,
                null,
                1000L,
                TransactionType.RECHARGE,
                Instant.now(),
                "stripe_456"
                );

        var savedEntity = createTransactionEntity(transactionId, "stripe_456");

        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(savedEntity);

        // When
        var result = adapter.saveTransaction(transaction);

        // Then
        assertNotNull(result);
        verify(walletRepository, never()).getReferenceById(any());
        verify(transactionRepository).save(argThat(entity ->
                entity.getSourceWallet() == null &&
                        entity.getDestinationWallet() == null
        ));
    }

    @Test
    void saveTransaction_ShouldLinkOnlyDestination_WhenOnlyDestIdPresent() {
        // Given
        var transactionId = UUID.randomUUID();
        var destWalletId = UUID.randomUUID();
        var destUserId = UUID.randomUUID();

        var transaction = new Transaction(
                null,
                destWalletId,
                1000L,
                TransactionType.REDISTRIBUTION,
                Instant.now(),
                "stripe_789"
        );

        var destWallet = createWalletEntity(destWalletId, destUserId, 2000L);
        var savedEntity = createTransactionEntity(transactionId, "stripe_789");

        when(walletRepository.getReferenceById(destWalletId)).thenReturn(destWallet);
        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(savedEntity);

        // When
        var result = adapter.saveTransaction(transaction);

        // Then
        assertNotNull(result);
        verify(walletRepository).getReferenceById(destWalletId);
        verify(walletRepository, times(1)).getReferenceById(any());
        verify(transactionRepository).save(argThat(entity ->
                entity.getSourceWallet() == null &&
                        entity.getDestinationWallet() != null &&
                        entity.getDestinationWallet().getId().equals(destWalletId)
        ));
    }

    @Test
    void findByUserId_ShouldReturnWallet_WhenFound() {
        // Given
        var userId = UUID.randomUUID();
        var walletId = UUID.randomUUID();
        var walletEntity = createWalletEntity(walletId, userId, 1000L);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(walletEntity));

        // When
        var result = adapter.findByUserId(userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(walletId, result.get().id());
        assertEquals(userId, result.get().userId());
        verify(walletRepository, times(2)).findByUserId(userId);
    }

    @Test
    void findByUserId_ShouldThrowException_WhenNotFound() {
        // Given
        var userId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When/Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> adapter.findByUserId(userId)
        );

        assertTrue(exception.getMessage().contains(userId.toString()));
        verify(walletRepository).findByUserId(userId);
    }

    @Test
    void getBalance_ShouldReturnBalance_WhenWalletExists() {
        // Given
        var userId = UUID.randomUUID();
        var balance = 5000L;

        when(walletRepository.findBalanceByUserId(userId)).thenReturn(balance);

        // When
        var result = adapter.getBalance(userId);

        // Then
        assertEquals(balance, result);
        verify(walletRepository).findBalanceByUserId(userId);
    }

    @Test
    void getBalance_ShouldReturnZero_WhenWalletNotExists() {
        // Given
        var userId = UUID.randomUUID();

        when(walletRepository.findBalanceByUserId(userId)).thenReturn(null);

        // When
        var result = adapter.getBalance(userId);

        // Then
        assertEquals(0L, result);
        verify(walletRepository).findBalanceByUserId(userId);
    }
}