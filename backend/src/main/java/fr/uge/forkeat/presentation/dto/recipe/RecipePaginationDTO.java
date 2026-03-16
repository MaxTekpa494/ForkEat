package fr.uge.forkeat.presentation.dto.recipe;

public sealed class RecipePaginationDTO permits RecipeSearchDTO {
    private String status = "PUBLISHED";
    private int size = 12;
    private int page = 0;

    public RecipePaginationDTO() {}

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
}