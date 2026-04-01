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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.any;

import java.util.Properties;

import org.forgerock.guice.core.GuiceBind;
import org.forgerock.guice.core.GuiceExtension;
import org.forgerock.openam.radius.server.config.ClientConfig;
import org.forgerock.openam.radius.server.spi.AccessRequestHandler;
import org.forgerock.openam.radius.server.spi.handlers.RejectAllHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link AccessRequestHandlerFactory}.
 */
@ExtendWith({MockitoExtension.class, GuiceExtension.class})

class AccessRequestHandlerFactoryTest {

    @Mock
    private RadiusRequestContext requestContext;

    @Mock
    private ClientConfig clientConfig;

    @Mock
    @GuiceBind(AccessRequestHandler.class)
    private AccessRequestHandler accessRequestHandler;

    private AccessRequestHandlerFactory requestHandlerFactory;

    @BeforeEach
    void setUp() {
        requestHandlerFactory = new AccessRequestHandlerFactory();
        given(requestContext.getClientConfig()).willReturn(clientConfig);
        given(clientConfig.getHandlerConfig()).willReturn(new Properties());
    }

    @Test
    void shouldReturnRejectAllHandlerGivenInvalidRadiusClientConfigurationException()
            throws Exception {
        // Given
        willThrow(new InvalidRadiusClientConfigurationException("invalid"))
                .given(accessRequestHandler).init(any(Properties.class));
        given(clientConfig.getAccessRequestHandlerClass()).willReturn(AccessRequestHandler.class);

        // When
        AccessRequestHandler handler = requestHandlerFactory.getAccessRequestHandler(requestContext);

        // Then
        assertThat(handler).isInstanceOf(RejectAllHandler.class);
    }

    @Test
    void shouldReturnNullGivenGenericInitException() throws Exception {
        // Given
        willThrow(new IllegalStateException("generic"))
                .given(accessRequestHandler).init(any(Properties.class));
        given(clientConfig.getAccessRequestHandlerClass()).willReturn(AccessRequestHandler.class);

        // When
        AccessRequestHandler handler = requestHandlerFactory.getAccessRequestHandler(requestContext);

        // Then
        assertThat(handler).isNull();
    }

    @Test
    void shouldReturnHandlerGivenValidInitialization() {
        // Given
        given(clientConfig.getAccessRequestHandlerClass()).willReturn(AccessRequestHandler.class);

        // When
        AccessRequestHandler handler = requestHandlerFactory.getAccessRequestHandler(requestContext);

        // Then
        assertThat(handler).isSameAs(accessRequestHandler);
    }
}
