package com.szilberhornz.valueinvdata.services.stockvaluation;

import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerCache;
import com.szilberhornz.valueinvdata.services.stockvaluation.cache.ValuationServerNoEvictionCache;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Disabled
class AppContainerTest {

    @Test
    void containerShouldInstantiateNoEvictionCacheByDefault(){
        AppContainer sut = new AppContainer();
        final ValuationServerCache result = sut.getCache();
        assertInstanceOf(ValuationServerNoEvictionCache.class, result);
    }

}
