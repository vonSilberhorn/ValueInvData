package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.authr;

public interface FMPAuthorization {

    //use char[] because it cannot be inadvertently logged and even memory dump doesn't show human-readable format
    //throw checked exception to make sure the user gets http 401
    char[] retrieveApiKey() throws NoApiKeyFoundException;
}
