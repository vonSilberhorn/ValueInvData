package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport;

/**
 * This would be the class to wire other http methods too, but for now we only have GET
 */
public class ValuationReportRestController {

    private final VRSagaOrchestrator orchestrator;

    public ValuationReportRestController(final VRSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    //mapping: GET: /valuation-report?ticker=TICKER
    public ValuationReport getValuationReport(final String ticker){
        return this.orchestrator.getValuationResponse(ticker);
    }
}
