package com.jexunit.core.data;

import com.jexunit.core.JExUnitConfig;
import com.jexunit.core.data.entity.TestModelBase;
import com.jexunit.core.data.entity.TestModelSub;
import org.junit.After;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Tests for restricting the (OGNL) expressions of the column headers to property access. The "malicious" expressions
 * only set a system property to check whether they were executed.
 */
public class OgnlUtilsTest {

    private static final String PROPERTY_ACCESS_ONLY_KEY = JExUnitConfig.ConfigKey.OGNL_PROPERTY_ACCESS_ONLY.getKey();

    @After
    public void resetConfig() {
        JExUnitConfig.setConfigProperty(PROPERTY_ACCESS_ONLY_KEY,
                JExUnitConfig.ConfigKey.OGNL_PROPERTY_ACCESS_ONLY.getDefaultConfig());
    }

    @Test
    public void shouldRejectStaticMethodCallInHeader() {
        assertThrows(Exception.class, () -> OgnlUtils.setPropertyToObject(new HashMap<String, String>(),
                "@java.lang.System@setProperty('jexunit.probe.header', 'executed').x", "value"));

        assertNull(System.getProperty("jexunit.probe.header"));
    }

    @Test
    public void shouldRejectStaticMethodCallInCondition() {
        final TestModelBase tmb = new TestModelBase();
        tmb.getSubEntityListAttr().add(new TestModelSub());

        assertThrows(Exception.class, () -> OgnlUtils.setPropertyToObject(tmb,
                "subEntityListAttr[@java.lang.System@setProperty(\"jexunit.probe.condition\", \"executed\")=x]"
                        + ".intAttr", "1"));

        assertNull(System.getProperty("jexunit.probe.condition"));
    }

    @Test
    public void shouldRejectMethodCallInGetProperty() {
        final TestModelBase tmb = new TestModelBase();
        tmb.setStringAttr("value");

        assertThrows(Exception.class, () -> OgnlUtils.getProperty(tmb, "stringAttr.toUpperCase()"));
    }

    @Test
    public void shouldAllowAnyExpressionIfConfigured() throws Exception {
        JExUnitConfig.setConfigProperty(PROPERTY_ACCESS_ONLY_KEY, "false");
        final TestModelBase tmb = new TestModelBase();
        tmb.setStringAttr("value");

        assertEquals("VALUE", OgnlUtils.getProperty(tmb, "stringAttr.toUpperCase()"));
    }

    @Test
    public void shouldAllowPropertyAccess() throws Exception {
        final TestModelBase tmb = new TestModelBase();

        OgnlUtils.setPropertyToObject(tmb, "subEntityAttr.stringAttr", "nested");
        OgnlUtils.setPropertyToObject(tmb, "subEntityListAttr[1].intAttr", "5");
        OgnlUtils.setPropertyToObject(tmb, "subEntityListAttr[intAttr=5].stringAttr", "by condition");
        OgnlUtils.setPropertyToObject(tmb, "mapAttr.myKey", "mapped");

        assertEquals("nested", tmb.getSubEntityAttr().getStringAttr());
        assertEquals("by condition", tmb.getSubEntityListAttr().get(1).getStringAttr());
        assertEquals("mapped", tmb.getMapAttr().get("myKey"));
        assertEquals(2, OgnlUtils.getProperty(tmb, "subEntityListAttr.size"));
    }

    @Test
    public void shouldAccessPublicPropertiesOfNonPublicClass() throws Exception {
        final PackagePrivateEntity entity = new PackagePrivateEntity();

        OgnlUtils.setPropertyToObject(entity, "name", "John");

        assertEquals("John", entity.getName());
        assertEquals("John", OgnlUtils.getProperty(entity, "name"));
    }

    @Test
    public void shouldRejectNonPublicMembers() {
        final PackagePrivateEntity entity = new PackagePrivateEntity();

        // a private field without accessors and a protected setter
        assertThrows(Exception.class, () -> OgnlUtils.setPropertyToObject(entity, "secret", "x"));
        assertThrows(Exception.class, () -> OgnlUtils.setPropertyToObject(entity, "hidden", "x"));

        assertNull(entity.secret);
        assertNull(entity.hidden);
    }

    static class PackagePrivateEntity {

        private String name;
        private String secret;
        private String hidden;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        protected void setHidden(final String hidden) {
            this.hidden = hidden;
        }
    }

}
