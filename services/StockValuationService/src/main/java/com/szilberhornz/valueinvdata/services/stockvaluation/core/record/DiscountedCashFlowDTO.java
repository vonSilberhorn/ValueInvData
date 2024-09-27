package com.szilberhornz.valueinvdata.services.stockvaluation.core.record;

/**
 * A record class representing the data coming from the discounted cashflow endpoint of the Financial Modeling Prep api
 */
public record DiscountedCashFlowDTO(String ticker, String dateString, double dcf, double stockPrice) {

}
