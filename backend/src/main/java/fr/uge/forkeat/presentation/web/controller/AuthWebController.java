package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.web.form.RegisterFormDTO;
import fr.uge.forkeat.service.user.UserRegistrationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import fr.uge.forkeat.presentation.mapper.web.UserFormDTOMapper;

@Controller
@RequestMapping("/auth")
public class AuthWebController {

  private final Logger logger = LoggerFactory.getLogger(AuthWebController.class);
  private final UserRegistrationService userRegistrationService;

  public AuthWebController(UserRegistrationService userRegistrationService) {
    this.userRegistrationService = userRegistrationService;
  }

  @GetMapping("/login")
  public String loginPage() {
    if (isAuthenticated()) {
      return "redirect:/dashboard";
    }
    logger.debug("Loading user by username: ");
    return "layout/login";
  }

  @GetMapping("/register")
  public String registerPage(Model model) {
    if (isAuthenticated()) {
      return "redirect:/dashboard";
    }
    model.addAttribute("registerForm", new RegisterFormDTO());
    return "layout/register";
  }

  @PostMapping("/register")
  public String register(@Valid @ModelAttribute RegisterFormDTO form, BindingResult result,
                         RedirectAttributes redirectAttributes) {
    if (result.hasErrors()) {
      return "layout/register";
    }

    userRegistrationService.registerUser(UserFormDTOMapper.toUserRegister(form));
    redirectAttributes.addFlashAttribute("success",
            "Compte créé avec succès ! Vous pouvez maintenant vous connecter.");
    return "redirect:/login";
  }

  private boolean isAuthenticated() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || AnonymousAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
      return false;
    }
    return authentication.isAuthenticated();
  }
}