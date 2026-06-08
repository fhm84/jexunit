package com.jexunit.core.data;

import com.jexunit.core.data.entity.TestModelBase;
import com.jexunit.core.data.entity.TestModelSub;
import ognl.OgnlException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CollectionPropertyHelperTest {

    @Test
    public void shouldGetConditionalProperty() throws OgnlException {
        final TestModelBase tmb = new TestModelBase();
        tmb.setStringAttr("my testentity");

        final TestModelSub tms1 = new TestModelSub();
        tms1.setIntAttr(1);
        tms1.setStringAttr("egon");
        tmb.getSubEntityListAttr().add(tms1);

        final TestModelSub tms2 = new TestModelSub();
        tms2.setIntAttr(7);
        tms2.setStringAttr("huber");
        tmb.getSubEntityListAttr().add(tms2);

        final TestModelSub tms3 = new TestModelSub();
        tms3.setIntAttr(5);
        tms3.setStringAttr("meier");
        tmb.getSubEntityListAttr().add(tms3);

        final String propertyCondition = "subEntityListAttr[stringAttr=meier].intAttr";

        OgnlUtils.setPropertyToObject(tmb, propertyCondition, "12");

        assertThat(tmb.getSubEntityListAttr().get(0).getIntAttr(), equalTo(1));
        assertThat(tmb.getSubEntityListAttr().get(1).getIntAttr(), equalTo(7));
        assertThat(tmb.getSubEntityListAttr().get(2).getIntAttr(), equalTo(12));
    }
}
