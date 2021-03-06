/*
 * $Id: AsyncDelegateMessageProcessor.java 24927 2012-10-03 20:25:08Z svacas $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.MessageProcessorPathElement;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.processor.ProcessingStrategy.StageNameSource;
import org.mule.config.i18n.CoreMessages;
import org.mule.construct.Flow;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.util.NotificationUtils;
import org.mule.work.AbstractMuleEventWork;
import org.mule.work.MuleWorkManager;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to schedule asynchronous
 * processing of MessageProcessor delegate configured the next {@link MessageProcessor}. The next
 * {@link MessageProcessor} is therefore be executed in a different thread regardless of the exchange-pattern
 * configured on the inbound endpoint. If a transaction is present then an exception is thrown.
 */
public class AsyncDelegateMessageProcessor extends AbstractMessageProcessorOwner
        implements MessageProcessor, Initialisable, Startable, Stoppable
{

    protected Log logger = LogFactory.getLog(getClass());

    protected MessageProcessor delegate;

    protected List<MessageProcessor> processors;
    protected ProcessingStrategy processingStrategy;
    protected String name;

    private MessageProcessor target;

    public AsyncDelegateMessageProcessor(MessageProcessor delegate,
                                         ProcessingStrategy processingStrategy,
                                         String name)
    {
        this.delegate = delegate;
        this.processingStrategy = processingStrategy;
        this.name = name;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (delegate == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("delegate message processor"), this);
        }
        if (processingStrategy == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("processingStrategy"), this);
        }
        StageNameSource nameSource = null;
        if (name != null)
        {
            nameSource = ((Flow) flowConstruct).getAsyncStageNameSource(name);
        }
        else
        {
            nameSource = ((Flow) flowConstruct).getAsyncStageNameSource();
        }

        MessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(flowConstruct);
        processingStrategy.configureProcessors(Collections.singletonList(delegate), nameSource, builder,
                                               muleContext);
        try
        {
            target = builder.build();
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
        super.initialise();
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event.isTransacted())
        {
            throw new MessagingException(CoreMessages.asyncDoesNotSupportTransactions(), event, this);
        }

        if (target != null)
        {
            // Clone event and make it async
            MuleEvent newEvent = new DefaultMuleEvent(
                    (MuleMessage) ((ThreadSafeAccess) event.getMessage()).newThreadCopy(), event, false);
            target.process(newEvent);
        }
        if (muleContext.getConfiguration().isFlowEndingWithOneWayEndpointReturnsNull())
        {
            return event;
        }
        else
        {
            return VoidMuleEvent.getInstance();
        }
    }

    public void setDelegate(MessageProcessor delegate)
    {
        this.delegate = delegate;
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return Collections.singletonList(target);
    }

    public ProcessingStrategy getProcessingStrategy()
    {
        return processingStrategy;
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
    {
        if (delegate instanceof MessageProcessorContainer)
        {
            ((MessageProcessorContainer) delegate).addMessageProcessorPathElements(pathElement);
        }
        else
        {
            NotificationUtils.addMessageProcessorPathElements(Collections.singletonList(delegate), pathElement);
        }
    }

    class AsyncMessageProcessorWorker extends AbstractMuleEventWork
    {

        public AsyncMessageProcessorWorker(MuleEvent event)
        {
            super(event);
        }

        @Override
        protected void doRun()
        {
            try
            {
                delegate.process(event);
            }
            catch (MuleException e)
            {
                event.getFlowConstruct().getExceptionListener().handleException(e, event);
            }
        }
    }

}
