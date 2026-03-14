package fr.uge.forkeat.service.exception;

public class PromotionNotProfitableException extends DomainException {
    public PromotionNotProfitableException(int bonusEveryN, double earningsRatio) {
        super(String.format(
                "This promotion would cause the platform to lose money. " +
                "With an earnings ratio of %.0f%%, the bonus threshold must be at least %d (currently %d). " +
                "Rule: bonusEveryN × earningsRatio > (1 - earningsRatio).",
                earningsRatio * 100,
                (int) Math.floor((1 - earningsRatio) / earningsRatio) + 1,
                bonusEveryN
        ));
    }
}
