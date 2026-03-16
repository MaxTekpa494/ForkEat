package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.PromotionService;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.WithdrawalException;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.BankInfoService;
import fr.uge.forkeat.service.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller("walletWebController")
public class WalletWebController {

    private final UserService userService;
    private final WalletService walletService;
    private final BankInfoService bankInfoService;
    private final AuthenticationPort authPort;
    private final PromotionService promotionService;

    public WalletWebController(
            UserService userService,
            WalletService walletService,
            BankInfoService bankInfoService,
            AuthenticationPort authPort,
            PromotionService promotionService
    ) {
        this.userService = Objects.requireNonNull(userService);
        this.walletService = Objects.requireNonNull(walletService);
        this.bankInfoService = Objects.requireNonNull(bankInfoService);
        this.authPort = Objects.requireNonNull(authPort);
        this.promotionService = Objects.requireNonNull(promotionService);
    }

    @GetMapping("/wallet")
    public String walletPage(
            @RequestParam(name = "payment", required = false) String payment,
            Model model
    ) {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);
        var balance = walletService.getBalance(user.id());
        var transactions = walletService.getTransactionHistory(user.id());
        var bankInfo = bankInfoService.getBankInfoByUserId(user.id()).orElse(null);
        var superLikeHistory = promotionService.findSuperLikeHistory(user.id());

        model.addAttribute("user", user);
        model.addAttribute("balance", balance);
        model.addAttribute("transactions", transactions);
        model.addAttribute("bankInfo", bankInfo);
        model.addAttribute("superLikeHistory", superLikeHistory);
        model.addAttribute("pageTitle", "Mon Wallet - ForkEat");

        if ("success".equals(payment)) {
            model.addAttribute("paymentSuccess", true);
        } else if ("cancel".equals(payment)) {
            model.addAttribute("paymentCancel", true);
        }

        return "wallet/index";
    }

    @PostMapping("/wallet/recharge")
    public String recharge(@RequestParam("amount") Long amountInEuros) {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);

        var amountInCents = amountInEuros * 100;
        var paymentUrl = walletService.prepareTopUp(user.id(), user.email(), amountInCents, null);

        return "redirect:" + paymentUrl;
    }

    @PostMapping("/wallet/bank-info")
    public String saveBankInfo(
            @RequestParam("bankName") String bankName,
            @RequestParam("iban") String iban,
            @RequestParam("bic") String bic,
            RedirectAttributes redirectAttributes
    ) {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);

        try {
            bankInfoService.createOrUpdateBankInfo(user.id(), user.email(), bankName, iban.toUpperCase(), bic.toUpperCase());
            redirectAttributes.addFlashAttribute("bankInfoSuccess", true);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("bankInfoError", e.getMessage());
        }

        return "redirect:/wallet";
    }

    @PostMapping("/wallet/withdraw")
    public String withdraw(
            @RequestParam("amount") Long amountInEuros,
            RedirectAttributes redirectAttributes
    ) {
        var username = authPort.extractUsername();
        var user = userService.getUserByUsername(username);

        try {
            var amountInCents = amountInEuros * 100;
            walletService.requestWithdrawal(user.id(), amountInCents);
            redirectAttributes.addFlashAttribute("withdrawalSuccess", true);
        } catch (WithdrawalException e) {
            redirectAttributes.addFlashAttribute("withdrawalError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("withdrawalError", "Une erreur est survenue lors du retrait.");
        }

        return "redirect:/wallet";
    }
}