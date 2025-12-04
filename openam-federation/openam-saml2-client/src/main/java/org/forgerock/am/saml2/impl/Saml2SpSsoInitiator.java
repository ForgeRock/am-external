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
 * Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.am.saml2.impl;

import static com.sun.identity.saml2.meta.SAML2MetaUtils.getAttributes;
import static java.util.Collections.singletonMap;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.AM_LOCATION_COOKIE;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.am.saml2.api.Saml2SsoInitiator;
import org.forgerock.am.saml2.api.Saml2Options;
import org.forgerock.am.saml2.api.Saml2SsoException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.headers.CookieUtilsWrapper;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Options;
import org.forgerock.util.encode.Base64url;

import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * Initiates SAML2 single sign-on on the service provider side. This involves the creation of a SAML2 authentication
 * request, saving it to CTS (so that necessary validations can be completed when the response is received), and
 * constructs a callback that can be used by the authentication framework to trigger the SAML2 authentication.
 */
public class Saml2SpSsoInitiator implements Saml2SsoInitiator {

    private final SAML2MetaManager metaManager;
    private final CookieUtilsWrapper cookieUtils;
    private final AuthnRequestUtils authnRequestUtils;

    /**
     * Constructor.
     *
     * @param metaManager The SAML2 metadata manager.
     * @param cookieUtils Cookie utilities.
     * @param authnRequestUtils Authentication request utilities.
     */
    @Inject
    public Saml2SpSsoInitiator(SAML2MetaManager metaManager, CookieUtilsWrapper cookieUtils,
            AuthnRequestUtils authnRequestUtils) {
        this.metaManager = metaManager;
        this.cookieUtils = cookieUtils;
        this.authnRequestUtils = authnRequestUtils;
    }

    @Override
    public Callback initiateSso(HttpServletRequest request, HttpServletResponse response, Realm realm,
            String spEntityId, String idpEntityId, Options saml2Options) throws Saml2SsoException {
        try {
            String realmName = realm.asPath();
            IDPSSODescriptorType idpDescriptor = metaManager.getIDPSSODescriptor(realmName, idpEntityId);
            SPSSODescriptorType spDescriptor = metaManager.getSPSSODescriptor(realmName, spEntityId);
            if (idpDescriptor == null || spDescriptor == null) {
                throw new Saml2SsoException("Failed to load SAML2 Configuration.");
            }
            Map<String, List<String>> spConfig =
                    getAttributes(metaManager.getSPSSOConfig(realmName, spEntityId));

            EndpointType ssoEndpoint = getSsoEndpoint(idpDescriptor, saml2Options);
            AuthnRequest authnRequest = authnRequestUtils.createAuthnRequest(request, response, realm, spEntityId,
                    idpEntityId, spDescriptor, spConfig, idpDescriptor, ssoEndpoint, saml2Options);

            authnRequestUtils.cacheAuthnRequest(authnRequest);
            saveLoginUrlInCookie(request, response, realmName);

            return getRedirectCallback(authnRequest, realm, idpEntityId, spDescriptor, spEntityId, idpDescriptor,
                    ssoEndpoint);
        } catch (SAML2Exception ex) {
            throw new Saml2SsoException(ex.getMessage(), ex);
        }
    }

    private EndpointType getSsoEndpoint(IDPSSODescriptorType idpDescriptor, Options saml2Options)
            throws Saml2SsoException {
        EndpointType endPoint = authnRequestUtils.findSsoEndpoint(idpDescriptor,
                saml2Options.get(Saml2Options.REQUEST_BINDING));
        if (endPoint == null || StringUtils.isEmpty(endPoint.getLocation())) {
            throw new Saml2SsoException(SAML2Utils.bundle.getString("ssoServiceNotfound"));
        }
        return endPoint;
    }

    private void saveLoginUrlInCookie(HttpServletRequest request, HttpServletResponse response, String realm) {
        StringBuilder originalUrl = new StringBuilder();
        originalUrl.append(request.getContextPath());
        originalUrl.append("/?realm=").append(urlEncodeQueryParameterNameOrValue(realm));

        String requestedQuery = request.getQueryString();
        if (requestedQuery != null) {
            originalUrl.append('&').append(requestedQuery);
        }

        cookieUtils.addCookieToResponseForRequestDomains(request, response, AM_LOCATION_COOKIE,
                Base64url.encode(originalUrl.toString()), -1);
    }

    private Callback getRedirectCallback(AuthnRequest authnRequest, Realm realm, String idpEntityId,
            SPSSODescriptorType spDescriptor, String spEntityId, IDPSSODescriptorType idpDescriptor,
            EndpointType ssoEndpoint) throws SAML2Exception {
        if (SAML2Constants.HTTP_POST.equals(ssoEndpoint.getBinding())) {
            String postMsg = authnRequestUtils.encodeForPostBinding(authnRequest, realm, idpEntityId, spDescriptor,
                    spEntityId, idpDescriptor);
            return createRedirectCallback(ssoEndpoint.getLocation(), singletonMap(SAML2Constants.SAML_REQUEST, postMsg),
                    "POST");
        } else {
            String redirectUrl = authnRequestUtils.getRedirectBindingUrl(authnRequest, realm, idpEntityId, spDescriptor,
                    spEntityId, idpDescriptor, ssoEndpoint);
            return createRedirectCallback(redirectUrl, null, "GET");
        }
    }

    private RedirectCallback createRedirectCallback(String redirectUrl, Map<String, String> redirectData,
            String method) {
        RedirectCallback redirectCallback = new RedirectCallback();
        redirectCallback.setRedirectUrl(redirectUrl);
        redirectCallback.setRedirectData(redirectData);
        redirectCallback.setMethod(method);
        redirectCallback.setTrackingCookie(true);
        return redirectCallback;
    }
}
