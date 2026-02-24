package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.user.UserProfileDTO;
import fr.uge.forkeat.presentation.response.HttpResponse;
import fr.uge.forkeat.presentation.response.ItemResponse;
import fr.uge.forkeat.service.ProfileService;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileRestController {

    private final ProfileService profileService;
    private final AuthenticationPort authenticationPort;

    public ProfileRestController(ProfileService profileService, AuthenticationPort authenticationPort) {
        this.profileService = profileService;
        this.authenticationPort = authenticationPort;
    }

    @GetMapping("/{username}")
    public ResponseEntity<HttpResponse<UserProfileDTO>> getProfile(
            @PathVariable String username,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var result = profileService.getProfileInfos(username, page, size, authenticationPort.extractUsername());
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

        return ResponseEntity.ok(new ItemResponse<>(dto));
    }
}