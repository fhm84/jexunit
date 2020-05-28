package com.jexunit.examples.features;

import com.jexunit.core.spi.AfterSheet;

public class AfterLog implements AfterSheet {

    @Override
    public void run() {
        System.out.println("after");
    }
}
