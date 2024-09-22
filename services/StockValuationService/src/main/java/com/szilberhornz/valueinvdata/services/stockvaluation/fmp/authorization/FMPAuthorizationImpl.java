package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authorization;

/**
 * This implementation retrieves the api key from VM Options (set as -DFMP_API_KEY=...)
 * In a normal production environment, this API key would be stored in a secret store like Hashicorp Vault.
 */
public class FMPAuthorizationImpl implements FMPAuthorization {

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
