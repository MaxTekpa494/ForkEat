package fr.uge.forkeat.infrastructure.scheduler;

import fr.uge.forkeat.service.RedistributionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class RedistributionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RedistributionScheduler.class);

    private final RedistributionService redistributionService;

    public RedistributionScheduler(RedistributionService redistributionService) {
        this.redistributionService = redistributionService;
    }

    @Scheduled(cron = "0 0 1 1 * *")
    public void runMonthlyRedistribution() {
        var batchMonth = YearMonth.now().minusMonths(1).toString();
        logger.info("[RedistributionScheduler] Starting monthly redistribution for month: {}", batchMonth);
        redistributionService.processAllPending(batchMonth);
    }
}