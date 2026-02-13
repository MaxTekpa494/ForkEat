package fr.uge.forkeat.presentation.web.viewmodel;

import fr.uge.forkeat.presentation.dto.recipe.AllergenDTO;
import fr.uge.forkeat.presentation.dto.recipe.RecipeDTO;

import java.util.ArrayList;
import java.util.List;

public record RecipeListViewModel(List<RecipeDTO> recipes, int currentPage, int totalPages, long totalRecipes,
                                  String search, List<String> selectedAllergens, List<AllergenDTO> allAllergens) {
    /**
     * Retourne les numéros de page à afficher dans la pagination.
     * -1 représente une ellipse ("...").
     * Affiche : la première, la dernière, la page actuelle ± 1, avec des ellipses pour les écarts.
     */
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