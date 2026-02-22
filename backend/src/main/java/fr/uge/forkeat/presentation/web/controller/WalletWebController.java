package fr.uge.forkeat.presentation.web.controller;
import fr.uge.forkeat.service.WalletService;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller("walletWebController")
public class WalletWebController {

    private final UserService userService;
    private final WalletService walletService;
    private final AuthenticationPort authPort;

    public WalletWebController(
            UserService userService,
            WalletService walletService,
            AuthenticationPort authPort
    ) {
        this.userService = userService;
        this.walletService = walletService;
        this.authPort = authPort;
    }

    @GetMapping("/wallet")
    public String walletPage(
            Authentication authentication,
            Model model
    ) throws ResourceNotFoundException {
        var username = authPort.extractUsername();

        var user = userService.getUserByUsername(username);
        var balance = walletService.getBalance(user.id());

        model.addAttribute("user", user);
        model.addAttribute("balance", balance);
        model.addAttribute("pageTitle", "Mon Wallet - ForkEat");

        return "wallet/index";
    }
}