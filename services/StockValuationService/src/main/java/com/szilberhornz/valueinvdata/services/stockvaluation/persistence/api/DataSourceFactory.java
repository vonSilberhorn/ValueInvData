package com.szilberhornz.valueinvdata.services.stockvaluation.persistence.api;

import javax.sql.DataSource;

public interface DataSourceFactory {

    DataSource getValuationDbDataSource();
}
