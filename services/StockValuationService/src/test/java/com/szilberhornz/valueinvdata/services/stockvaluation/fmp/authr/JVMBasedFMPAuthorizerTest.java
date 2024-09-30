package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr;

import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.authr.FMPAuthorizer;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.authr.JVMBasedFMPAuthorizer;
import com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.fmp.authr.NoApiKeyFoundException;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JVMBasedFMPAuthorizerTest {

    final FMPAuthorizer sut = new JVMBasedFMPAuthorizer();

    @Test
    @ClearSystemProperty(key = "FMP_API_KEY")
    void retrieveApiKeyShouldThrowException() {
        final Exception exception = assertThrows(NoApiKeyFoundException.class, this.sut::retrieveApiKey);
        assertEquals("No api key was set for the Financial Modeling Prep api!\n" +
                "Please set it via the -DFMP_API_KEY VM option or if you don't yet have one, get one first at https://site.financialmodelingprep.com/developer/docs", exception.getMessage());
    }

    @Test
    @SetSystemProperty(key = "FMP_API_KEY", value = "abcdef")
    void retrieveApiKeyShouldSucceed() throws NoApiKeyFoundException {
        final char[] result = this.sut.retrieveApiKey();
        assertEquals("abcdef", new String(result));
    }
}