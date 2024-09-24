package com.szilberhornz.valueinvdata.services.stockvaluation.core.record;

public record PriceTargetSummaryDTO (String ticker, int lastMonth, double lastMonthAvgPriceTarget,
                                     int lastQuarter, double lastQuarterAvgPriceTarget) {
}
