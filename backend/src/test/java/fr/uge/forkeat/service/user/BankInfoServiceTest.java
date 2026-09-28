package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.external.PayoutGateway;
import fr.uge.forkeat.service.model.user.BankInfo;
import fr.uge.forkeat.service.persistence.BankInfoPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankInfoServiceTest {

    @Mock
    private BankInfoPersistence bankInfoPersistence;
    @Mock
    private PayoutGateway payoutGateway;
    @InjectMocks
    private BankInfoService bankInfoService;

    private UUID userId;
    private String userEmail;
    private String bankName;
    private String iban;
    private String bic;
    private String externalAccountId;
    private BankInfo bankInfo;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userEmail = "test@example.com";
        bankName = "Test Bank";
        iban = "FR1234567890123456789012345";
        bic = "TESTFRXX";
        externalAccountId = "ext_acct_test123";
        bankInfo = new BankInfo(userId, bankName, externalAccountId);
    }

    @Test
    void shouldCreateOrUpdateBankInfoSuccessfully() {
        // Given
        when(bankInfoPersistence.findByUserId(userId)).thenReturn(Optional.empty());
        when(payoutGateway.createExternalAccount(userId, userEmail, bankName, iban, bic, null)).thenReturn(externalAccountId);
        when(bankInfoPersistence.saveBankInfo(any(BankInfo.class), eq(userId))).thenReturn(bankInfo);

        // When
        BankInfo result = bankInfoService.createOrUpdateBankInfo(userId, userEmail, bankName, iban, bic);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(bankName, result.bankName());
        assertEquals(externalAccountId, result.externalAccountId());
        verify(payoutGateway, times(1)).createExternalAccount(userId, userEmail, bankName, iban, bic, null);
        verify(bankInfoPersistence, times(1)).saveBankInfo(any(BankInfo.class), eq(userId));
    }

    @Test
    void shouldGetBankInfoByUserIdSuccessfully() {
        // Given
        when(bankInfoPersistence.findByUserId(userId)).thenReturn(Optional.of(bankInfo));

        // When
        Optional<BankInfo> result = bankInfoService.getBankInfoByUserId(userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(bankInfo, result.get());
        verify(bankInfoPersistence, times(1)).findByUserId(userId);
    }

    @Test
    void shouldReturnEmptyWhenBankInfoNotFound() {
        // Given
        when(bankInfoPersistence.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        Optional<BankInfo> result = bankInfoService.getBankInfoByUserId(userId);

        // Then
        assertTrue(result.isEmpty());
        verify(bankInfoPersistence, times(1)).findByUserId(userId);
    }

    @Test
    void shouldDeleteBankInfoSuccessfully() {
        // Given
        UUID bankInfoId = UUID.randomUUID();
        doNothing().when(bankInfoPersistence).deleteBankInfo(bankInfoId);

        // When
        bankInfoService.deleteBankInfo(bankInfoId);

        // Then
        verify(bankInfoPersistence, times(1)).deleteBankInfo(bankInfoId);
    }
}
