package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.user.TopUpRequestDTO;
import fr.uge.forkeat.presentation.dto.user.CreateBankInfoRequestDTO;
import fr.uge.forkeat.presentation.dto.user.BankInfoResponseDTO;
import fr.uge.forkeat.presentation.dto.user.WithdrawalRequestDTO;
import fr.uge.forkeat.presentation.mapper.rest.BankInfoDTOMapper;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.exception.WithdrawalException;
import fr.uge.forkeat.service.model.Transaction;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.BankInfoService;
import fr.uge.forkeat.service.user.UserQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController("walletRestController")
@RequestMapping("/api/wallet")
public class WalletRestController {

    private final WalletService walletService;
    private final AuthenticationPort authPort;
    private final UserQueryService userQueryService;
    private final BankInfoService bankInfoService; // New injected dependency

    public WalletRestController(WalletService walletService, AuthenticationPort authPort,
                                UserQueryService userQueryService, BankInfoService bankInfoService) { // Added BankInfoService
        this.walletService = walletService;
        this.authPort = authPort;
        this.userQueryService = userQueryService;
        this.bankInfoService = bankInfoService; // Assign BankInfoService
    }

    @PostMapping("/recharge")
    public ResponseEntity<Map<String, String>> recharge(@RequestBody TopUpRequestDTO request) {
        var user = userQueryService.getUserByUsername(authPort.extractUsername());
        var paymentUrl = walletService.prepareTopUp(user.id(), user.email(), request.amount(), request.source());
        return ResponseEntity.ok(Map.of("url", paymentUrl));
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Long>> getBalance() {
        var user = userQueryService.getUserByUsername(authPort.extractUsername());
        var balance = walletService.getBalance(user.id());
        return ResponseEntity.ok(Map.of("balance", balance));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactions() {
        var user = userQueryService.getUserByUsername(authPort.extractUsername());
        var transactions = walletService.getTransactionHistory(user.id());
        return ResponseEntity.ok(transactions);
    }

    // New Endpoint: Create or Update Bank Information
    @PostMapping("/bank-info")
    public ResponseEntity<BankInfoResponseDTO> createOrUpdateBankInfo(@RequestBody @Valid CreateBankInfoRequestDTO request) {
        var user = userQueryService.getUserByUsername(authPort.extractUsername());
        var bankInfo = bankInfoService.createOrUpdateBankInfo(user.id(), user.email(), request.bankName(), request.iban(), request.bic());
        return ResponseEntity.status(HttpStatus.CREATED).body(BankInfoDTOMapper.toResponseDTO(bankInfo));
    }

    // New Endpoint: Get Bank Information
    @GetMapping("/bank-info")
    public ResponseEntity<BankInfoResponseDTO> getBankInfo() {
        var user = userQueryService.getUserByUsername(authPort.extractUsername());
        var bankInfo = bankInfoService.getBankInfoByUserId(user.id())
                .orElseThrow(() -> new ResourceNotFoundException("Bank information not found for user: " + user.id()));
        return ResponseEntity.ok(BankInfoDTOMapper.toResponseDTO(bankInfo));
    }

    // New Endpoint: Request Withdrawal
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, String>> requestWithdrawal(@RequestBody @Valid WithdrawalRequestDTO request) {
        var user = userQueryService.getUserByUsername(authPort.extractUsername());
        try {
            String payoutId = walletService.requestWithdrawal(user.id(), request.amount());
            return ResponseEntity.ok(Map.of("message", "Withdrawal initiated successfully", "payoutId", payoutId));
        } catch (WithdrawalException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}