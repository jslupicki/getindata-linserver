package com.slupicki.linserver;

public class TreeSearchServiceTest extends AbstractSearchServiceTest {
    @Override
    SearchService getServiceUnderTest() {
        return new TreeSearchServiceImpl();
    }
}
