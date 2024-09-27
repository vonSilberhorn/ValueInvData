package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import org.json.JSONObject;

/**
 * This would be the basic formatting for responses for an app running in prod.
 */
public class ValuationResponseBodyJSONFormatter implements ValuationResponseBodyFormatter {

    @Override
    public String getFormattedResponseBody(final RecordHolder recordHolder, final String errorString) {
        final JSONObject result = new JSONObject();
        if (recordHolder != null) {
            result.put("ticker", recordHolder.getTicker());
            if (recordHolder.getDiscountedCashFlowDto() != null) {
                final JSONObject dcfJson = new JSONObject();
                dcfJson.put("date", recordHolder.getDiscountedCashFlowDto().dateString());
                dcfJson.put("dcf", recordHolder.getDiscountedCashFlowDto().dcf());
                dcfJson.put("stockPrice", recordHolder.getDiscountedCashFlowDto().stockPrice());
                result.put("discountedCashFlow", dcfJson);
            }
            if (recordHolder.getPriceTargetConsensusDto() != null) {
                final JSONObject ptcJson = new JSONObject();
                ptcJson.put("targetHigh", recordHolder.getPriceTargetConsensusDto().targetHigh());
                ptcJson.put("targetLow", recordHolder.getPriceTargetConsensusDto().targetLow());
                ptcJson.put("targetConsensus", recordHolder.getPriceTargetConsensusDto().targetConsensus());
                ptcJson.put("targetMedian", recordHolder.getPriceTargetConsensusDto().targetMedian());
                result.put("priceTargetConsensus", ptcJson);
            }
            if (recordHolder.getPriceTargetSummaryDto() != null) {
                final JSONObject ptsJson = new JSONObject();
                ptsJson.put("lastMonth", recordHolder.getPriceTargetSummaryDto().lastMonth());
                ptsJson.put("lastMonthAvgPriceTarget", recordHolder.getPriceTargetSummaryDto().lastMonthAvgPriceTarget());
                ptsJson.put("lastQuarter", recordHolder.getPriceTargetSummaryDto().lastQuarter());
                ptsJson.put("lastQuarterAvgPriceTarget", recordHolder.getPriceTargetSummaryDto().lastQuarterAvgPriceTarget());
                result.put("priceTargetConsensus", ptsJson);
            }
        }
        if (errorString != null && !errorString.isBlank()){
            result.put("error", errorString);
        }
        return result.toString();
    }
}
