package com.szilberhornz.valueinvdata.services.stockvaluation.core.fmp.dto;

//immutable because it has no reason to change
public class DiscountedCashFlowDTO {

    private final String symbol;
    private final String dateString;
    private final double dcf;
    private final double stockPrice;

    public DiscountedCashFlowDTO(String symbol, String dateString, double dcf, double stockPrice) {
        this.symbol = symbol;
        this.dateString = dateString;
        this.dcf = dcf;
        this.stockPrice = stockPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDateString() {
        return dateString;
    }

    public double getDcf() {
        return dcf;
    }

    public double getStockPrice() {
        return stockPrice;
    }

    @Override
    public String toString() {
        return "[\n" +
                "\t{\n" +
                "\t\t\"symbol\":" + this.getSymbol() + ",\n" +
                "\t\t\"date\":" + this.getDateString() + ",\n" +
                "\t\t\"dcf\":" + this.getDcf() + ",\n" +
                "\t\t\"Stock Price\":" + this.getStockPrice() + "\n" +
                "\t}\n" +
                "]";
    }
}
