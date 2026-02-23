package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.WithdrawalException;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.BankInfoService;
import fr.uge.forkeat.service.user.UserQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller("walletWebController")
public class WalletWebController {

    private final UserQueryService userQueryService;
    private final WalletService walletService;
    private final BankInfoService bankInfoService;
    private final AuthenticationPort authPort;

    public WalletWebController(
            UserQueryService userQueryService,
            WalletService walletService,
            BankInfoService bankInfoService,
            AuthenticationPort authPort
    ) {
        this.userQueryService = Objects.requireNonNull(userQueryService);
        this.walletService = Objects.requireNonNull(walletService);
        this.bankInfoService = Objects.requireNonNull(bankInfoService);
        this.authPort = Objects.requireNonNull(authPort);
    }

    @GetMapping("/wallet")
    public String walletPage(
            @RequestParam(name = "payment", required = false) String payment,
            Model model
    ) {
        var username = authPort.extractUsername();
        var user = userQueryService.getUserByUsername(username);
        var balance = walletService.getBalance(user.id());
        var transactions = walletService.getTransactionHistory(user.id());
        var bankInfo = bankInfoService.getBankInfoByUserId(user.id()).orElse(null);

        model.addAttribute("user", user);
        model.addAttribute("balance", balance);
        model.addAttribute("transactions", transactions);
        model.addAttribute("bankInfo", bankInfo);
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
        var user = userQueryService.getUserByUsername(username);

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
        var user = userQueryService.getUserByUsername(username);

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
        var user = userQueryService.getUserByUsername(username);

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
