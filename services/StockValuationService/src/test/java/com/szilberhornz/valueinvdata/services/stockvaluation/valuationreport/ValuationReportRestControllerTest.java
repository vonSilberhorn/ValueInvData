package com.szilberhornz.valueinvdata.services.stockvaluation.valuationreport;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ValuationReportRestControllerTest {

    @Test
    void testGetCall() {
        final VRSagaOrchestrator orchestratorMock = Mockito.mock(VRSagaOrchestrator.class);
        final ValuationReportRestController controller = new ValuationReportRestController(orchestratorMock);
        controller.getValuationReport("TICKER");
        Mockito.verify(orchestratorMock, Mockito.times(1)).getValuationResponse("TICKER");
    }
}
