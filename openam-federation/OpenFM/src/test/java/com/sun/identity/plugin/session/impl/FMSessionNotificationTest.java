/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2023 ForgeRock AS.
 */

package com.sun.identity.plugin.session.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.sun.identity.plugin.session.SessionListener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@RunWith(MockitoJUnitRunner.class)
public class FMSessionNotificationTest {

    @Mock
    private ScheduledExecutorService scheduledService;

    @Mock
    private SessionService sessionService;

    @Mock
    private SSOToken token;

    @Mock
    private SSOTokenID tokenID;

    @Mock
    private SessionListener listener;

    @InjectMocks
    private FMSessionNotification sessionNotification;

    private ListAppender<ILoggingEvent> appender;


    @Before
    public void setup() {

        appender = new ListAppender<>();
        appender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(FMSessionNotification.class);
        logger.addAppender(appender);
    }

    @Test
    public void testDoNotRemoveSessionIndexIsSet() throws Exception {
        //given
        given(token.getTokenID()).willReturn(tokenID);
        given(tokenID.toString()).willReturn("sessionIndex");
        sessionNotification.store(token, listener);

        //when
        sessionNotification.setDoNotRemoveSessionIndex("sessionIndex", "true");

        //then
        //no other way to assert so assert the code flow vie debug message
        assertLoggerMessage("FMSessionNotification.setDoNotRemoveSessionIndex:" +
                " Updated session sessionIndex, doNotRemoveSAML2IDPSession to true");
    }

    @Test
    public void testDoNotRemoveSessionIndexFailedToSet() throws Exception {
        //given
        //when
        sessionNotification.setDoNotRemoveSessionIndex("sessionIndex", "true");

        //then
        //no other way to assert so assert the code flow vie debug message
        assertLoggerMessage("FMSessionNotification.setDoNotRemoveSessionIndex: Failed to update store for sessionId");
    }

    private void assertLoggerMessage(String actualMessage) {
        assertThat(1, is(appender.list.size()));
        ILoggingEvent event = appender.list.get(0);
        assertThat(Level.DEBUG, is(event.getLevel()));
        assertThat(actualMessage, is(event.getFormattedMessage()));
    }

}