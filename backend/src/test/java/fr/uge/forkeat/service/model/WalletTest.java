package fr.uge.forkeat.service.model;

import fr.uge.forkeat.service.exception.InsufficientFundsException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    private Wallet createWallet(Long balance) {
        return new Wallet(UUID.randomUUID(), balance, UUID.randomUUID(), Instant.now());
    }

    @Nested
    class AddFundsTests {

        @Test
        void addFunds_ShouldIncreaseBalance() {
            // Given
            var wallet = createWallet(1000L);

            // When
            var newWallet = wallet.addFunds(500L);

            // Then
            assertEquals(1500L, newWallet.balance());
        }

        @Test
        void addFunds_ShouldReturnNewWalletInstance() {
            // Given
            var wallet = createWallet(1000L);

            // When
            var newWallet = wallet.addFunds(500L);

            // Then
            assertNotSame(wallet, newWallet);
            assertEquals(1000L, wallet.balance()); // Original unchanged
            assertEquals(1500L, newWallet.balance());
        }

        @Test
        void addFunds_ShouldPreserveWalletId() {
            // Given
            var wallet = createWallet(1000L);

            // When
            var newWallet = wallet.addFunds(500L);

            // Then
            assertEquals(wallet.id(), newWallet.id());
        }

        @Test
        void addFunds_ShouldPreserveUserId() {
            // Given
            var wallet = createWallet(1000L);

            // When
            var newWallet = wallet.addFunds(500L);

            // Then
            assertEquals(wallet.userId(), newWallet.userId());
        }

        @Test
        void addFunds_ShouldUpdateTimestamp() {
            // Given
            var oldTimestamp = Instant.now().minusSeconds(100);
            var wallet = new Wallet(UUID.randomUUID(), 1000L, UUID.randomUUID(), oldTimestamp);

            // When
            var newWallet = wallet.addFunds(500L);

            // Then
            assertTrue(newWallet.updatedAt().isAfter(oldTimestamp));
        }

        @Test
        void addFunds_ShouldWorkWithZeroAmount() {
            // Given
            var wallet = createWallet(1000L);

            // When
            var newWallet = wallet.addFunds(0L);

            // Then
            assertEquals(1000L, newWallet.balance());
        }

        @Test
        void addFunds_ShouldWorkWithLargeAmounts() {
            // Given
            var wallet = createWallet(0L);

            // When
            var newWallet = wallet.addFunds(Long.MAX_VALUE - 1);

            // Then
            assertEquals(Long.MAX_VALUE - 1, newWallet.balance());
        }
    }

    @Nested
    class RemoveFundsTests {

        @Test
        void removeFunds_ShouldDecreaseBalance_WhenSufficientFunds() {
            // Given
            var wallet = createWallet(1000L);

            // When
            var newWallet = wallet.removeFunds(300L);

            // Then
            assertEquals(700L, newWallet.balance());
        }

        @Test
        void removeFunds_ShouldReturnNewWalletInstance() {
            // Given
            var wallet = createWallet(1000L);

            // When
            var newWallet = wallet.removeFunds(300L);

            // Then
            assertNotSame(wallet, newWallet);
            assertEquals(1000L, wallet.balance()); // Original unchanged
        }

        @Test
        void removeFunds_ShouldAllowRemovingExactBalance() {
            // Given
            var wallet = createWallet(1000L);

            // When
            var newWallet = wallet.removeFunds(1000L);

            // Then
            assertEquals(0L, newWallet.balance());
        }

        @Test
        void removeFunds_ShouldThrow_WhenInsufficientFunds() {
            // Given
            var wallet = createWallet(500L);

            // When/Then
            InsufficientFundsException exception = assertThrows(
                    InsufficientFundsException.class,
                    () -> wallet.removeFunds(1000L)
            );

            assertEquals(500L, exception.getAvailable());
            assertEquals(1500L, exception.getRequired());
        }

        @Test
        void removeFunds_ShouldThrow_WhenBalanceIsZero() {
            // Given
            var wallet = createWallet(0L);

            // When/Then
            assertThrows(
                    InsufficientFundsException.class,
                    () -> wallet.removeFunds(1L)
            );
        }

        @Test
        void removeFunds_ShouldPreserveWalletIdAndUserId() {
            // Given
            var wallet = createWallet(1000L);

            // When
            var newWallet = wallet.removeFunds(300L);

            // Then
            assertEquals(wallet.id(), newWallet.id());
            assertEquals(wallet.userId(), newWallet.userId());
        }

        @Test
        void removeFunds_ShouldWorkWithZeroAmount() {
            // Given
            var wallet = createWallet(1000L);

            // When
            var newWallet = wallet.removeFunds(0L);

            // Then
            assertEquals(1000L, newWallet.balance());
        }

        @Test
        void removeFunds_ShouldUpdateTimestamp() {
            // Given
            var oldTimestamp = Instant.now().minusSeconds(100);
            var wallet = new Wallet(UUID.randomUUID(), 1000L, UUID.randomUUID(), oldTimestamp);

            // When
            var newWallet = wallet.removeFunds(500L);

            // Then
            assertTrue(newWallet.updatedAt().isAfter(oldTimestamp));
        }
    }

    @Nested
    class ImmutabilityTests {

        @Test
        void wallet_ShouldBeImmutable() {
            // Given
            var id = UUID.randomUUID();
            var userId = UUID.randomUUID();
            var timestamp = Instant.now();
            var wallet = new Wallet(id, 1000L, userId, timestamp);

            // When - perform operations
            var afterAdd = wallet.addFunds(500L);
            var afterRemove = wallet.removeFunds(200L);

            // Then - original should be unchanged
            assertEquals(1000L, wallet.balance());
            assertEquals(id, wallet.id());
            assertEquals(userId, wallet.userId());
            assertEquals(timestamp, wallet.updatedAt());

            // And new instances should have different values
            assertEquals(1500L, afterAdd.balance());
            assertEquals(800L, afterRemove.balance());
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        void wallet_ShouldAcceptNullUpdatedAt() {
            // Given/When
            var wallet = new Wallet(UUID.randomUUID(), 1000L, UUID.randomUUID(), null);

            // Then
            assertNull(wallet.updatedAt());
            assertEquals(1000L, wallet.balance());
        }

        @Test
        void wallet_ShouldAcceptZeroBalance() {
            // Given/When
            var wallet = new Wallet(UUID.randomUUID(), 0L, UUID.randomUUID(), Instant.now());

            // Then
            assertEquals(0L, wallet.balance());
        }
    }
}
