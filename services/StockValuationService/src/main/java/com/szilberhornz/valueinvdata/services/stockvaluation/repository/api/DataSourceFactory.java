package com.szilberhornz.valueinvdata.services.stockvaluation.repository.api;

import javax.sql.DataSource;

public interface DataSourceFactory {

    DataSource getValuationDbDataSource();
}
