package fr.uge.forkeat.service.model.user.projection;

import fr.uge.forkeat.service.model.PageResult;
import fr.uge.forkeat.service.model.recipe.projection.PersonalizedRecipeSummary;

import java.util.Objects;

public record UserProfileWithRecipes(
        UserProfile profile,
        PageResult<PersonalizedRecipeSummary> recipes
) {

    public UserProfileWithRecipes {
        Objects.requireNonNull(profile);
        Objects.requireNonNull(recipes);
    }
}
