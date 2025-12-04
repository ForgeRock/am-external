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
 * Copyright 2016-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.plugins;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.protocol.Status;
import org.forgerock.openam.authentication.api.AuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;

import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.authentication.AuthIndexType;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.authentication.api.AuthenticationConstants;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.wsfederation.common.ActiveRequestorException;

import static com.sun.identity.authentication.util.ISAuthConstants.AUTH_SERVICE_NAME;
import static com.sun.identity.authentication.util.ISAuthConstants.PRINCIPAL_UID_REQUEST_ATTR;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

/**
 * The default {@link WsFedAuthenticator} implementation that just authenticates using the default authentication chain
 * in the selected realm.
 */
public class DefaultWsFedAuthenticator implements WsFedAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultWsFedAuthenticator.class);
    private static final int AUTHENTICATION_START = 0;
    private static final int AUTHENTICATION_MAX_TRIES = 3;
    private static final int FIRST_ELEMENT = 0;

    private final AuthenticationHandler authenticationHandler;
    private final CoreWrapper coreWrapper;
    private final RealmLookup realmLookup;
    private final SSOTokenManager ssoTokenManager;

    /**
     * Default Constructor.
     */
    public DefaultWsFedAuthenticator() {
        this.authenticationHandler = InjectorHolder.getInstance(AuthenticationHandler.class);
        this.coreWrapper = InjectorHolder.getInstance(CoreWrapper.class);
        this.realmLookup = InjectorHolder.getInstance(RealmLookup.class);
        this.ssoTokenManager = InjectorHolder.getInstance(SSOTokenManager.class);
    }

    /**
     * Constructs a new DefaultWsFedAuthenticator.
     *
     * @param authenticationHandler The authentication handler to be used for authentication.
     * @param coreWrapper The guice-safe wrapper around core APIs.
     * @param realmLookup Realm lookup utility.
     * @param ssoTokenManager The Token manager that can obtain the associated SSOToken given a tokenId string value.
     */
    public DefaultWsFedAuthenticator(AuthenticationHandler authenticationHandler, CoreWrapper coreWrapper,
            RealmLookup realmLookup, SSOTokenManager ssoTokenManager) {
        this.authenticationHandler = authenticationHandler;
        this.coreWrapper = coreWrapper;
        this.realmLookup = realmLookup;
        this.ssoTokenManager = ssoTokenManager;
    }

    @Override
    public SSOToken authenticate(HttpServletRequest servletRequest, HttpServletResponse servletResponse, SOAPMessage soapMessage,
            String realmIdentifier, String username, char[] password) throws ActiveRequestorException {
        try {
            Realm realm = realmLookup.lookup(realmIdentifier);
            JsonValue payload = json(object(0));
            String indexType = AuthIndexType.SERVICE.toString();
            String indexValue = getDefaultService(realm);
            DefaultWsFedAuthHttpRequestWrapper servletRequestWrapper =
                    new DefaultWsFedAuthHttpRequestWrapper(servletRequest);
            servletRequestWrapper.addParameter(ISAuthConstants.REALM_PARAM, realm.asPath());

            // Supports username/password callbacks only and up to 2 callbacks max.
            // Either username and password callbacks combined or separate callback and loop iteration for each.
            // If authentication hasn't succeeded on 3rd try (after submission of 2nd callback), then fail.
            for (int authenticationCount = AUTHENTICATION_START; authenticationCount < AUTHENTICATION_MAX_TRIES;
                     authenticationCount++) {
                Response response = authenticationHandler.authenticate(servletRequestWrapper, servletResponse, payload,
                        indexType, indexValue, null);
                payload = getPayload(response);
                if (servletRequest.getAttribute(PRINCIPAL_UID_REQUEST_ATTR) != null) {
                    // Successful authentication!
                    JsonValue tokenId = payload.get("tokenId");
                    return ssoTokenManager.createSSOToken(tokenId.asString());
                }

                JsonValue missing = handleCallbacks(payload, response, username, password);
                if (missing.size() > 0) {
                    // there's missing requirements not able to be fulfilled by this authentication.
                    logger.error("Authentication failed for user, could not fulfil authn requirements: {}", missing);
                }

                int statusCode = response.getStatus().getCode();
                if (statusCode >= Status.BAD_REQUEST.getCode() &&
                        statusCode < Status.INTERNAL_SERVER_ERROR.getCode()) {
                    throw newSenderUnauthorizedException();
                } else if (statusCode > Status.INTERNAL_SERVER_ERROR.getCode()) {
                    throw newReceiverException();
                }
            }
            // Catch-all failure
            logger.error("Authentication failed for user");
            throw newReceiverException();
        } catch (RealmLookupException e) {
            logger.error("Failed to lookup realm {}, {}", realmIdentifier, e);
            throw newSenderUnauthorizedException();
        } catch (RestAuthException | IOException e) {
            logger.error("An error occurred while trying to authenticate the end-user", e);
            throw newSenderUnauthorizedException();
        } catch (SSOException e) {
            logger.error("An error occurred while trying to obtain the session ID during authentication", e);
            throw newReceiverException();
        }
    }

    /**
     * Populate the received callbacks with the supplied username and password information.
     *
     * @param payload The response paylod.
     * @param response The response received from the authentication API.
     * @param username The supplied username credential.
     * @param password The supplied password credential.
     * @return JsonValue of any callbacks that cannot be satisfied by this logic
     * @throws ActiveRequestorException If there is a failure to perform the authentication.
     * @throws IOException If there is a failure retrieving information from the response entity.
     */
    private JsonValue handleCallbacks(JsonValue payload, Response response, String username, char[] password)
            throws ActiveRequestorException, IOException {
        if (!payload.isDefined(AuthenticationConstants.CALLBACKS)) {
            logger.error("Authentication failed for user: {}", response.getEntity().getString());
            throw newSenderUnauthorizedException();
        }
        JsonValue missing = json(array());
        for (JsonValue callback : payload.get("callbacks")) {
            String type = callback.get("type").asString();
            if (type.equals("NameCallback")) {
                callback.get("input").get(FIRST_ELEMENT).put("value", username);
            } else if (type.equals("PasswordCallback")) {
                callback.get("input").get(FIRST_ELEMENT).put("value",
                        password != null ? new String(password) : null);
            } else {
                missing.add(callback.getObject());
            }
        }
        return missing;
    }

    /**
     * Determine the default authentication service set for the realm.
     *
     * @param realm The realm/organization.
     * @return The name of the authentication service.
     * @throws ActiveRequestorException If there is a failure in looking up the authentication service.
     */
    private String getDefaultService(Realm realm) throws ActiveRequestorException {
        try {
            ServiceConfigManager scm = coreWrapper.getServiceConfigManager(AUTH_SERVICE_NAME,
                    coreWrapper.getAdminToken());
            if (scm == null) {
                logger.error("Failed to retrieve service config manager");
                throw newReceiverException();
            }
            ServiceConfig organizationConfig = scm.getOrganizationConfig(realm.asDN(), null);
            if (organizationConfig == null) {
                logger.error("Failed to retrieve service config for organization {}", realm.asDN());
                throw newReceiverException();
            }
            return CollectionHelper.getMapAttr(organizationConfig.getAttributes(), ISAuthConstants.AUTHCONFIG_ORG);
        } catch (SSOException | SMSException e) {
            logger.error("Could not get auth service config", e);
            throw newReceiverException();
        }
    }

    /**
     * Obtain the payload from the supplied response.
     *
     * @param response The response.
     * @return The Json payload.
     * @throws IOException If there is a failure retrieving the response entity.
     */
    private JsonValue getPayload(Response response) throws IOException {
        return json(response.getEntity().getJson());
    }

    /**
     * Create a new sender exception to represent an unauthorized error, i.e. 401.
     *
     * @return The exception.
     */
    private ActiveRequestorException newSenderUnauthorizedException() {
        return ActiveRequestorException.newSenderException("unableToAuthenticate")
                .setStatusCode(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Create a new receiver exception to represent a server side error, i.e. 5xx error.
     * @return
     */
    private ActiveRequestorException newReceiverException() {
        return ActiveRequestorException.newReceiverException("unableToAuthenticate");
    }
}
