package fr.uge.forkeat.presentation.dto.recipe;

import java.util.List;

public final class RecipeSearchDTO extends RecipePaginationDTO {
    private String search;
    private List<String> allergens = List.of();

    public RecipeSearchDTO() {}

    public RecipeSearchDTO(String status, int size, int page, String search, List<String> allergens) {
        setStatus(status);
        setSize(size);
        setPage(page);
        setSearch(search);
        setAllergens(allergens);
    }

    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }

    public List<String> getAllergens() { return allergens; }
    public void setAllergens(List<String> allergens) {
        if (allergens != null) {
            this.allergens = allergens;
        }
    }
}