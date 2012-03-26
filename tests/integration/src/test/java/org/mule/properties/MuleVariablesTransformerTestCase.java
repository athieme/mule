/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import org.junit.Test;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.tck.functional.FlowAssert;
import org.mule.tck.junit4.FunctionalTestCase;

public class MuleVariablesTransformerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/properties/mule-variables-transformer-test-case.xml";
    }

    @Test
    public void testAddVariable() throws Exception
    {
        runScenario("addVariable");
    }

    @Test
    public void testAddVariableWithExpressionKey() throws Exception
    {
        runScenario("addVariableUsingExpressionKey");
    }

    @Test
    public void testRemoveVariable() throws Exception
    {
        runScenario("removeVariable");
    }

    @Test
    public void testRemoveVariableUsingExpression() throws Exception
    {
        runScenario("removeVariableUsingExpression");
    }

    @Test
    public void testRemoveVariableUsingRegex() throws Exception
    {
        runScenario("removeVariableUsingRegex");
    }

    @Test
    public void testRemoveAllVariables() throws Exception
    {
        runScenario("removeAllVariables");
    }

    public void runScenario(String flowName) throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("data", muleContext);
        DefaultMuleEvent event = new DefaultMuleEvent(message, getTestInboundEndpoint(""), getTestService());
        Flow flow = (Flow) getFlowConstruct(flowName);
        flow.process(event);
        FlowAssert.verify(flowName);
    }

}
