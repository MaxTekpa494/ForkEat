package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.TransactionEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.infrastructure.persistence.mapper.TransactionEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.mapper.WalletEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.TransactionRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserJpaRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.WalletRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.model.Wallet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletPersistenceAdapterTest {

    @Mock
    WalletRepository walletRepository;
    @Mock
    TransactionRepository transactionRepository;
    @Mock UserJpaRepository userRepository;
    @Mock
    WalletEntityMapper walletEntityMapper;
    @Mock
    TransactionEntityMapper transactionEntityMapper;

    @InjectMocks
    WalletPersistenceAdapter adapter;

    @Test
    void loadWalletWithLock_ShouldReturnWallet_WhenFound() {
        var userId = UUID.randomUUID();
        var entity = new WalletEntity();
        var domain = mock(Wallet.class);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(entity));
        when(walletEntityMapper.toDomain(entity)).thenReturn(domain);

        var result = adapter.loadWalletWithLock(userId);

        assertTrue(result.isPresent());
        assertEquals(domain, result.get());
        verify(walletRepository).findByUserId(userId);
    }

    @Test
    void loadWalletWithLock_ShouldReturnEmpty_WhenNotFound() {
        var userId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var result = adapter.loadWalletWithLock(userId);

        assertTrue(result.isEmpty());
        verifyNoInteractions(walletEntityMapper);
    }

    @Test
    void saveWallet_ShouldCreateNewWallet_WhenNotExists() {
        var walletId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var walletDomain = mock(Wallet.class);
        when(walletDomain.id()).thenReturn(walletId);
        when(walletDomain.userId()).thenReturn(userId);

        var userEntity = mock(UserEntity.class);

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(walletEntityMapper.toDomain(any(WalletEntity.class))).thenReturn(walletDomain);

        var result = adapter.saveWallet(walletDomain);

        verify(userEntity).setWallet(any(WalletEntity.class));
        verify(walletRepository).save(any(WalletEntity.class));
        assertEquals(walletDomain, result);
    }

    @Test
    void saveWallet_ShouldUpdateAndReturnWallet_WhenExists() {
        var walletId = UUID.randomUUID();
        var walletDomain = mock(Wallet.class);
        when(walletDomain.id()).thenReturn(walletId);

        var existingEntity = new WalletEntity();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(existingEntity));
        when(walletRepository.save(existingEntity)).thenReturn(existingEntity);
        when(walletEntityMapper.toDomain(existingEntity)).thenReturn(walletDomain);

        var result = adapter.saveWallet(walletDomain);

        verify(walletEntityMapper).updateEntity(existingEntity, walletDomain);
        verify(walletRepository).save(existingEntity);
        assertEquals(walletDomain, result);
    }

    @Test
    void transactionExists_ShouldReturnTrue_WhenRepoReturnsTrue() {
        var externalId = "stripe_123";
        when(transactionRepository.existsByStripeTransactionID(externalId)).thenReturn(true);

        boolean exists = adapter.transactionExists(externalId);

        assertTrue(exists);
    }

    @Test
    void saveTransaction_ShouldLinkWallets_WhenIdsArePresent() {
        var sourceId = UUID.randomUUID();
        var destId = UUID.randomUUID();

        var domainTx = mock(Transaction.class);
        when(domainTx.walletSourceId()).thenReturn(sourceId);
        when(domainTx.walletDestinationId()).thenReturn(destId);

        var entityTx = new TransactionEntity();
        when(transactionEntityMapper.toEntity(domainTx)).thenReturn(entityTx);

        var sourceRef = new WalletEntity();
        var destRef = new WalletEntity();

        when(walletRepository.getReferenceById(sourceId)).thenReturn(sourceRef);
        when(walletRepository.getReferenceById(destId)).thenReturn(destRef);

        when(transactionRepository.save(any())).thenReturn(entityTx);
        when(transactionEntityMapper.toDomain(entityTx)).thenReturn(domainTx);

        Transaction result = adapter.saveTransaction(domainTx);

        assertEquals(sourceRef, entityTx.getSourceWallet());
        assertEquals(destRef, entityTx.getDestinationWallet());
        assertEquals(domainTx, result);
    }

    @Test
    void saveTransaction_ShouldNotLinkWallets_WhenIdsAreNull() {
        var domainTx = mock(Transaction.class);
        when(domainTx.walletSourceId()).thenReturn(null);
        when(domainTx.walletDestinationId()).thenReturn(null);

        var entityTx = new TransactionEntity();
        when(transactionEntityMapper.toEntity(domainTx)).thenReturn(entityTx);

        when(transactionRepository.save(any())).thenReturn(entityTx);
        when(transactionEntityMapper.toDomain(entityTx)).thenReturn(domainTx);

        adapter.saveTransaction(domainTx);

        assertNull(entityTx.getSourceWallet());
        assertNull(entityTx.getDestinationWallet());

        verify(walletRepository, never()).getReferenceById(any());
    }

    @Test
    void getWalletById_ReturnEmpty_WhenNotFound(){
        var id = UUID.randomUUID();
        assertEquals(Optional.empty(),  adapter.getWalletById(id));
    }

    @Test
    void getWalletById_ShouldReturnWallet_WhenFound(){
        var walletDomain = mock(Wallet.class);
        assertEquals(Optional.empty(),  adapter.getWalletById(walletDomain.id()));
    }

    @Test
    void getWalletByUserId_ThrowsException_WhenNotFound() {
        var walletDomain = mock(Wallet.class);

        assertThrows(
                ResourceNotFoundException.class,
                () -> adapter.findByUserId(walletDomain.userId())
        );
    }

}