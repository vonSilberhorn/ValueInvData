package com.szilberhornz.valueinvdata.services.stockvaluation.model.record;

/**
 * A record class representing the data coming from the price target consensus endpoint of the Financial Modeling Prep api
 */
public record PriceTargetConsensusDTO (String ticker, double targetHigh, double targetLow,
                                       double targetConsensus, double targetMedian) {
}
