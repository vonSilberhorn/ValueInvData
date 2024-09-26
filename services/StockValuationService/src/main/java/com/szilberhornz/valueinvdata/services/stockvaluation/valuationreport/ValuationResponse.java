package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.StockValuationServiceResponse;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter.ValuationResponseBodyFormatter;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter.ValuationResponseBodyJSONFormatter;
import org.jetbrains.annotations.Nullable;

public class ValuationResponse implements StockValuationServiceResponse {

    private RecordHolder recordHolder;
    private int statusCode;
    private String errorMessage;
    //the default is JSON, can be overwritten with the builder
    private ValuationResponseBodyFormatter responseBodyFormatter = new ValuationResponseBodyJSONFormatter();

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }

    @Override
    @Nullable
    public String getMessageBody() {
        return this.responseBodyFormatter.getFormattedResponseBody(this.recordHolder);
    }

    @Override
    @Nullable
    public String getErrorMessage() {
        return this.errorMessage;
    }

    private ValuationResponse() {
        //instantiate with builder
    }

    public static class Builder {

        private final ValuationResponse response = new ValuationResponse();

        public ValuationResponse.Builder statusCode(final int statusCode) {
            this.response.statusCode = statusCode;
            return this;
        }

        public ValuationResponse.Builder recordHolder(final RecordHolder recordHolder){
            this.response.recordHolder = recordHolder;
            return this;
        }

        public ValuationResponse.Builder errorMessage(final String errorMessage){
            this.response.errorMessage = errorMessage;
            return this;
        }

        public ValuationResponse.Builder responseBodyFormatter(final ValuationResponseBodyFormatter responseBodyFormatter){
            this.response.responseBodyFormatter = responseBodyFormatter;
            return this;
        }

        public ValuationResponse build(){
            return this.response;
        }
    }
}
