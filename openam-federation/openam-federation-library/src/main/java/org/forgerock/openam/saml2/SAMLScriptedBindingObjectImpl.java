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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.saml2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.impl.AssertionImpl;
import com.sun.identity.saml2.assertion.impl.SubjectImpl;
import com.sun.identity.saml2.profile.ResponseInfo;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.scripting.domain.SAMLScriptedBindingObject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.impl.AuthnRequestImpl;

/**
 * Object for binding into a decision node script, which holds information about an incoming SAML authentication
 * request.
 */
@Supported(scriptingApi = true, javaApi = false)
public class SAMLScriptedBindingObjectImpl implements SAMLData, SAMLScriptedBindingObject {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String applicationId;
    private final FlowInitiator flowInitiator;
    private final AuthnRequest authnRequest;
    private final Map<String, List<String>> idpAttributes;
    private final Map<String, List<String>> spAttributes;
    private Subject subject;
    private Assertion assertion;
    private String sessionIndex;
    private ResponseInfo responseInfo;

    /**
     * Create a new SAMLScriptedBindingObjectImpl with the values available on the IdP side of a SAML flow.
     * @param applicationId The application ID of the SAML2 service provider.
     * @param flowInitiator Either IDP or SP depending on where the flow was initiated.
     * @param authnRequest The {@link AuthnRequest} object.
     * @param idpAttributes The idp configuration attributes.
     * @param spAttributes The sp configuration attributes.
     */
    public SAMLScriptedBindingObjectImpl(
            String applicationId,
            FlowInitiator flowInitiator,
            AuthnRequest authnRequest,
            Map<String, List<String>> idpAttributes,
            Map<String, List<String>> spAttributes) {
        this.applicationId = applicationId;
        this.flowInitiator = flowInitiator;
        this.authnRequest = authnRequest;
        this.idpAttributes = idpAttributes;
        this.spAttributes = spAttributes;
    }

    /**
     * Create a new SAMLScriptedBindingObjectImpl with all values. Used by the JsonCreator to instantiate the object.
     * @param applicationId The application ID of the SAML2 service provider.
     * @param flowInitiator Either IDP or SP depending on where the flow was initiated.
     * @param authnRequest The {@link AuthnRequest} object.
     * @param idpAttributes The idp configuration attributes.
     * @param spAttributes The sp configuration attributes.
     * @param subject The {@link Subject} object.
     * @param assertion The {@link Assertion} object.
     * @param sessionIndex The session index.
     * @param responseInfo The {@link ResponseInfo} object.
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SAMLScriptedBindingObjectImpl(
            @JsonProperty("applicationId") String applicationId,
            @JsonProperty("flowInitiator") FlowInitiator flowInitiator,
            @JsonProperty("authnRequest") AuthnRequest authnRequest,
            @JsonProperty("idpAttributes") Map<String, List<String>> idpAttributes,
            @JsonProperty("spAttributes") Map<String, List<String>> spAttributes,
            @JsonProperty("subject") Subject subject,
            @JsonProperty("assertion") Assertion assertion,
            @JsonProperty("sessionIndex") String sessionIndex,
            @JsonProperty("responseInfo") ResponseInfo responseInfo
    ) {
        this(applicationId, flowInitiator, authnRequest, idpAttributes, spAttributes);
        this.subject = subject;
        this.assertion = assertion;
        this.sessionIndex = sessionIndex;
        this.responseInfo = responseInfo;
    }

    /**
     * Provides an example SAMLScriptedBindingObject.
     *
     * @return An example {@link SAMLScriptedBindingObject}.
     */
    public static SAMLScriptedBindingObject getExampleObject() {
        return new SAMLScriptedBindingObjectImpl(
                "applicationId", FlowInitiator.IDP, new AuthnRequestImpl(), new HashMap<>(), new HashMap<>(),
                new SubjectImpl(), new AssertionImpl(), "sessionIndex", new ResponseInfo()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Supported(scriptingApi = true, javaApi = false)
    @Override
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * {@inheritDoc}
     */
    @Supported(scriptingApi = true, javaApi = false)
    @Override
    public Map<String, Object> getAuthnRequest() {
        return MAPPER.convertValue(authnRequest, new TypeReference<>() {
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthnRequest getRawAuthnRequest() {
        return authnRequest;
    }

    /**
     * {@inheritDoc}
     */
    @Supported(scriptingApi = true, javaApi = false)
    @Override
    public Map<String, List<String>> getIdpAttributes() {
        return idpAttributes;
    }

    /**
     * {@inheritDoc}
     */
    @Supported(scriptingApi = true, javaApi = false)
    @Override
    public Map<String, List<String>> getSpAttributes() {
        return spAttributes;
    }

    /**
     * {@inheritDoc}
     */
    @Supported(scriptingApi = true, javaApi = false)
    @Override
    public String getFlowInitiator() {
        // Return a string rather than enum because even with the enum correctly annotated we still get:
        // 'Access to Java class "org.forgerock.openam.scripting.domain.SAMLScriptedBindingObject$FLowInitiator" is prohibited'
        // in the logs.  Maybe the script binding security doesn't like internal enums/classes?
        return flowInitiator.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subject getRawSubject() {
        return subject;
    }

    /**
     * {@inheritDoc}
     */
    @Supported(scriptingApi = true, javaApi = false)
    @Override
    public Map<String, Object> getAssertion() {
        if (assertion == null) {
            return null;
        }
        return MAPPER.convertValue(assertion, new TypeReference<>() {
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Assertion getRawAssertion() {
        return assertion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSessionIndex() {
        return sessionIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseInfo getRawResponseInfo() {
        return responseInfo;
    }
}
