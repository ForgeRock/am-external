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
 * Copyright 2021-2026 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import static java.util.Collections.emptyList;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ATTRIBUTE_MAPPER;
import static org.forgerock.util.LambdaExceptionUtils.rethrowFunction;

import java.util.List;

import javax.script.ScriptException;

import org.forgerock.http.Client;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.saml2.service.Saml2IdpAttributeMapperContext;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.IDPAttributeMapper;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpAttributeMapperBindings;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpAttributeMapperNextGenBindings;
import com.sun.identity.saml2.plugins.scripted.wrappers.ScriptedSaml2Attribute;
import com.sun.identity.saml2.profile.IDPSSOUtil;

import jakarta.inject.Inject;

/**
 * Scripted implementation of the IDPAttributeMapper
 */
public class ScriptedIdpAttributeMapper implements IDPAttributeMapper {

    private static final Logger logger = LoggerFactory.getLogger(ScriptedIdpAttributeMapper.class);
    
    private final ObjectMapper jackson = new ObjectMapper();
    private final ScriptEvaluationHelper<LegacyScriptBindings> legacyScriptEvaluationHelper;
    private final ScriptEvaluationHelper<SamlIdpAttributeMapperNextGenBindings> nextGenScriptEvaluationHelper;
    private final ScriptStoreFactory scriptStoreFactory;
    private final ValidationHelper validationHelper;
    private final RealmLookup realmLookup;
    private final Client httpClient;

    @Inject
    public ScriptedIdpAttributeMapper(
            ScriptEvaluatorFactory scriptEvaluatorFactory,
            ScriptStoreFactory scriptStoreFactory,
            ValidationHelper validationHelper, RealmLookup realmLookup, Client httpClient,
            Saml2IdpAttributeMapperContext saml2IdpAttributeMapperContext
    ) {
        this.legacyScriptEvaluationHelper = scriptEvaluatorFactory.create(SAML2_IDP_ATTRIBUTE_MAPPER)
                .getScriptEvaluationHelper();
        this.nextGenScriptEvaluationHelper = scriptEvaluatorFactory.create(saml2IdpAttributeMapperContext)
                .getScriptEvaluationHelper();
        this.scriptStoreFactory = scriptStoreFactory;
        this.validationHelper = validationHelper;
        this.realmLookup = realmLookup;
        this.httpClient = httpClient;
    }

    @Override
    public List<Attribute> getAttributes(Object session, String hostedEntityId, String remoteEntityId, String realm)
            throws SAML2Exception {
        validationHelper.validateRealm(realm);
        validationHelper.validateHostedEntity(hostedEntityId);
        validationHelper.validateSession(session);

        try {
            Script script = getScript(realm, hostedEntityId);

            switch (script.getEvaluatorVersion()) {
            case V1_0:
                SamlIdpAttributeMapperBindings legacyBindings = SamlIdpAttributeMapperBindings.builder()
                        .withHostedEntityId(hostedEntityId)
                        .withIdpAttributeMapperScriptHelper(new IdpAttributeMapperScriptHelper())
                        .withRemoteEntityId(remoteEntityId)
                        .withSession(session)
                        .withHttpClient(httpClient)
                        .build();
                return legacyScriptEvaluationHelper
                        .evaluateScript(script, legacyBindings, List.class, getRealm(realm))
                        .orElse(emptyList());
            case V2_0:
                SamlIdpAttributeMapperNextGenBindings nextGenBindings = SamlIdpAttributeMapperNextGenBindings.builder()
                        .withSsoToken(session)
                        .withRealm(realm)
                        .withRemoteEntityId(remoteEntityId)
                        .withHostedEntityId(hostedEntityId)
                        .build();
                Object scriptResults = nextGenScriptEvaluationHelper
                        .evaluateScript(script, nextGenBindings, List.class, getRealm(realm))
                        .orElse(emptyList());

                try {
                    List<ScriptedSaml2Attribute> scriptedSaml2Attributes = jackson.convertValue(
                            scriptResults,
                            new TypeReference<>() {
                            }
                    );
                    return scriptedSaml2Attributes
                            .stream()
                            .map(rethrowFunction(ScriptedSaml2Attribute::toAttribute))
                            .toList();
                } catch (IllegalArgumentException e) {
                    throw new SAML2Exception("Error converting script results to ScriptedSaml2Attribute list", e);
                }

            default:
                throw new SAML2Exception("Unsupported script evaluator version: " + script.getEvaluatorVersion());
            }
        } catch (ScriptException | org.forgerock.openam.scripting.domain.ScriptException e) {
            throw new SAML2Exception(e);
        }
    }

    private Script getScript(String realm, String hostedEntityID)
            throws org.forgerock.openam.scripting.domain.ScriptException {
        String idpAttributeMapperScript = IDPSSOUtil.getAttributeValueFromIDPSSOConfig(realm, hostedEntityID,
                SAML2Constants.IDP_ATTRIBUTE_MAPPER_SCRIPT);
        return scriptStoreFactory.create(getRealm(realm)).get(idpAttributeMapperScript);
    }

    private Realm getRealm(String realm) {
        try {
            return realmLookup.lookup(realm);
        } catch (RealmLookupException e) {
            throw new IllegalArgumentException("Cannot find realm " + realm, e);
        }
    }
}
