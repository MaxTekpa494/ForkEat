package fr.uge.forkeat.presentation.web.viewmodel;

import fr.uge.forkeat.service.model.recipe.projection.AuthorRecipeSummary;
import fr.uge.forkeat.service.model.recipe.projection.UserRecipeStats;

import java.util.ArrayList;
import java.util.List;

public record AuthorRecipesViewModel(
        UserRecipeStats stats,
        List<AuthorRecipeSummary> recipes,
        int currentPage,
        int totalPages,
        long totalRecipes,
        String currentStatus
) {
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
        if (start > 1) pages.add(-1);
        for (int i = start; i <= end; i++) pages.add(i);
        if (end < totalPages - 2) pages.add(-1);
        pages.add(totalPages - 1);
        return pages;
    }
}