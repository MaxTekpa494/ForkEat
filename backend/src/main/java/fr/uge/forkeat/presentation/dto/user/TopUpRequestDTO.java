package fr.uge.forkeat.presentation.rest.dto;


public record TopUpRequestDto(Long amount) {
    public TopUpRequestDto{
        if(amount < 100L){
            throw new IllegalArgumentException("Amount must be at least 1 euro");
        }
    }
}