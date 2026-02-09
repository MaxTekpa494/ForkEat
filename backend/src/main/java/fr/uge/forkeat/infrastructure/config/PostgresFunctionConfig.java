package fr.uge.forkeat.infrastructure.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostgresFunctionConfig implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
                "ts_match",
                "to_tsvector('french', ?1) @@ plainto_tsquery('french', ?2)",
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
        );
        functionContributions.getFunctionRegistry().registerPattern(
                "ts_rank",
                "ts_rank(to_tsvector('french', ?1), plainto_tsquery('french', ?2))",
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.DOUBLE)
        );
    }
}
