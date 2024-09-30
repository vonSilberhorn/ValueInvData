package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter;

import com.szilberhornz.valueinvdata.services.stockvaluation.utility.cache.RecordHolder;

public interface ValuationResponseBodyFormatter {

    String getFormattedResponseBody(RecordHolder recordHolder, String errorString);
}
