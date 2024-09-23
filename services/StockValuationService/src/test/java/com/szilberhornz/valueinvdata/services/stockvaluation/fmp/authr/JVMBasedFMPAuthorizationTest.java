package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JVMBasedFMPAuthorizationTest {

    final FMPAuthorization sut = new JVMBasedFMPAuthorization();

    @Test
    @ClearSystemProperty(key = "FMP_API_KEY")
    void retrieveApiKeyShouldThrowException() {
        final Exception exception = assertThrows(NoApiKeyFoundException.class, sut::retrieveApiKey);
        assertEquals("No api key was set for the Financial Modeling Prep api!\n" +
                "Please set it via the -DFMP_API_KEY VM option or if you don't yet have one, get one first at https://site.financialmodelingprep.com/developer/docs", exception.getMessage());
    }

    @Test
    @SetSystemProperty(key = "FMP_API_KEY", value = "abcdef")
    void retrieveApiKeyShouldSucceed() throws NoApiKeyFoundException {
        final char[] result = sut.retrieveApiKey();
        assertEquals("abcdef", new String(result));
    }
}