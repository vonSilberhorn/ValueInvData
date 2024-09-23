package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr;

/**
 * This implementation uses VM Options to retrieve the necessary api key for FMP.
 */
public class JVMBasedFMPAuthorization implements FMPAuthorization {

    private static final String EXCEPTION_MSG = "No api key was set for the Financial Modeling Prep api!\n" +
            "Please set it via the -DFMP_API_KEY VM option or if you don't yet have one, get one first at https://site.financialmodelingprep.com/developer/docs";

    @Override
    public char[] retrieveApiKey() throws NoApiKeyFoundException {
        final String apiKeyString = System.getProperty("FMP_API_KEY");
        if (apiKeyString == null) {
            throw new NoApiKeyFoundException(EXCEPTION_MSG);
        } else {
            return apiKeyString.toCharArray();
        }
    }
}
