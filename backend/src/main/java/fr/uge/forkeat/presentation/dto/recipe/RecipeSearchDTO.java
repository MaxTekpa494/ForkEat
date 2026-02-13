package fr.uge.forkeat.presentation.dto.recipe;

import java.util.List;

public class RecipeSearchDTO {
    private String status = "PUBLISHED";
    private int size = 12;
    private int page = 0;
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

    public String getStatus() { return status; }
    public void setStatus(String status) {
        if (status != null && !status.isBlank()) {
            this.status = status;
        }
    }

    public int getSize() { return size; }
    public void setSize(int size) {
        if (size > 0) {
            this.size = size;
        }
    }

    public int getPage() { return page; }
    public void setPage(int page) {
        if (page >= 0) {
            this.page = page;
        }
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