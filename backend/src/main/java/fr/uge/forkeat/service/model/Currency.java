package fr.uge.forkeat.service.model;

public enum Currency {
    EUR("eur", "€", "Euro");

    private final String code;
    private final String symbol;
    private final String displayName;
    
    Currency(String code, String symbol, String displayName) {
        this.code = code;
        this.symbol = symbol;
        this.displayName = displayName;
    }
    
    public String code() { return code; }
    public String symbol() { return symbol; }
    public String displayName() { return displayName; }
    
    public static final Currency DEFAULT = EUR;
}