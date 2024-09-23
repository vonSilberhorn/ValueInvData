package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.record;

public record PriceTargetConsensusDTO (String ticker, double targetHigh, double targetLow,
                                       double targetConsensus, double targetMedian) {
}
