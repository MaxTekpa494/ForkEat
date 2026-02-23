package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.presentation.dto.user.UserProfileDTO;
import fr.uge.forkeat.service.ProfileService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/profile")
public class ProfileWebController {

    private final AuthenticationPort authPort;
    private final ProfileService profileService;

    ProfileWebController(AuthenticationPort authPort, ProfileService profileService) {
        this.authPort = authPort;
        this.profileService = profileService;
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
        var dto = new UserProfileDTO(
                profileWithRecipes.profile(),
                profileWithRecipes.recipes().items(),
                profileWithRecipes.recipes().total(),
                page,
                totalPages,
                result.followedByCurrentUser()
        );
        model.addAttribute("vm", dto);
        return "profile/user";
    }
}