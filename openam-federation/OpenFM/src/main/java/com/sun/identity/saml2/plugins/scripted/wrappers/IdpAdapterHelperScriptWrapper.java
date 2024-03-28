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
 * Copyright 2023 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins.scripted.wrappers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import com.sun.identity.entitlement.Evaluator;
import org.forgerock.openam.annotations.SupportedAll;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.saml2.plugins.scripted.IdpAdapterScriptHelper;
import com.sun.identity.saml2.plugins.scripted.wrappers.ScriptEntitlementInfoWrapper;
import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * Provides helper functions for IDP Adapter Script Implementations.
 */
@SupportedAll(scriptingApi = true, javaApi = false)
public class IdpAdapterHelperScriptWrapper {

    private final IdpAdapterScriptHelper idpAdapterScriptHelper;

    public IdpAdapterHelperScriptWrapper(IdpAdapterScriptHelper idpAdapterScriptHelper) {
        this.idpAdapterScriptHelper = idpAdapterScriptHelper;
    }

    /**
     * Retrieve the issuer name for the AuthN request.
     *
     * @param authnRequest the AuthN request
     * @return the issuer name
     */
    public String getIssuerName(AuthnRequest authnRequest) {
        return idpAdapterScriptHelper.getIssuerName(authnRequest);
    }

    /**
     * Retrieve the issuer name from the AuthN request, within a Singleton Set.
     *
     * @param authnRequest the {@link AuthnRequest}
     * @return the resources for the AuthN token
     */
    public List<String> getResourcesForToken(AuthnRequest authnRequest) {
        return List.copyOf(idpAdapterScriptHelper.getResourcesForToken(authnRequest));
    }

    /**
     * Retrieve {@link Subject} for an {@link SSOToken}.
     *
     * @param sessionToken the SSO Token of type Object
     * @return the {@link Subject} associated with the SSO Token
     */
    public Subject getSubjectForToken(Object sessionToken) {
        return idpAdapterScriptHelper.getSubjectForToken(sessionToken);
    }

    /**
     * Retrieve {@link Subject} for an {@link SSOToken}.
     *
     * @param sessionToken the {@link SSOToken}
     * @return the {@link Subject} associated with the SSO Token
     */
    public Subject getSubjectForToken(SSOToken sessionToken) {
        return idpAdapterScriptHelper.getSubjectForToken(sessionToken);
    }

    /**
     * Retrieve a {@link List} of {@link ScriptEntitlementInfoWrapper}s.
     *
     * @param applicationName the application name
     * @param realm           the realm
     * @param sessionToken    the session token
     * @param authnRequest    the {@link AuthnRequest}
     * @return the {@link List} of {@link ScriptEntitlementInfoWrapper}s
     * @throws EntitlementException if an error occurs when evaluating the entitlement information.
     */
    public List<ScriptEntitlementInfoWrapper> getEntitlements(String applicationName, String realm, Object sessionToken,
                                             AuthnRequest authnRequest) throws EntitlementException {
        Subject subject = getSubjectForToken(sessionToken);
        List<String> resources = getResourcesForToken(authnRequest);
        return getEntitlements(applicationName, realm, subject, resources);
    }

    /**
     * Retrieve a {@link List} of {@link ScriptEntitlementInfoWrapper}s.
     *
     * @param applicationName the application name to retrieve the entitlement information for
     * @param realm           the realm to evaluate
     * @param subject         the {@link Subject} to retrieve the entitlement information for
     * @param resources       the Set of resources to evaluate
     * @return the {@link List} of {@link ScriptEntitlementInfoWrapper}s
     * @throws EntitlementException if an error occurs when evaluating the entitlement information.
     */
    public List<ScriptEntitlementInfoWrapper> getEntitlements(String applicationName, String realm, Subject subject,
                                             List<String> resources) throws EntitlementException {
        return getEntitlements(applicationName, realm, subject, resources, Collections.emptyMap());
    }

    /**
     * Retrieve a {@link List} of {@link ScriptEntitlementInfoWrapper}s.
     *
     * @param applicationName the application name to retrieve the entitlement information for
     * @param realm           the realm to evaluate
     * @param subject         the {@link Subject} to retrieve the entitlement information for
     * @param resources       the Set of resources to evaluate
     * @param environment     the environment parameters used in the evaluation
     * @return the {@link List} of {@link ScriptEntitlementInfoWrapper}s
     * @throws EntitlementException if an error occurs when evaluating the entitlement information.
     */
    public List<ScriptEntitlementInfoWrapper> getEntitlements(String applicationName, String realm, Subject subject,
                                             List<String> resources, Map<String, List<String>> environment)
            throws EntitlementException {
        Evaluator evaluator = idpAdapterScriptHelper.getEvaluatorForSubject(applicationName, subject);
        return evaluator.evaluate(realm, subject, Set.copyOf(resources), convertToMapOfSets(environment)).stream()
                .map(ScriptEntitlementInfoWrapper::new).collect(Collectors.toList());
    }

    private Map<String, Set<String>> convertToMapOfSets(Map<String, List<String>> attributes) {
        return attributes
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Set.copyOf(entry.getValue())
                ));
    }
}
