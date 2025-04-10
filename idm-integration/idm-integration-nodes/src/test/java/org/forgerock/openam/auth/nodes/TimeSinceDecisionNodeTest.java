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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.forgerock.util.time.Duration.duration;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TimeSinceDecisionNodeTest {

    @Mock
    private TimeSinceDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @InjectMocks
    private TimeSinceDecisionNode timeSinceDecisionNode;

    public static Stream<Arguments> timeSinceData() {
        TimeTravelUtil.useFixedClock(1_000_000);
        return Stream.of(
                // 1 day from now, should return false
                Arguments.of(duration(1, DAYS), false, nowAsString()),
                // 1 hour from now, should return false
                Arguments.of(duration(1, HOURS), false, nowAsString()),
                // 1 minute from now, should return false
                Arguments.of(duration(1, MINUTES), false, nowAsString()),
                // 1 second from now, should return false
                Arguments.of(duration(1, SECONDS), false, nowAsString()),
                // 1 day, 1 hour, 1 minute, 1 second from now, should return false
                Arguments.of(duration("1 day and 1 hour and 1 minute and 1 second"), false, nowAsString()),
                // zero time from now, should return true
                Arguments.of(Duration.ZERO, true, nowAsString()),
                // zero minutes from a minute before, should return true
                Arguments.of(Duration.ZERO, true, Time.zonedDateTime(ZoneOffset.UTC).minusMinutes(1).toString()),
                // 1 hour from an hour ago, should return true
                Arguments.of(duration(1, HOURS), true, Time.zonedDateTime(ZoneOffset.UTC).minusHours(1).toString()),
                // 1 day from a day ago, should return true
                Arguments.of(duration(1, DAYS), true, Time.zonedDateTime(ZoneOffset.UTC).minusDays(1).toString()),
                // 1 week from a week ago, should return true
                Arguments.of(duration(1, MINUTES), true, Time.zonedDateTime(ZoneOffset.UTC).minusMinutes(1).toString()),
                // 1 month from a month ago, should return true
                Arguments.of(duration(1, SECONDS), true, Time.zonedDateTime(ZoneOffset.UTC).minusSeconds(1).toString())
        );
    }

    private static String nowAsString() {
        return Time.zonedDateTime(ZoneOffset.UTC).toString();
    }

    @BeforeEach
    void setUp() throws Exception {
        TimeTravelUtil.useFixedClock(1_000_000);

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
    }

    @AfterEach
    void tearDown() {
        TimeTravelUtil.resetClock();
    }

    @Test
    void shouldThrowExceptionIfFailedToRetrieveObject() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenThrow(newResourceException(BAD_REQUEST));

        assertThatThrownBy(() -> timeSinceDecisionNode.process(getContext(emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldReturnFalseIfTimeSinceAttributeNotInObject() throws Exception {
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

    @ParameterizedTest
    @MethodSource("timeSinceData")
    public void shouldReturnExpectedValue(Duration elapsedTime, boolean expectedResult, String time)
            throws Exception {

        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(config.elapsedTime()).thenReturn("1 minute");
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        when(idmIntegrationService.retrieveCreateDate(any(), any(), any(), any()))
                .thenReturn(json(object(field("createDate", time))));
        when(config.elapsedTime()).thenReturn(elapsedTime.toString());

        String outcome = timeSinceDecisionNode.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(expectedResult));
    }

    @ParameterizedTest
    @MethodSource("timeSinceData")
    public void shouldReturnExpectedOutcomeWithUsernameAsIdentity(Duration elapsedTime, boolean expectedResult,
            String time) throws Exception {
        JsonValue sharedState = json(object(
                field(USERNAME, "test-username")
        ));

        when(config.elapsedTime()).thenReturn("1 minute");
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        when(idmIntegrationService.retrieveCreateDate(any(), any(), any(), any()))
                .thenReturn(json(object(field("createDate", time))));
        when(config.elapsedTime()).thenReturn(elapsedTime.toString());

        String outcome = timeSinceDecisionNode.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(expectedResult));
    }

    @Test
    void shouldThrowExceptionIfNoIdentityPresent() throws Exception {
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        JsonValue sharedState = json(object());

        assertThatThrownBy(() -> timeSinceDecisionNode.process(getContext(emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class);
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
