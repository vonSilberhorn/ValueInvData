package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authorization;

/**
 * Interface to retrieve the api key for the Financial Modeling Prep website
 */
public interface FMPAuthorization {

    //LOGGER doesn't accept char[] so it cannot be inadvertently logged
    char[] retrieveApiKey() throws NoApiKeyFoundException;
}
