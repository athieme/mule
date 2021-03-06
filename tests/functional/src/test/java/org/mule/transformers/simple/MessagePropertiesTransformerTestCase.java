/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.simple.MessagePropertiesTransformer;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MessagePropertiesTransformerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "message-properties-transformer-config.xml";
    }

    @Test
    public void testOverwriteFlagEnabledByDefault() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map<String, Object> add = new HashMap<String, Object>();
        add.put("addedProperty", "overwrittenValue");
        t.setAddProperties(add);
        t.setMuleContext(muleContext);

        MuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setOutboundProperty("addedProperty", "originalValue");
        MuleEventContext ctx = getTestEventContext(msg);
        // context clones message
        msg = ctx.getMessage();
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, (String)null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        compareProperties(msg, transformed);

        assertEquals("overwrittenValue", transformed.getOutboundProperty("addedProperty"));
    }

    @Test
    public void testOverwriteFalsePreservesOriginal() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map<String, Object> add = new HashMap<String, Object>();
        add.put("addedProperty", "overwrittenValue");
        t.setAddProperties(add);
        t.setOverwrite(false);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setProperty("addedProperty", "originalValue", PropertyScope.INVOCATION);
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, (String)null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        compareProperties(msg, transformed);

        assertEquals("originalValue", transformed.getInvocationProperty("addedProperty"));
    }

    @Test
    public void testExpressionsInAddProperties() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map<String, Object> add = new HashMap<String, Object>();
        add.put("Foo", "#[header:public-house]");
        t.setAddProperties(add);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setOutboundProperty("public-house", "Bar");
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, (String)null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        compareProperties(msg, transformed);

        assertEquals("Bar", transformed.getOutboundProperty("Foo"));
    }

    @Test
    public void testRenameProperties() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map<String, String> add = new HashMap<String, String>();
        add.put("Foo", "Baz");
        t.setRenameProperties(add);
        t.setScope(PropertyScope.INVOCATION);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setInvocationProperty("Foo", "Bar");
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        compareProperties(msg, transformed);

        assertEquals("Bar", transformed.getInvocationProperty("Baz"));
    }

    @Test
    public void testDelete() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        t.setDeleteProperties("badProperty");
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setOutboundProperty("badProperty", "badValue");
        assertEquals("badValue", msg.<Object>getOutboundProperty("badProperty"));
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, (String)null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        compareProperties(msg, transformed);

        assertFalse(transformed.getInvocationPropertyNames().contains("badValue"));
        assertFalse(transformed.getInboundPropertyNames().contains("badValue"));
        assertFalse(transformed.getOutboundPropertyNames().contains("badValue"));
        assertFalse(transformed.getSessionPropertyNames().contains("badValue"));
    }

    @Test
    public void testTransformerConfig() throws Exception
    {
        MessagePropertiesTransformer transformer = (MessagePropertiesTransformer) muleContext.getRegistry().lookupTransformer("testTransformer");
        transformer.setMuleContext(muleContext);
        assertNotNull(transformer);
        assertNotNull(transformer.getAddProperties());
        assertNotNull(transformer.getDeleteProperties());
        assertEquals(2, transformer.getAddProperties().size());
        assertEquals(2, transformer.getDeleteProperties().size());
        assertEquals(1, transformer.getRenameProperties().size());
        assertTrue(transformer.isOverwrite());
        assertEquals("text/baz;charset=UTF-16BE", transformer.getAddProperties().get("Content-Type"));
        assertEquals("value", transformer.getAddProperties().get("key"));
        assertEquals("test-property1", transformer.getDeleteProperties().get(0));
        assertEquals("test-property2", transformer.getDeleteProperties().get(1));
        assertEquals("Faz", transformer.getRenameProperties().get("Foo"));
        assertEquals(PropertyScope.OUTBOUND, transformer.getScope());
    }

    @Test
    public void testDeleteUsingPropertyName() throws Exception
    {
        final String expression = "badProperty";
        final String[] validProperties = new String[] {"somethingnotsobad"};
        final String[] invalidProperties = new String[] {"badProperty"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    @Test
    public void testDeletePropertiesStartingWithExpression() throws Exception
    {
        final String expression = "^bad.*";
        final String[] validProperties = new String[] {"somethingnotsobad"};
        final String[] invalidProperties = new String[] {"badProperty", "badThing"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    @Test
    public void testDeletePropertiesCaseInsensitiveRegex() throws Exception
    {
        final String expression = "(?i)^BAD.*";
        final String[] validProperties = new String[] {"somethingnotsobad"};
        final String[] invalidProperties = new String[] {"badProperty", "badThing"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    @Test
    public void testDeletePropertiesEndingWithExpression() throws Exception
    {
        final String expression = ".*bad$";
        final String[] validProperties = new String[] {"badProperty", "badThing"};
        final String[] invalidProperties = new String[] {"somethingnotsobad"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    @Test
    public void testDeletePropertiesContainingExpression() throws Exception
    {
        final String expression = ".*bad.*";
        final String[] validProperties = new String[] {};
        final String[] invalidProperties = new String[] {"badProperty", "badThing", "somethingnotsobad"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    @Test
    public void testDeletePropertiesUsingWildcard() throws Exception
    {
        final String expression = "bad*";
        final String[] validProperties = new String[] {"somethingnotsobad"};
        final String[] invalidProperties = new String[] {"badProperty", "badThing"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    private void doTestMessageTransformationWithExpression(String expression, String[] validProperties, String[] invalidProperties)
            throws TransformerException
    {
        MessagePropertiesTransformer t = createTransformerWithExpression(expression);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        addPropertiesToMessage(validProperties, msg);
        addPropertiesToMessage(invalidProperties, msg);

        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        assertMessageContainsExpectedProperties(validProperties, invalidProperties, transformed);
    }

    private void assertMessageContainsExpectedProperties(String[] validProperties, String[] invalidProperties, DefaultMuleMessage transformed)
    {
        for (String property : validProperties)
        {
            assertTrue("Should contain property: " + property, transformed.getOutboundPropertyNames().contains(property));
        }

        for (String property : invalidProperties)
        {
            assertFalse("Should not contain property: " + property, transformed.getOutboundPropertyNames().contains(property));
        }
    }

    private MessagePropertiesTransformer createTransformerWithExpression(String expression)
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        t.setDeleteProperties(expression);
        t.setMuleContext(muleContext);
        return t;
    }

    private void addPropertiesToMessage(String[] validProperties, DefaultMuleMessage msg)
    {
        for (String property : validProperties)
        {
            msg.setOutboundProperty(property, "defaultPropertyValue");
        }
    }

    private void compareProperties(MuleMessage msg, MuleMessage transformed)
    {
        assertEquals(msg.getInvocationPropertyNames(), transformed.getInvocationPropertyNames());
        assertEquals(msg.getInboundPropertyNames(), transformed.getInboundPropertyNames());
        assertEquals(msg.getOutboundPropertyNames(), transformed.getOutboundPropertyNames());
        assertEquals(msg.getSessionPropertyNames(), transformed.getSessionPropertyNames());
    }

}
