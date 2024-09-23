package com.szilberhornz.valueinvdata.services.stockvaluation;

/**
 * This class is a collection of static variables, some of which are coming from VMOptions.
 * I chose not to use any files to hold AppContext values because a) it's a small app,
 * b) even though it's not a concern, file changes need releases while command line changes
 * only need a bounce to take effect.
 */
public final class AppContext {

    private AppContext() {
        //hide implicit public constructor. This class will only contain constants so no need to instantiate
    }
}
