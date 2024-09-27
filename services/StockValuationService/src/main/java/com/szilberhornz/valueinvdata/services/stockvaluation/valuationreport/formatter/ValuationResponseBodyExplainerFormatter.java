package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import org.json.JSONObject;

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

    private static final String PTS_EXPLAINED = """
            Last month %d stock analysts made price target prediction about this stock, \
            with and average price target of %.2f.\s
            Last quarter %d analysts made predictions with %.2f average price target!
            %s\s
            """;

    private static final String PTC_EXPLAINED = """
            Overall, the highest projection from any analyst was %.2f, while the lowest was %.2f, \
            with the consensus being around %.2f and the median prediction at %.2f\s
            Price target predictions are the stock analysts own overall calculations for a price point where they think a \
            stock would be fairly valued.\s
            This is based on a number of factors, you can find out more about those at https://www.investopedia.com/investing/target-prices-and-sound-investing/""";

    private static final double NINETY_PERCENT = 0.9;
    private static final double HUNDERD_AND_TEN_PERCENT = 1.1;

    @Override
    public String getFormattedResponseBody(final RecordHolder recordHolder, final String errorMessage) {
        if (recordHolder == null || recordHolder.getDtoCount() == 0) {
            if (errorMessage == null || errorMessage.isBlank()) {
                return "Could not find any data!";
            } else {
                final JSONObject temporalJsonFormatter = new JSONObject(errorMessage);
                if (temporalJsonFormatter.has("Error Message")) {
                    //some error from the FMP Api
                    return temporalJsonFormatter.getString("Error Message");
                } else {
                    //invalid ticker query
                    return errorMessage;
                }

            }
        } else {
            final StringBuilder stringBuilder = new StringBuilder();
            double actualStockPrice = 0;
            final boolean hasDcfData = recordHolder.getDiscountedCashFlowDto() != null;
            final boolean hasPtsData = recordHolder.getPriceTargetSummaryDto() != null;
            final boolean hasPtcData = recordHolder.getPriceTargetConsensusDto() != null;
            if (hasDcfData) {
                actualStockPrice = recordHolder.getDiscountedCashFlowDto().stockPrice();
                stringBuilder.append(this.getDcfExplanation(recordHolder.getDiscountedCashFlowDto()));
            }
            if (hasPtsData) {
                stringBuilder.append(this.getPtsExplanation(recordHolder.getPriceTargetSummaryDto(), actualStockPrice));
            }
            if (hasPtcData) {
                stringBuilder.append(this.getPtcExplanation(recordHolder.getPriceTargetConsensusDto()));
            }
            //special case when we get api key related issues from FMP Api. This would normally be an internal thing
            //since we should own the api key, but for demo version, when the user supplies the key, we should display these issues
            if (hasDcfData && !hasPtcData && !hasPtsData && !errorMessage.isBlank()) {
                stringBuilder.append("Unfortunately no more data is available at this time, as the FMP Api returned the following response for the " +
                        "PriceTargetSummary and PriceTargetConsensus calls: ");
                final JSONObject temporalJsonFormatter = new JSONObject(errorMessage);
                if (temporalJsonFormatter.has("Error Message")) {
                    stringBuilder.append(temporalJsonFormatter.getString("Error Message"));
                } else {
                    stringBuilder.append(errorMessage);
                }
            } else if (errorMessage != null && !errorMessage.isBlank()) {
                final JSONObject temporalJsonFormatter = new JSONObject(errorMessage);
                if (temporalJsonFormatter.has("Error Message")) {
                    stringBuilder.append(temporalJsonFormatter.getString("Error Message"));
                } else {
                    stringBuilder.append("Encountered the following issue while retrieving the data: ");
                    stringBuilder.append(errorMessage);
                }
            }
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append("\n\nDisclaimer: this is solely for educational purposes and does not constitute as financial or investment advice!\n");
            }
            return stringBuilder.toString();
        }
    }

    private String getPtcExplanation(final PriceTargetConsensusDTO ptcDto) {
        return String.format(PTC_EXPLAINED, ptcDto.targetHigh(), ptcDto.targetLow(), ptcDto.targetConsensus(), ptcDto.targetMedian());
    }

    private String getPtsExplanation(final PriceTargetSummaryDTO ptsDto, final double stockPrice) {
        return String.format(PTS_EXPLAINED, ptsDto.lastMonth(), ptsDto.lastMonthAvgPriceTarget(), ptsDto.lastQuarter(), ptsDto.lastQuarterAvgPriceTarget(), this.ptsValuationMeaning(ptsDto, stockPrice));
    }

    private String getDcfExplanation(final DiscountedCashFlowDTO dcfDto) {
        return String.format(DCF_EXPLAINED, dcfDto.dateString(), dcfDto.ticker(), dcfDto.dcf(), dcfDto.stockPrice(), this.dcfValuationMeaning(dcfDto));
    }

    private String ptsValuationMeaning(final PriceTargetSummaryDTO ptsDto, final double stockPrice) {
        if (stockPrice != 0) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("This suggests that analyst believe the stock price ");
            final double avgPrediction = (ptsDto.lastMonthAvgPriceTarget() + ptsDto.lastQuarterAvgPriceTarget()) / 2;
            if (avgPrediction * NINETY_PERCENT < stockPrice && stockPrice < avgPrediction * HUNDERD_AND_TEN_PERCENT) {
                stringBuilder.append("is very close to its fair value so holding or selling might be better options than buying.");
            } else if (avgPrediction > stockPrice) {
                stringBuilder.append("has a potential to climb in the future, which makes it a candidate to buy and hold.");
            } else if (stockPrice > avgPrediction) {
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
