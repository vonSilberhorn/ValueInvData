package com.szilberhornz.valueinvdata.services.stockvaluation.fmp.record;

public record DiscountedCashFlowDTO(String symbol, String dateString, double dcf, double stockPrice) {

    private String valuationMeaning() {
        StringBuilder verboseValuation = new StringBuilder();
        if (this.dcf() > this.stockPrice()) {
            verboseValuation.append("\nThis means that the company seems to be undervalued on the stock market and may be " +
                    "considered a candidate to buy or hold");
        } else if (this.dcf() < this.stockPrice()) {
            verboseValuation.append("\nThis means that the company seems to be overvalued on the stock market and may be " +
                    "considered a candidate to sell.");
        } else {
            verboseValuation.append("\nThis means that the company seems to be fairly valued on the stock market and is " +
                    "neither a good candidate to sell or buy");
        }
        verboseValuation.append("\nIt is advised to look for other valuation methods too, especially if the spread between the valuation price and the actual stock price is large.");
        verboseValuation.append("\n\nDisclaimer: this is solely for educational purposes and does not constitute as financial or investment advice!");
        return verboseValuation.toString();
    }

    @Override
    public String toString() {
        return "On " + this.dateString() + " the discounted cash flow valuation model for " + this.symbol() +
                " shows that the fair valuation per share is " + String.format("%.2f", this.dcf()) + ", while the current price per share is " +
                this.stockPrice() + this.valuationMeaning() +
                "\nFind out more about the discounted cash flow valuation here: https://www.investopedia.com/terms/d/dcf.asp";
    }
}
