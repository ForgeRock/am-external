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
public class SAMLScriptedBindingObjectImpl implements SAMLScriptedBindingObject {

    private final String applicationId;
    private final FlowInitiator flowInitiator;
    private final AuthnRequest authnRequest;
    private final Map<String, List<String>> idpAttributes;
    private final Map<String, List<String>> spAttributes;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SAMLScriptedBindingObjectImpl(
            @JsonProperty("applicationId") String applicationId,
            @JsonProperty("flowInitiator") FlowInitiator flowInitiator,
            @JsonProperty("authnRequest") AuthnRequest authnRequest,
            @JsonProperty("idpAttributes") Map<String, List<String>> idpAttributes,
            @JsonProperty("spAttributes") Map<String, List<String>> spAttributes
    ) {
        this.applicationId = applicationId;
        this.flowInitiator = flowInitiator;
        this.authnRequest = authnRequest;
        this.idpAttributes = idpAttributes;
        this.spAttributes = spAttributes;
    }

    public static SAMLScriptedBindingObject getExampleObject() {
        return new SAMLScriptedBindingObjectImpl(
                "applicationId", FlowInitiator.IDP, new AuthnRequestImpl(), new HashMap<>(), new HashMap<>()
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
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(authnRequest, new TypeReference<>() {
        });
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
}
