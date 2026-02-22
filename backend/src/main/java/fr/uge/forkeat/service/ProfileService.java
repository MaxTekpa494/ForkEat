package fr.uge.forkeat.service;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.RecipeStatus;
import fr.uge.forkeat.service.model.recipe.RecipeUserInteraction;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.RecipeSummary;
import fr.uge.forkeat.service.model.user.projection.UserAccountDetails;
import fr.uge.forkeat.service.model.user.projection.PersonalizedUserProfile;
import fr.uge.forkeat.service.model.user.projection.UserProfileWithRecipes;
import fr.uge.forkeat.service.persistence.RecipePersistence;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final UserPersistence userPersistence;
    private final RecipePersistence recipePersistence;
    private final WalletService walletService;

    public ProfileService(UserPersistence userPersistence, RecipePersistence recipePersistence,
                          WalletService walletService) {
        this.userPersistence = userPersistence;
        this.recipePersistence = recipePersistence;
        this.walletService = walletService;
    }

    public PersonalizedUserProfile getProfileInfos(String username, int page, int size, String currentUsername) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(currentUsername);
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Invalid page or size");
        }
        var profile = userPersistence.findUserProfile(username);
        var summaries = recipePersistence.findRecipeSummaries(username, RecipeStatus.PUBLISHED, size, page);
        var ids = summaries.items().stream().map(RecipeSummary::id).toList();
        var interactions = recipePersistence.findUserRecipeInteractions(ids, currentUsername);
        var personalized = summaries.items().stream()
                .map(s -> {
                    var interaction = interactions.getOrDefault(s.id(), RecipeUserInteraction.NONE);
                    return new PersonalizedRecipeSummary(s, interaction.likedByCurrentUser(), interaction.superLikedByCurrentUser());
                })
                .toList();
        var recipes = new PageResult<>(personalized, summaries.total());
        var profileWithRecipes = new UserProfileWithRecipes(profile, recipes);
        var followedByCurrentUser = userPersistence.isFollowing(currentUsername, username);
        return new PersonalizedUserProfile(profileWithRecipes, followedByCurrentUser);
    }

    public UserAccountDetails getAccountDetails(String username) {
        Objects.requireNonNull(username);
        var user = userPersistence.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        var profile = userPersistence.findUserProfile(username);
        var balance = walletService.getBalance(user.id());
        var recipeCount = recipePersistence.countByAuthorUsername(username);
        return new UserAccountDetails(user, profile.socialStats(), balance, recipeCount);
    }
}