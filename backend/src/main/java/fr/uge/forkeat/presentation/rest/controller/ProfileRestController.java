package fr.uge.forkeat.presentation.rest.controller;

import fr.uge.forkeat.presentation.dto.user.UserDashboardDTO;
import fr.uge.forkeat.presentation.dto.user.UserProfileDTO;
import fr.uge.forkeat.presentation.mapper.rest.RecipeDTOMapper;
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

    @GetMapping
    public ResponseEntity<HttpResponse<UserDashboardDTO>> getMyProfile() {
        var username = authenticationPort.extractUsername();
        var details = profileService.getAccountDetails(username);
        return ResponseEntity.ok(new ItemResponse<>(UserDashboardDTO.from(details)));
    }

    @GetMapping("/{username}")
    public ResponseEntity<HttpResponse<UserProfileDTO>> getProfile(
            @PathVariable String username,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        var currentUsername = authenticationPort.extractUsername();
        var result = profileService.getProfileInfos(username, page, size, currentUsername);
        var profileWithRecipes = result.profileWithRecipes();

        int totalPages = size > 0 ? (int) Math.ceil((double) profileWithRecipes.recipes().total() / size) : 0;

        var dto = new UserProfileDTO(
                profileWithRecipes.profile(),
                profileWithRecipes.recipes().items().stream().map(RecipeDTOMapper::toSummaryDTO).toList(),
                profileWithRecipes.recipes().total(),
                page,
                totalPages,
                result.followedByCurrentUser()
        );

        return ResponseEntity.ok(new ItemResponse<>(dto));
    }
}