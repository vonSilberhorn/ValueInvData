package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;

/**
 * This is to generate humanly readable content with explanations, instead of just looking at JSONs.
 * Of course, it is only intended for demo purposes, as rendering would be the job of the frontend.
 */
public class ValuationResponseBodyExplainerFormatter implements ValuationResponseBodyFormatter {

    private static final String DCF_EXPLAINED = """
            On %s the discounted cash flow valuation model for the ticker %s\
             shows that the fair valuation per share is %.2f, while the current price per share is %.2f.\
            %s
            Find out more about the discounted cash flow valuation here: https://www.investopedia.com/terms/d/dcf.asp

            """;

    private static final String PTS_EXPLAINED = "Last month %d stock analysts made price target prediction about this stock, " +
            "with and average price target of %.2f. \nLast quarter %d analysts made predictions with %.2f average price target!\n%s \n";

    private static final String PTC_EXPLAINED = "Overall, the highest projection from any analyst was %.2f, while the lowest was %.2f, " +
            "with the consensus being around %.2f and the median prediction at %.2f \n" +
            "Price target predictions are the stock analysts own overall calculations for a price point where they think a " +
            "stock would be fairly valued. \nThis is based on a number of factors, you can find out more about those at https://www.investopedia.com/investing/target-prices-and-sound-investing/\n\n";

    @Override
    public String getFormattedResponseBody(final RecordHolder recordHolder, final String errorMessage) {
        if (recordHolder == null){
            if (errorMessage == null || errorMessage.isBlank()){
                return "Could not find any data!";
            } else {
                return errorMessage;
            }
        } else {
            final StringBuilder stringBuilder = new StringBuilder();
            double actualStockPrice = 0;
            if (recordHolder.getDiscountedCashFlowDto() != null){
                actualStockPrice = recordHolder.getDiscountedCashFlowDto().stockPrice();
                stringBuilder.append(this.getDcfExplanation(recordHolder.getDiscountedCashFlowDto()));
            }
            if (recordHolder.getPriceTargetSummaryDto() != null) {
                stringBuilder.append(this.getPtsExplanation(recordHolder.getPriceTargetSummaryDto(), actualStockPrice));
            }
            if (recordHolder.getPriceTargetConsensusDto() != null) {
                stringBuilder.append(this.getPtcExplanation(recordHolder.getPriceTargetConsensusDto()));
            }
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append("\nDisclaimer: this is solely for educational purposes and does not constitute as financial or investment advice!\n");
            }
            if (errorMessage != null && !errorMessage.isBlank()) {
                stringBuilder.append("Encountered the following issue while retrieving the data: ");
                stringBuilder.append(errorMessage);
            }
            return stringBuilder.toString();
        }
    }

    private String getPtcExplanation(final PriceTargetConsensusDTO ptcDto) {
        return String.format(PTC_EXPLAINED, ptcDto.targetHigh(), ptcDto.targetLow(), ptcDto.targetConsensus(), ptcDto.targetMedian());
    }

    private String getPtsExplanation(final PriceTargetSummaryDTO ptsDto, final double stockPrice){
        return String.format(PTS_EXPLAINED, ptsDto.lastMonth(), ptsDto.lastMonthAvgPriceTarget(), ptsDto.lastQuarter(), ptsDto.lastQuarterAvgPriceTarget(), this.ptsValuationMeaning(ptsDto, stockPrice));
    }

    private String getDcfExplanation(final DiscountedCashFlowDTO dcfDto) {
        return String.format(DCF_EXPLAINED, dcfDto.dateString(), dcfDto.ticker(), dcfDto.dcf(), dcfDto.stockPrice(), this.dcfValuationMeaning(dcfDto));
    }

    private String ptsValuationMeaning(final PriceTargetSummaryDTO ptsDto, final double stockPrice){
        if (stockPrice != 0) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("This suggests that analyst believe the stock price ");
            final double avgPrediction = (ptsDto.lastMonthAvgPriceTarget() + ptsDto.lastQuarterAvgPriceTarget()) / 2;
            if (avgPrediction*0.9 < stockPrice && stockPrice < avgPrediction*1.1) {
                stringBuilder.append("is very close to its fair value so holding or selling might be better options than buying.");
            } else if (avgPrediction > stockPrice) {
                stringBuilder.append("has a potential to climb in the future, which makes it a candidate to buy and hold.");
            } else if (stockPrice > avgPrediction){
                stringBuilder.append("may be overvalued and not a good candidate for buying.");
            }
            return stringBuilder.toString();
        } else {
            return "";
        }
    }

    private String dcfValuationMeaning(final DiscountedCashFlowDTO dcfDto) {
        final StringBuilder verboseValuation = new StringBuilder();
        if (dcfDto.dcf() > dcfDto.stockPrice()) {
            verboseValuation.append("\nThis means that the company seems to be undervalued on the stock market and may be " +
                    "considered a candidate to buy or hold");
        } else if (dcfDto.dcf() < dcfDto.stockPrice()) {
            verboseValuation.append("\nThis means that the company seems to be overvalued on the stock market and may be " +
                    "considered a candidate to sell.");
        } else {
            verboseValuation.append("\nThis means that the company seems to be fairly valued on the stock market and is " +
                    "neither a good candidate to sell or buy");
        }
        verboseValuation.append("\nIt is advised to look for other valuation methods too, especially if the spread between the valuation price and the actual stock price is large.");
        return verboseValuation.toString();
    }
}
