package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.dto.user.UserProfileDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
import fr.uge.forkeat.service.ProfileService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import fr.uge.forkeat.service.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileWebController {

    private final AuthenticationPort authPort;
    private final ProfileService profileService;
    private final UserService userService;

    ProfileWebController(AuthenticationPort authPort, ProfileService profileService, UserService userService) {
        this.authPort = authPort;
        this.profileService = profileService;
        this.userService = userService;
    }

    @GetMapping
    public String profile(Model model) {
        var username = authPort.extractUsername();
        model.addAttribute("vm", profileService.getAccountDetails(username));
        return "profile/index";
    }

    @GetMapping("/{username}")
    public String userProfile(@PathVariable String username,
                              @RequestParam(name = "page", defaultValue = "0") int page,
                              @RequestParam(name = "size", defaultValue = "12") int size,
                              Model model) {
        var currentUsername = authPort.extractUsername();
        if (username.equals(currentUsername)) {
            return "redirect:/profile";
        }
        var result = profileService.getProfileInfos(username, page, size, currentUsername);
        var profileWithRecipes = result.profileWithRecipes();
        int totalPages = size > 0 ? (int) Math.ceil((double) profileWithRecipes.recipes().total() / size) : 0;
        var recipeDTOs = profileWithRecipes.recipes().items().stream()
                .map(RecipeDTOMapper::toSummaryDTO)
                .toList();
        var dto = new UserProfileDTO(
                profileWithRecipes.profile(),
                recipeDTOs,
                profileWithRecipes.recipes().total(),
                page,
                totalPages,
                result.followedByCurrentUser()
        );
        model.addAttribute("vm", dto);
        return "profile/user";
    }

    @PostMapping("/{username}/follow")
    public String follow(@PathVariable String username, RedirectAttributes redirectAttributes) {
        userService.follow(authPort.extractUsername(), username);
        redirectAttributes.addFlashAttribute("success", "Vous suivez maintenant " + username);
        return "redirect:/profile/" + username;
    }

    @PostMapping("/{username}/unfollow")
    public String unfollow(@PathVariable String username, RedirectAttributes redirectAttributes) {
        userService.unfollow(authPort.extractUsername(), username);
        redirectAttributes.addFlashAttribute("success", "Vous ne suivez plus " + username);
        return "redirect:/profile/" + username;
    }
}