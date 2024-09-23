package com.szilberhornz.valueinvdata.services.stockvaluation.persistence;

import javax.sql.DataSource;

/**
 * Separate persistence layer with an interface as usual
 */
//todo instantiate it in the appcontainer based on the
public interface DataSourceFactory {

    /**
     * @return A DataSource pointing to the ValuationDB sql server, wherever that may be
     */
    DataSource getValuationDBDataSource();
}
