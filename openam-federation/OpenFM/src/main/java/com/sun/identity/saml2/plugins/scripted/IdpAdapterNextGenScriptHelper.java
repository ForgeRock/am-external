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
package com.sun.identity.saml2.plugins.scripted;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import org.forgerock.openam.annotations.Supported;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.saml2.plugins.scripted.wrappers.ScriptEntitlementInfoWrapper;
import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * Helper methods for Next Gen SAML2 IDP Adapter scripts.
 */
public class IdpAdapterNextGenScriptHelper {

    private final SSOToken ssoToken;
    private final AuthnRequest authnRequest;

    /**
     * Create a helper for the current request.
     *
     * @param ssoToken the SSO token for the current session
     * @param authnRequest the SAML2 AuthnRequest
     */
    public IdpAdapterNextGenScriptHelper(SSOToken ssoToken, AuthnRequest authnRequest) {
        this.ssoToken = ssoToken;
        this.authnRequest = authnRequest;
    }

    /**
     * Retrieve a list of entitlements for the current session and AuthnRequest.
     *
     * @param applicationName the application name to retrieve entitlement information for
     * @param realm the realm to evaluate
     * @return a list of ScriptEntitlementInfoWrapper objects, or null if required context is missing
     * @throws EntitlementException if an error occurs when evaluating the entitlement information
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<ScriptEntitlementInfoWrapper> getEntitlements(String applicationName, String realm)
            throws EntitlementException {
        return getEntitlements(applicationName, realm, emptyMap());
    }

    /**
     * Retrieve a list of entitlements for the current session and AuthnRequest.
     *
     * @param applicationName the application name to retrieve entitlement information for
     * @param realm the realm to evaluate
     * @param environment the environment parameters used in the evaluation
     * @return a list of ScriptEntitlementInfoWrapper objects, or null if required context is missing
     * @throws EntitlementException if an error occurs when evaluating the entitlement information
     */
    @Supported(scriptingApi = true, javaApi = false)
    public List<ScriptEntitlementInfoWrapper> getEntitlements(String applicationName, String realm,
            Map<String, List<String>> environment) throws EntitlementException {
        if (ssoToken == null || authnRequest == null) {
            return null;
        }
        Subject subject = SubjectUtils.createSubject(ssoToken);
        String issuer = authnRequest.getIssuer() == null ? null : authnRequest.getIssuer().getValue();
        Set<String> resources = issuer == null ? emptySet() : Collections.singleton(issuer);
        Map<String, Set<String>> envParams = toSetMap(environment);
        Evaluator evaluator = getEvaluatorForSubject(applicationName, subject);
        return evaluator.evaluate(realm, subject, resources, envParams).stream()
                .map(ScriptEntitlementInfoWrapper::new)
                .collect(Collectors.toList());
    }

    Evaluator getEvaluatorForSubject(String applicationName, Subject subject) {
        return new Evaluator(subject, applicationName);
    }

    private Map<String, Set<String>> toSetMap(Map<String, List<String>> environment) {
        if (environment == null || environment.isEmpty()) {
            return emptyMap();
        }
        return environment.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() == null ? emptySet() : new LinkedHashSet<>(entry.getValue())
                ));
    }
}
