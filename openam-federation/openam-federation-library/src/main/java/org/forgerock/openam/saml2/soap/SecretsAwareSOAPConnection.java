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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.soap;

import static com.sun.identity.saml2.common.SAML2Constants.SECRET_ID_IDENTIFIER;
import static com.sun.identity.saml2.common.SAML2Utils.getAttributeValueFromSSOConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.net.ssl.KeyManager;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.http.HttpStatus;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Header;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.services.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.shared.encode.Base64;

/**
 * An implementation of {@link SOAPConnection} that uses an {@link Handler} instance to make the connection.
 * The handler is configured with a {@link KeyManager} retrieved from the secrets API in order to facilitate
 * authentication with a client certificate if required.
 */
public class SecretsAwareSOAPConnection extends SOAPConnection {

    private static final String AUTHORISATION_HEADER = "Authorization";
    private static final String POST = "POST";
    private static final Logger logger = LoggerFactory.getLogger(SecretsAwareSOAPConnection.class);

    private final SamlMtlsHandlerFactory factory;
    private final Realm realm;
    private final SOAPCommunicator soapCommunicator;
    private final String secretLabel;

    private List<Header> headers;

    @Inject
    public SecretsAwareSOAPConnection(SamlMtlsHandlerFactory samlMtlsHandlerFactory, @Assisted Realm realm,
            @Assisted SOAPCommunicator soapCommunicator, @Assisted String entityId, @Assisted Saml2EntityRole role) {
        this.factory = samlMtlsHandlerFactory;
        this.realm = realm;
        this.soapCommunicator = soapCommunicator;
        this.secretLabel = getAttributeValueFromSSOConfig(realm.asPath(), entityId, role.getName(),
                SECRET_ID_IDENTIFIER);
    }

    /**
     * Sends the given SOAP message to the specified endpoint, waiting until a response is received.
     * @param soapMessage the SOAPMessage object to be sent
     * @param location identifies where the message should be sent
     * @return the SOAPMessage received in response to the request
     *
     * @throws SOAPException if there is a SOAP error
     */
    @Override
    public SOAPMessage call(SOAPMessage soapMessage, Object location) throws SOAPException {
        try {
            URI locationUri;
            // Support String and URI location objects
            // Should strictly also support javax.xml.messaging.URLEndpoint
            // see https://docs.oracle.com/javase/8/docs/api/index.html?javax/xml/soap/SOAPConnection.html
            if (location instanceof String) {
                locationUri = new URI(location.toString());
            } else if (location instanceof URI){
                locationUri = (URI)location;
            } else {
                throw new IllegalArgumentException("location must be string or URI");
            }

            // Create Headers
            headers = headersFromMessage(soapMessage);
            addBasicAuthIfRequired(locationUri);

            Response response = factory.getHandler(realm, secretLabel).handle(new RootContext(),
                            createSOAPRequest(soapMessage, locationUri.toString())).getOrThrowIfInterrupted();

            int responseCode = response.getStatus().getCode();

            // Expect 200 (OK) or 500 (error). Anything else, throw an exception.
            // SOAP faults can be present in both cases.
            if (!List.of(HttpStatus.SC_OK, HttpStatus.SC_INTERNAL_SERVER_ERROR).contains(responseCode)) {
                throw new SOAPException("SOAPConnection failed with error code " + responseCode + " and message "
                        + response.getEntity().getString());
            }

            return soapCommunicator.getSOAPMessage(response);
        } catch (URISyntaxException | IOException e) {
            // logged by caller
            throw new SOAPException(e);
        }
    }

    @Override
    public void close() throws SOAPException {
        // nothing to do
    }

    private List<Header> headersFromMessage(SOAPMessage soapMessage) {
        List<Header> headers = new ArrayList<>();

        // Transfer headers from SOAP Message.
        // SOAP Message should contain required SOAPAction and ContentType headers.
        MimeHeaders mimeHeaders = soapMessage.getMimeHeaders();
        mimeHeaders.getAllHeaders().forEachRemaining(mimeHeader -> {
            headers.add(new Header() {
                @Override
                public String getName() {
                    return mimeHeader.getName();
                }

                @Override
                public List<String> getValues() {
                    return List.of(mimeHeader.getValue());
                }
            });
        });

        return headers;
    }

    private boolean authorizationHeaderPresent() {
        return headers.stream().anyMatch(header -> header.getName().equals(AUTHORISATION_HEADER));
    }

    private void addBasicAuthIfRequired(URI locationUri) {
        String userInfo = locationUri.getRawUserInfo();
        if (userInfo != null && !authorizationHeaderPresent()) {
            headers.add(new Header() {
                @Override
                public String getName() {
                    return AUTHORISATION_HEADER;
                }

                @Override
                public List<String> getValues() {
                    return List.of("Basic " + Base64.encode(userInfo.getBytes(StandardCharsets.UTF_8)));
                }
            });
        }
    }

    private Request createSOAPRequest(SOAPMessage soapMessage, String location) throws SOAPException, IOException,
            URISyntaxException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        soapMessage.writeTo(stream);

        return new Request()
                .setUri(location)
                .setMethod(POST)
                .setEntity(stream.toString())
                .addHeaders(headers.toArray(new Header[0]));
    }
}
