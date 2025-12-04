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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.am.saml2.api.Saml2Options;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.util.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.profile.AuthnRequestInfo;
import com.sun.identity.saml2.profile.AuthnRequestInfoCopy;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.profile.SPSSOFederate;
import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * This class contains utility methods for performing service provider initiated SAML2 single sign-on flows.
 */
@Singleton
class AuthnRequestUtils {

    private static final Logger logger = LoggerFactory.getLogger(AuthnRequestUtils.class);

    /**
     * Finds the first SingleSignOnService endpoint that matches the requested binding. If there is no constraint on the
     * request binding, this method will choose the first endpoint with a supported binding.
     *
     * @param idpDescriptor The IDP's standard metadata.
     * @param requestBinding The binding the IDP needs to support for SSO. May be null to imply that any supported
     * binding is acceptable.
     * @return The SSO endpoint that matched the requested binding. May be null.
     */
    EndpointType findSsoEndpoint(IDPSSODescriptorType idpDescriptor, String requestBinding) {
        return SPSSOFederate.getSingleSignOnServiceEndpoint(idpDescriptor.getSingleSignOnService(), requestBinding);
    }

    /**
     * Creates a new SAML Authentication Request based on the provided details.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @param realm The realm the hosted service provider belongs to.
     * @param spEntityId The service provider's entity ID.
     * @param idpEntityId The identity provider's entity ID.
     * @param spDescriptor The service provider's standard metadata.
     * @param spConfig The service provider's extended metadata.
     * @param idpDescriptor The identity provider's standard metadata.
     * @param ssoEndpoint The SSO endpoint where the authentication request should be sent to.
     * @param saml2Options Additional SAML2 options that control the contents of the authentication request.
     * @return The SAML2 authentication request.
     * @throws SAML2Exception If there was an error while creating the SAML2 authentication request.
     */
    AuthnRequest createAuthnRequest(HttpServletRequest request, HttpServletResponse response,
            Realm realm, String spEntityId, String idpEntityId, SPSSODescriptorType spDescriptor,
            Map<String, List<String>> spConfig, IDPSSODescriptorType idpDescriptor, EndpointType ssoEndpoint,
            Options saml2Options) throws SAML2Exception {
        return SPSSOFederate.createAuthnRequest(request, response, realm.asPath(), spEntityId,
                idpEntityId, getParameterMap(saml2Options), spConfig, emptyList(), spDescriptor, idpDescriptor,
                ssoEndpoint.getLocation(), false);
    }

    /**
     * Stores the provided SAML2 authentication request in memory as well as in SAML SFO.
     *
     * @param authnRequest The SAML2 authentication request to cache.
     * @throws SAML2Exception If the authentication request could not be cached in SAML SFO.
     */
    void cacheAuthnRequest(AuthnRequest authnRequest) throws SAML2Exception {
        AuthnRequestInfo reqInfo = new AuthnRequestInfo(authnRequest, null);
        synchronized (SPCache.requestHash) {
            SPCache.requestHash.put(authnRequest.getID(), reqInfo);
        }
        String key = authnRequest.getID();

        try {
            SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(key, new AuthnRequestInfoCopy(reqInfo));
            logger.debug("AuthnRequestInfoCopy for requestID {} saved in SAML SFO", key);
        } catch (SAML2TokenRepositoryException e) {
            logger.error("Unable to save AuthnRequestInfoCopy in SAML SFO for requestID {}", key, e);
            throw new SAML2Exception("Saml2Client", "samlFailover");
        }
    }

    /**
     * Optionally signs the authentication request and Base64 encodes the serialised authentication request to prepare
     * it for transmission using the HTTP-POST binding.
     *
     * @param authnRequest The SAML authentication request.
     * @param realm The realm the hosted service provider belongs to.
     * @param idpEntityId The identity provider's entity ID.
     * @param spDescriptor The service provider's standard metadata.
     * @param spEntityId The service provider's entity ID.
     * @param idpDescriptor The identity provider's standard metadata.
     * @return The encoded SAML2 authentication request.
     * @throws SAML2Exception If there was an error while signing or serialising the authentication request.
     */
    String encodeForPostBinding(AuthnRequest authnRequest, Realm realm, String idpEntityId,
            SPSSODescriptorType spDescriptor, String spEntityId, IDPSSODescriptorType idpDescriptor)
            throws SAML2Exception {
        return SPSSOFederate.getPostBindingMsg(realm.asPath(), idpEntityId, idpDescriptor, spDescriptor, spEntityId,
                authnRequest);
    }

    /**
     * Compresses the authentication request, Base64 encodes the binary, then optionally signs the query string and adds
     * the signature related parameters to the query string. The resulting query string will be added to the SSO
     * endpoint's location.
     *
     * @param authnRequest The SAML2 authentication request.
     * @param realm The realm the hosted service provider belongs to.
     * @param idpEntityId The identity provider's entity ID.
     * @param spDescriptor The service provider's standard metadata.
     * @param spEntityId The service provider's entity ID.
     * @param idpDescriptor The identity provider's standard metadata.
     * @param ssoEndpoint The identity provider's SSO endpoint where the authentication request should be sent.
     * @return The URL that contains the SAML2 authentication request encoded according to HTTP-Redirect binding's
     * requirements.
     * @throws SAML2Exception If there was an error while signing or serialising the authentication request.
     */
    String getRedirectBindingUrl(AuthnRequest authnRequest, Realm realm, String idpEntityId,
            SPSSODescriptorType spDescriptor, String spEntityId, IDPSSODescriptorType idpDescriptor,
            EndpointType ssoEndpoint) throws SAML2Exception {
        return SPSSOFederate.getRedirect(realm.asPath(), idpEntityId, authnRequest.toXMLString(true, true), null,
                ssoEndpoint.getLocation(), idpDescriptor, spDescriptor, spEntityId);
    }

    private Map<String, List<String>> getParameterMap(Options saml2Options) {
        return ImmutableMap.<String, List<String>>builder()
                .put(SAML2Constants.ALLOWCREATE, singletonList(saml2Options.get(Saml2Options.ALLOW_CREATE).toString()))
                .put(SAML2Constants.AUTH_CONTEXT_CLASS_REF, saml2Options.get(Saml2Options.AUTH_CONTEXT_CLASS_REF))
                .put(SAML2Constants.AUTH_CONTEXT_DECL_REF, saml2Options.get(Saml2Options.AUTH_CONTEXT_DECL_REF))
                .put(SAML2Constants.BINDING, singletonList(saml2Options.get(Saml2Options.RESPONSE_BINDING)))
                .put(SAML2Constants.FORCEAUTHN, singletonList(saml2Options.get(Saml2Options.FORCE_AUTHN).toString()))
                .put(SAML2Constants.ISPASSIVE, singletonList(saml2Options.get(Saml2Options.IS_PASSIVE).toString()))
                .put(SAML2Constants.NAMEID_POLICY_FORMAT, singletonList(saml2Options.get(Saml2Options.NAME_ID_FORMAT)))
                .put(SAML2Constants.REQ_BINDING, singletonList(saml2Options.get(Saml2Options.REQUEST_BINDING)))
                .put(SAML2Constants.SP_AUTHCONTEXT_COMPARISON,
                        singletonList(saml2Options.get(Saml2Options.AUTH_COMPARISON).name().toLowerCase()))
                .build();
    }
}
