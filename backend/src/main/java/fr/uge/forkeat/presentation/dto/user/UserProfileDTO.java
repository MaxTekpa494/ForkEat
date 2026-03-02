package fr.uge.forkeat.presentation.dto.user;

import fr.uge.forkeat.presentation.dto.recipe.PersonalizedRecipeSummaryDTO;
import fr.uge.forkeat.service.model.user.projection.UserProfile;

import java.util.ArrayList;
import java.util.List;

public record UserProfileDTO(
        UserProfile profile,
        List<PersonalizedRecipeSummaryDTO> recipes,
        long totalRecipes,
        int currentPage,
        int totalPages,
        boolean followedByCurrentUser
) {
    public String initials() {
        var pub = profile.publicProfile();
        var first = pub.firstName() != null && !pub.firstName().isEmpty() ? pub.firstName().substring(0, 1).toUpperCase() : "";
        var last = pub.lastName() != null && !pub.lastName().isEmpty() ? pub.lastName().substring(0, 1).toUpperCase() : "";
        return first + last;
    }

    public List<Integer> paginationPages() {
        if (totalPages <= 7) {
            var pages = new ArrayList<Integer>(totalPages);
            for (int i = 0; i < totalPages; i++) {
                pages.add(i);
            }
            return pages;
        }
        var pages = new ArrayList<Integer>();
        pages.add(0);
        var start = Math.max(1, currentPage - 1);
        var end = Math.min(totalPages - 2, currentPage + 1);
        if (start > 1) {
            pages.add(-1);
        }
        for (int i = start; i <= end; i++) {
            pages.add(i);
        }
        if (end < totalPages - 2) {
            pages.add(-1);
        }
        pages.add(totalPages - 1);
        return pages;
    }
}
