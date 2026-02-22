package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.user.TopUpRequestDTO;
import fr.uge.forkeat.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController("walletRestController")
@RequestMapping("/wallet")
public class WalletRestController {

    private final WalletService walletService;

    public WalletRestController(WalletService walletService) {
        this.walletService = Objects.requireNonNull(walletService);
    }

    // @PostMapping("/top-up")
    // public ResponseEntity<Map<String, String>> demarrerRechargement(
    // @AuthenticationPrincipal UserEntity user, // L'utilisateur connecté
    // @RequestBody TopUpRequestDto request
    // ) {
    // // On appelle le service
    // var paymentUrl = walletService.prepareTopUp(
    // user.getId(),
    // user.getEmail(),
    // request.amount(),
    // "EUR"
    // );
    //
    // // On renvoie l'URL au front
    // return ResponseEntity.ok(Map.of("url", paymentUrl));
    // }

    @PostMapping("/recharge")
    public ResponseEntity<Map<String, String>> demarrerRechargement(@RequestBody TopUpRequestDTO request) {
        // --- MODE TEST ---
        // Comme on a désactivé la sécurité, on force l'utilisateur ID 1
        var userId = UUID.fromString("0a6a42d0-696b-4fa6-aa8e-40ba65a660ca");
        var userEmail = "test@user.com";

        System.out.println("BYPASS SÉCURITÉ : Paiement pour le User ID " + userId);

        var paymentUrl = walletService.prepareTopUp(userId, userEmail, request.amount());

        return ResponseEntity.ok(Map.of("url", paymentUrl));
    }
}