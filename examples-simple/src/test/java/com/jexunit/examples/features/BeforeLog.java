package com.jexunit.examples.features;

import com.jexunit.core.spi.BeforeSheet;

public class BeforeLog implements BeforeSheet {

    @Override
    public void run() {
        System.out.println("before");
    }
}
