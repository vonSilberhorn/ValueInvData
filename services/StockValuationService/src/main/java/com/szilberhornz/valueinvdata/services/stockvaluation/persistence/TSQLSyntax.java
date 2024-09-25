package com.szilberhornz.valueinvdata.services.stockvaluation.persistence;

//we have few queries so I don't bother reading and writing files, just store the queries here
//MSSQL uses Transact SQL syntax, and the H2 db should be setup to use that too
public final class TSQLSyntax {

    static final String INSERT_INTO_DCF = "INSERT INTO DiscountedCashFlowDb VALUES (?, ?, ?, ?)";
    static final String INSERT_INTO_PTC = "INSERT INTO PriceTargetConsensusDb VALUES (?, ?, ?, ?, ?)";
    static final String INSERT_INTO_PTS = "INSERT INTO PriceTargetSummaryDb VALUES (?, ?, ?, ?, ?)";

    static final String SELECT_FROM_DCF = "SELECT * FROM DiscountedCashFlowDb WHERE DiscountedCashFlowDb.ticker= ?";
    static final String SELECT_FROM_PTC = "SELECT * FROM PriceTargetConsensusDb WHERE PriceTargetConsensusDb.ticker= ?";
    static final String SELECT_FROM_PTS = "SELECT * FROM PriceTargetSummaryDb WHERE PriceTargetSummaryDb.ticker= ?";

    static final String QUERY_ALL_DATA_FOR_TICKER = "SELECT * FROM DiscountedCashFlowDb " +
            "LEFT OUTER JOIN PriceTargetSummaryDb ON PriceTargetSummaryDb.Ticker = DiscountedCashFlowDb.Ticker " +
            "LEFT OUTER JOIN PriceTargetConsensusDb ON PriceTargetConsensusDb.Ticker = DiscountedCashFlowDb.Ticker " +
            "WHERE DiscountedCashFlowDb.ticker= ?";

    private TSQLSyntax(){
        //no need to instantiate;
    }
}
