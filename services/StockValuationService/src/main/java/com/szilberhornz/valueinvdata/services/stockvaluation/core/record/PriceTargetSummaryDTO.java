package com.szilberhornz.valueinvdata.services.stockvaluation.core.record;

/**
 * A record class representing the data coming from the price target summary endpoint of the Financial Modeling Prep api
 */
public record PriceTargetSummaryDTO (String ticker, int lastMonth, double lastMonthAvgPriceTarget,
                                     int lastQuarter, double lastQuarterAvgPriceTarget) {
}
