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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 * Portions copyright 2015-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.Utils;
import org.forgerock.openam.radius.server.config.ClientConfig;
import org.forgerock.openam.radius.server.spi.AccessRequestHandler;
import org.forgerock.openam.radius.server.spi.handlers.AcceptAllHandler;
import org.forgerock.openam.radius.server.spi.handlers.RejectAllHandler;
import org.forgerock.openam.test.extensions.LoggerExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.eventbus.EventBus;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Tests for {@link RadiusRequestHandler}.
 */
@ExtendWith(MockitoExtension.class)
public class RadiusRequestHandlerTest {

    @RegisterExtension
    public LoggerExtension loggerExtension = new LoggerExtension(RadiusRequestHandler.class);

    @Mock
    private AccessRequestHandlerFactory accessRequestHandlerFactory;
    @Mock
    private RadiusRequestContext reqCtx;
    @Mock
    private EventBus eventBus;
    @Mock
    private MessageAuthenticatorCalculator calculator;

    private final String res = "01 00 00 38 0f 40 3f 94 73 97 80 57 bd 83 d5 cb "
            + "98 f4 22 7a 01 06 6e 65 6d 6f 02 12 0d be 70 8d 93 d4 13 ce 31 96 e4 3f 78 2a 0a ee 04 06 c0 a8 "
            + "01 10 05 06 00 00 00 03";

    @BeforeEach
    void setUp() {
        given(reqCtx.getClientConfig()).willReturn(mock(ClientConfig.class));
    }

    /**
     * Test that when run is called with an AcceptAllHandler that the resultant packet sent via the request
     * context is an ACCESS_ACCEPT packet.
     *
     * @throws RadiusProcessingException - when something goes wrong processing a RADIUS packet.
     */
    @Test
    void testRun() throws RadiusProcessingException {

        // Given
        when(accessRequestHandlerFactory.getAccessRequestHandler(reqCtx)).thenReturn(new AcceptAllHandler());
        final ByteBuffer bfr = Utils.toBuffer(res);
        RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus,
                calculator);

        // When
        handler.run();

        // Then
        verify(reqCtx).send(isA(AccessAccept.class));
    }

    /**
     * Test that when run is called with an RejectAllHandler that the resultant packet sent via the request
     * context is an ACCESS_REJECT packet.
     *
     * @throws RadiusProcessingException - when something goes wrong processing a RADIUS packet.
     */
    @Test
    void testRunReject() throws RadiusProcessingException {

        // Given
        when(accessRequestHandlerFactory.getAccessRequestHandler(reqCtx)).thenReturn(new RejectAllHandler());
        final ByteBuffer bfr = Utils.toBuffer(res);
        RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus,
                calculator);

        // When
        handler.run();

        // Then
        verify(reqCtx).send(isA(AccessReject.class));
    }

    /**
     * Test that when run is called with an CatestrophicHandler that the run completes without sending any
     * packet. (ie: the packet is silently dropped since it can't be handled.)
     *
     * @throws RadiusProcessingException - when something goes wrong processing a RADIUS packet.
     */
    @Test
    void testRunCatastrophic() throws RadiusProcessingException {

        // Given
        final ByteBuffer bfr = Utils.toBuffer(res);
        when(accessRequestHandlerFactory.getAccessRequestHandler(reqCtx)).thenReturn(new CatastrophicHandler());
        RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus,
                calculator);

        // When
        handler.run();

        // Then
        verify(reqCtx, never()).send(isA(Packet.class));
    }

    @Test
    void shouldRunGivenValidMessageAuthenticator() throws RadiusProcessingException {
        // Given
        String messageAuthenticatorValue = "f7 ef dc 4a 2d 4c ea 1e 39 66 c6 41 6a 64 55 57";
        String radiusPacket = "01 71 00 4a 50 05 f8 3e 5b 19 ac 47 26 49 ea 6c e0 b6 d0 90 50 12 "
                + messageAuthenticatorValue + " 01 06 64 65 6d 6f 02 12 d5 6e 37 82 31 e4 d6 97 "
                + "7f f6 e3 c4 22 88 70 2a 04 06 7f 00 00 01 05 06 00 00 00 0a";
        ByteBuffer bfr = Utils.toBuffer(radiusPacket);
        String clientSecret = "secret";
        given(reqCtx.getClientSecret()).willReturn(clientSecret);
        given(reqCtx.getClientConfig().isMessageAuthenticatorRequired()).willReturn(true);
        given(calculator.computeFromPayloadIfPresent(bfr, clientSecret))
                .willReturn(Optional.of(Utils.toBuffer(messageAuthenticatorValue)));
        given(accessRequestHandlerFactory.getAccessRequestHandler(reqCtx)).willReturn(mock(AccessRequestHandler.class));
        RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus,
                calculator);

        // When
        handler.run();

        // Then
        verify(reqCtx).send(null);
    }

    @Test
    void shouldRunGivenInvalidMessageAuthenticator() throws RadiusProcessingException {
        // Given
        ByteBuffer bfr = Utils.toBuffer(res);
        String clientSecret = "secret";
        given(reqCtx.getClientSecret()).willReturn(clientSecret);
        given(reqCtx.getClientConfig().isMessageAuthenticatorRequired()).willReturn(true);
        given(calculator.computeFromPayloadIfPresent(bfr, clientSecret)).willReturn(Optional.empty());
        RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus,
                calculator);

        // When
        handler.run();

        // Then
        assertThat(loggerExtension.getDebug(ILoggingEvent::getMessage))
                .containsOnlyOnce("Message-Authenticator is missing or invalid. Dropping the request");
        verify(reqCtx, never()).send(isA(Packet.class));
    }

    @Test
    void runShouldNotCalculateMessageAuthenticatorGivenAccountingRequestType() {
        // Given
        String radiusPacket = "04 71 00 4a 50 05 f8 3e 5b 19 ac 47 26 49 ea 6c e0 b6 d0 90 50 12 01 06 64";
        given(reqCtx.getClientConfig().isMessageAuthenticatorRequired()).willReturn(true);
        ByteBuffer bfr = Utils.toBuffer(radiusPacket);
        RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus,
                calculator);

        // When
        handler.run();

        // Then
        assertThat(loggerExtension.getWarnings(ILoggingEvent::getMessage))
                .containsOnlyOnce("Unable to parse packet received from RADIUS client 'null'. Dropping.");
        verify(calculator, never()).computeFromPayloadIfPresent(bfr, reqCtx.getClientSecret());
    }

    @Test
    void runShouldDropRequestGivenNullBufferAndMessageAuthenticatorRequired() {
        // Given
        ByteBuffer bfr = null;
        given(reqCtx.getClientConfig().isMessageAuthenticatorRequired()).willReturn(true);
        RadiusRequestHandler handler = new RadiusRequestHandler(accessRequestHandlerFactory, reqCtx, bfr, eventBus,
                calculator);

        // When
        handler.run();

        // Then
        assertThat(loggerExtension.getWarnings(ILoggingEvent::getMessage))
                .containsOnlyOnce("Unable to parse packet received from RADIUS client 'null'. Dropping.");
    }

}
