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
 * Copyright 2021-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins.scripted;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.security.auth.Subject;

import org.forgerock.openam.annotations.EvolvingAll;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * Provides helper functions for IDP Adapter Script Implementations.
 */
@EvolvingAll
@Singleton
public class IdpAdapterScriptHelper {

    /**
     * Retrieve the issuer name for the AuthN request.
     *
     * @param authnRequest the AuthN request
     * @return the issuer name
     */
    public String getIssuerName(AuthnRequest authnRequest) {
        return authnRequest.getIssuer().getValue();
    }

    /**
     * Retrieve the issuer name from the AuthN request, within a Singleton Set.
     *
     * @param authnRequest the {@link AuthnRequest}
     * @return the resources for the AuthN token
     */
    public Set<String> getResourcesForToken(AuthnRequest authnRequest) {
        return Collections.singleton(getIssuerName(authnRequest));
    }

    /**
     * Retrieve {@link Subject} for an {@link SSOToken}.
     *
     * @param sessionToken the SSO Token of type Object
     * @return the {@link Subject} associated with the SSO Token
     */
    public Subject getSubjectForToken(Object sessionToken) {
        return getSubjectForToken((SSOToken) sessionToken);
    }

    /**
     * Retrieve {@link Subject} for an {@link SSOToken}.
     *
     * @param sessionToken the {@link SSOToken}
     * @return the {@link Subject} associated with the SSO Token
     */
    public Subject getSubjectForToken(SSOToken sessionToken) {
        return SubjectUtils.createSubject(sessionToken);
    }

    /**
     * Retrieve {@link Evaluator} for an application and {@link Subject}.
     *
     * @param applicationName the name of the application
     * @param subject         the {@link Subject}
     * @return a newly created {@link Evaluator} for the given name and subject
     */
    public Evaluator getEvaluatorForSubject(String applicationName, Subject subject) {
        return new Evaluator(subject, applicationName);
    }

    /**
     * Retrieve a {@link List} of {@link ScriptEntitlementInfo}s.
     *
     * @param applicationName the application name
     * @param realm           the realm
     * @param sessionToken    the session token
     * @param authnRequest    the {@link AuthnRequest}
     * @return the {@link List} of {@link ScriptEntitlementInfo}s
     * @throws EntitlementException if an error occurs when evaluating the entitlement information.
     */
    public List<ScriptEntitlementInfo> getEntitlements(String applicationName, String realm, Object sessionToken,
                                             AuthnRequest authnRequest) throws EntitlementException {
        Subject subject = getSubjectForToken(sessionToken);
        Set<String> resources = getResourcesForToken(authnRequest);
        return getEntitlements(applicationName, realm, subject, resources);
    }

    /**
     * Retrieve a {@link List} of {@link ScriptEntitlementInfo}s.
     *
     * @param applicationName the application name to retrieve the entitlement information for
     * @param realm           the realm to evaluate
     * @param subject         the {@link Subject} to retrieve the entitlement information for
     * @param resources       the Set of resources to evaluate
     * @return the {@link List} of {@link ScriptEntitlementInfo}s
     * @throws EntitlementException if an error occurs when evaluating the entitlement information.
     */
    public List<ScriptEntitlementInfo> getEntitlements(String applicationName, String realm, Subject subject,
                                             Set<String> resources) throws EntitlementException {
        return getEntitlements(applicationName, realm, subject, resources, Collections.emptyMap());
    }

    /**
     * Retrieve a {@link List} of {@link ScriptEntitlementInfo}s.
     *
     * @param applicationName the application name to retrieve the entitlement information for
     * @param realm           the realm to evaluate
     * @param subject         the {@link Subject} to retrieve the entitlement information for
     * @param resources       the Set of resources to evaluate
     * @param environment     the environment parameters used in the evaluation
     * @return the {@link List} of {@link ScriptEntitlementInfo}s
     * @throws EntitlementException if an error occurs when evaluating the entitlement information.
     */
    public List<ScriptEntitlementInfo> getEntitlements(String applicationName, String realm, Subject subject,
                                             Set<String> resources, Map<String, Set<String>> environment)
            throws EntitlementException {
        Evaluator evaluator = getEvaluatorForSubject(applicationName, subject);
        return evaluator.evaluate(realm, subject, resources, environment).stream()
                .map(ScriptEntitlementInfo::new).collect(Collectors.toList());
    }

}
