package fr.uge.forkeat.infrastructure.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        Connect connect,
        Test test
) {

    public record Connect(
            String country,
            String mcc,
            String businessType,
            boolean tosAccepted
    ) {}

    public record Test(
            Dob dob,
            Address address,
            Document document
    ) {}

    public record Dob(int day, int month, int year) {}

    public record Address(
            String line1,
            String city,
            String postalCode,
            String country
    ) {}

    public record Document(String front) {}
}
