package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.HttpStatusCode;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter.ValuationResponseBodyJSONFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValuationResponseTest {

    @Test
    void testFullResponse() {
        final String expectedJsonString = "{\"ticker\":\"DUMMY\",\"error\":\"testMsg\"}";
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY", null, null, null);
        final ValuationResponse response = new ValuationResponse.Builder()
                .statusCode(HttpStatusCode.OK.getStatusCode())
                .responseBodyFormatter(new ValuationResponseBodyJSONFormatter())
                .recordHolder(recordHolder)
                .errorMessage("testMsg")
                .build();
        assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
        assertEquals("testMsg", response.getErrorMessage());
        assertEquals("testMsg", response.getErrorMessage());
        assertEquals(expectedJsonString, response.getMessageBody());
    }
}
