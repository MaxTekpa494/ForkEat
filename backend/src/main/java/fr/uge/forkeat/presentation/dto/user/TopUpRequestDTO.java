package fr.uge.forkeat.presentation.dto.user;


public record TopUpRequestDTO(Long amount, String source) {
    public TopUpRequestDTO {
        if(amount < 100L){
            throw new IllegalArgumentException("Amount must be at least 1 euro");
        }
    }
}