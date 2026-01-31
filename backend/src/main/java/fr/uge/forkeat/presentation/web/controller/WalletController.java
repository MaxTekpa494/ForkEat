package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.UserService;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller("walletWebController")
@RequestMapping("/wallet")
public class WalletController {

    private final UserService userService;
    private final WalletService walletService;

    public WalletController(UserService userService, WalletService walletService) {
        this.userService = userService;
        this.walletService = walletService;
    }

    @GetMapping
    public String walletPage(
        @AuthenticationPrincipal UserDetails currentUser,
        Model model
    ) throws ResourceNotFoundException {
        var user = userService.getUserByUsername(currentUser.getUsername());
        var balance = walletService.getBalance(user.id());
        
        // TODO: Récupérer l'historique des transactions
        // List<Transaction> transactions = walletService.getTransactionHistory(user.id());
        
        model.addAttribute("user", user);
        model.addAttribute("balance", balance);
        // model.addAttribute("transactions", transactions);
        model.addAttribute("pageTitle", "Mon Wallet - ForkEat");

        return "wallet/index";
    }
}