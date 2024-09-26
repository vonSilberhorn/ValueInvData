package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport.formatter;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.RecordHolder;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.DiscountedCashFlowDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetConsensusDTO;
import com.szilberhornz.valueinvdata.services.stockvaluation.core.record.PriceTargetSummaryDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValuationReportBodyExplainerFormatterTest {

    private final DiscountedCashFlowDTO dcfDto = new DiscountedCashFlowDTO("DUMMY", "2024-09-26", 15.5, 14);
    private final PriceTargetConsensusDTO ptcDto = new PriceTargetConsensusDTO("DUMMY", 20, 10, 16, 15);
    private final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("DUMMY", 2, 16, 5, 14);

    @Test
    void testFullScript(){
        final String expected = "On 2024-09-26 the discounted cash flow valuation model for the ticker DUMMY shows that the fair valuation per share is 15.50, while the current price per share is 14.00.\n" +
                "This means that the company seems to be undervalued on the stock market and may be considered a candidate to buy or hold\n" +
                "It is advised to look for other valuation methods too, especially if the spread between the valuation price and the actual stock price is large.\n" +
                "Find out more about the discounted cash flow valuation here: https://www.investopedia.com/terms/d/dcf.asp\n" +
                "\n" +
                "Last month 2 stock analysts made price target prediction about this stock, with and average price target of 16.00. \n" +
                "Last quarter 5 analysts made predictions with 14.00 average price target!\n" +
                "This suggests that analyst believe the stock price is very close to its fair value so holding or selling might be better options than buying. \n" +
                "Overall, the highest projection from any analyst was 20.00, while the lowest was 10.00, with the consensus being around 16.00 and the median prediction at 15.00 \n" +
                "Price target predictions are the stock analysts own overall calculations for a price point where they think a stock would be fairly valued. \n" +
                "This is based on a number of factors, you can find out more about those at https://www.investopedia.com/investing/target-prices-and-sound-investing/\n" +
                "\n" +
                "\n" +
                "Disclaimer: this is solely for educational purposes and does not constitute as financial or investment advice!\n";
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, this.ptcDto, this.ptsDto);
        final ValuationResponseBodyExplainerFormatter sut = new ValuationResponseBodyExplainerFormatter();
        assertEquals(expected, sut.getFormattedResponseBody(recordHolder, null));
    }

    @Test
    void testFullScriptWithBuyRecommendation(){
        final String expected = "On 2024-09-26 the discounted cash flow valuation model for the ticker DUMMY shows that the fair valuation per share is 25.00, while the current price per share is 14.00.\n" +
                "This means that the company seems to be undervalued on the stock market and may be considered a candidate to buy or hold\n" +
                "It is advised to look for other valuation methods too, especially if the spread between the valuation price and the actual stock price is large.\n" +
                "Find out more about the discounted cash flow valuation here: https://www.investopedia.com/terms/d/dcf.asp\n" +
                "\n" +
                "Last month 2 stock analysts made price target prediction about this stock, with and average price target of 25.00. \n" +
                "Last quarter 5 analysts made predictions with 30.00 average price target!\n" +
                "This suggests that analyst believe the stock price has a potential to climb in the future, which makes it a candidate to buy and hold. \n" +
                "Overall, the highest projection from any analyst was 20.00, while the lowest was 10.00, with the consensus being around 16.00 and the median prediction at 15.00 \n" +
                "Price target predictions are the stock analysts own overall calculations for a price point where they think a stock would be fairly valued. \n" +
                "This is based on a number of factors, you can find out more about those at https://www.investopedia.com/investing/target-prices-and-sound-investing/\n" +
                "\n" +
                "\n" +
                "Disclaimer: this is solely for educational purposes and does not constitute as financial or investment advice!\n";
        final DiscountedCashFlowDTO dcfDto = new DiscountedCashFlowDTO("DUMMY", "2024-09-26", 25, 14);
        final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("DUMMY", 2, 25, 5, 30);
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY", dcfDto, this.ptcDto, ptsDto);
        final ValuationResponseBodyExplainerFormatter sut = new ValuationResponseBodyExplainerFormatter();
        assertEquals(expected, sut.getFormattedResponseBody(recordHolder, null));
    }

    @Test
    void testFullScriptWithSellRecommendation(){
        final String expected = "On 2024-09-26 the discounted cash flow valuation model for the ticker DUMMY shows that the fair valuation per share is 5.00, while the current price per share is 14.00.\n" +
                "This means that the company seems to be overvalued on the stock market and may be considered a candidate to sell.\n" +
                "It is advised to look for other valuation methods too, especially if the spread between the valuation price and the actual stock price is large.\n" +
                "Find out more about the discounted cash flow valuation here: https://www.investopedia.com/terms/d/dcf.asp\n" +
                "\n" +
                "Last month 2 stock analysts made price target prediction about this stock, with and average price target of 5.00. \n" +
                "Last quarter 5 analysts made predictions with 7.00 average price target!\n" +
                "This suggests that analyst believe the stock price may be overvalued and not a good candidate for buying. \n" +
                "Overall, the highest projection from any analyst was 20.00, while the lowest was 10.00, with the consensus being around 16.00 and the median prediction at 15.00 \n" +
                "Price target predictions are the stock analysts own overall calculations for a price point where they think a stock would be fairly valued. \n" +
                "This is based on a number of factors, you can find out more about those at https://www.investopedia.com/investing/target-prices-and-sound-investing/\n" +
                "\n" +
                "\n" +
                "Disclaimer: this is solely for educational purposes and does not constitute as financial or investment advice!\n";
        final DiscountedCashFlowDTO dcfDto = new DiscountedCashFlowDTO("DUMMY", "2024-09-26", 5, 14);
        final PriceTargetSummaryDTO ptsDto = new PriceTargetSummaryDTO("DUMMY", 2, 5, 5, 7);
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY", dcfDto, this.ptcDto, ptsDto);
        final ValuationResponseBodyExplainerFormatter sut = new ValuationResponseBodyExplainerFormatter();
        assertEquals(expected, sut.getFormattedResponseBody(recordHolder, null));
    }

    @Test
    void testEmpty(){
        final ValuationResponseBodyExplainerFormatter sut = new ValuationResponseBodyExplainerFormatter();
        assertEquals("Could not find any data!", sut.getFormattedResponseBody(null, null));
    }


    @Test
    void testEmptyWithErrorMsg(){
        final ValuationResponseBodyExplainerFormatter sut = new ValuationResponseBodyExplainerFormatter();
        assertEquals("Error!", sut.getFormattedResponseBody(null, "Error!"));
    }

    @Test
    void testPartialWithErrorMsg(){
        final String expected = "On 2024-09-26 the discounted cash flow valuation model for the ticker DUMMY shows that the fair valuation per share is 15.50, while the current price per share is 14.00.\n" +
                "This means that the company seems to be undervalued on the stock market and may be considered a candidate to buy or hold\n" +
                "It is advised to look for other valuation methods too, especially if the spread between the valuation price and the actual stock price is large.\n" +
                "Find out more about the discounted cash flow valuation here: https://www.investopedia.com/terms/d/dcf.asp\n" +
                "\n" +
                "\n" +
                "Disclaimer: this is solely for educational purposes and does not constitute as financial or investment advice!\n" +
                "Encountered the following issue while retrieving the data: Error!!!";
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY", this.dcfDto, null, null);
        final ValuationResponseBodyExplainerFormatter sut = new ValuationResponseBodyExplainerFormatter();
        assertEquals(expected, sut.getFormattedResponseBody(recordHolder, "Error!!!"));
    }

    @Test
    void testPartialWithoutDcf(){
        final String expected = "Last month 2 stock analysts made price target prediction about this stock, with and average price target of 16.00. \n" +
                "Last quarter 5 analysts made predictions with 14.00 average price target!\n" +
                " \n" +
                "Overall, the highest projection from any analyst was 20.00, while the lowest was 10.00, with the consensus being around 16.00 and the median prediction at 15.00 \n" +
                "Price target predictions are the stock analysts own overall calculations for a price point where they think a stock would be fairly valued. \n" +
                "This is based on a number of factors, you can find out more about those at https://www.investopedia.com/investing/target-prices-and-sound-investing/\n" +
                "\n" +
                "\n" +
                "Disclaimer: this is solely for educational purposes and does not constitute as financial or investment advice!\n";
        final RecordHolder recordHolder = RecordHolder.newRecordHolder("DUMMY", null, this.ptcDto, this.ptsDto);
        final ValuationResponseBodyExplainerFormatter sut = new ValuationResponseBodyExplainerFormatter();
        assertEquals(expected, sut.getFormattedResponseBody(recordHolder, ""));
    }
}
