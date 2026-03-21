package fr.uge.forkeat.presentation.dto.redistribution;

import java.util.List;

public record EarningsByMonthDTO(
        String batchMonth,
        long totalCents,
        List<EarningsDetailDTO> details
) {}