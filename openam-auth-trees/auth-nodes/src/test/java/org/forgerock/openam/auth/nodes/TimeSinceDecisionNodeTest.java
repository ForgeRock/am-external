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
 * Copyright 2019-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.util.time.Duration.duration;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.ZoneOffset;
import java.util.List;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.utils.Time;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.forgerock.util.time.Duration;

import org.mockito.Mock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TimeSinceDecisionNodeTest {

    @Mock
    private TimeSinceDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    private TimeSinceDecisionNode timeSinceDecisionNode;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        TimeTravelUtil.useFixedClock(1_000_000);

        when(config.elapsedTime()).thenReturn("1 minute");
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();

        timeSinceDecisionNode = new TimeSinceDecisionNode(realm, config, idmIntegrationService);
    }

    @AfterMethod
    public void tearDown() {
        TimeTravelUtil.resetClock();
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfFailedToRetrieveObject() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenThrow(newResourceException(BAD_REQUEST));

        timeSinceDecisionNode.process(getContext(emptyList(), sharedState));
    }

    @Test
    public void shouldReturnFalseIfTimeSinceAttributeNotInObject() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        // invalid attribute
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        when(idmIntegrationService.retrieveCreateDate(any(), any(), any(), any())).thenReturn(json(object()));

        String outcome = timeSinceDecisionNode.process(getContext(emptyList(), sharedState)).outcome;
        assertThat(outcome).isEqualTo("false");
    }

    @DataProvider
    public Object[][] timeSinceData() {
        TimeTravelUtil.useFixedClock(1_000_000);
        return new Object[][] {
                // 1 day from now, should return false
                {duration(1, DAYS), false, nowAsString()},
                // 1 hour from now, should return false
                {duration(1, HOURS), false, nowAsString()},
                // 1 minute from now, should return false
                {duration(1, MINUTES), false, nowAsString()},
                // 1 second from now, should return false
                {duration(1, SECONDS), false, nowAsString()},
                // 1 day, 1 hour, 1 minute, 1 second from now, should return false
                {duration("1 day and 1 hour and 1 minute and 1 second"), false, nowAsString()},
                // zero time from now, should return true
                {Duration.ZERO, true, nowAsString()},
                // zero minutes from a minute before, should return true
                {Duration.ZERO, true, Time.zonedDateTime(ZoneOffset.UTC).minusMinutes(1).toString()},
                // 1 hour from an hour ago, should return true
                {duration(1, HOURS), true, Time.zonedDateTime(ZoneOffset.UTC).minusHours(1).toString()},
                // 1 day from a day ago, should return true
                {duration(1, DAYS), true, Time.zonedDateTime(ZoneOffset.UTC).minusDays(1).toString()},
                // 1 week from a week ago, should return true
                {duration(1, MINUTES), true, Time.zonedDateTime(ZoneOffset.UTC).minusMinutes(1).toString()},
                // 1 month from a month ago, should return true
                {duration(1, SECONDS), true, Time.zonedDateTime(ZoneOffset.UTC).minusSeconds(1).toString()}
        };
    }

    @Test(dataProvider = "timeSinceData")
    public void shouldReturnExpectedValue(Duration elapsedTime, boolean expectedResult, String time)
            throws Exception {

        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        when(idmIntegrationService.retrieveCreateDate(any(), any(), any(), any()))
                .thenReturn(json(object(field("createDate", time))));
        when(config.elapsedTime()).thenReturn(elapsedTime.toString());

        String outcome = timeSinceDecisionNode.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(expectedResult));
    }

    @Test(dataProvider = "timeSinceData")
    public void shouldReturnExpectedOutcomeWithUsernameAsIdentity(Duration elapsedTime, boolean expectedResult,
            String time) throws Exception {
        JsonValue sharedState = json(object(
                field(USERNAME, "test-username")
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        when(idmIntegrationService.retrieveCreateDate(any(), any(), any(), any()))
                .thenReturn(json(object(field("createDate", time))));
        when(config.elapsedTime()).thenReturn(elapsedTime.toString());

        String outcome = timeSinceDecisionNode.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(expectedResult));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfNoIdentityPresent() throws Exception {
        JsonValue sharedState = json(object());

        timeSinceDecisionNode.process(getContext(emptyList(), sharedState));
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }

    private static String nowAsString() {
        return Time.zonedDateTime(ZoneOffset.UTC).toString();
    }
}
