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
 * Copyright 2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import javax.script.ScriptException;

import jakarta.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.forgerock.http.Client;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.saml2.service.Saml2AccountMapperScriptContext;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.plugins.AccountMapperPluginHelper;
import com.sun.identity.saml2.plugins.SPAccountMapper;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpAccountMapperBindings;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;

/**
 * Scripted implementation of the {@link SPAccountMapper}
 */
public class ScriptedSpAccountMapper implements SPAccountMapper {

    private final ScriptEvaluator<SamlSpAccountMapperBindings> scriptEvaluator;
    private final ScriptStoreFactory scriptStoreFactory;
    private final ScriptEvaluationHelper<SamlSpAccountMapperBindings> scriptEvaluationHelper;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ValidationHelper validationHelper;
    private final RealmLookup realmLookup;
    private final Client httpClient;
    private final AccountMapperPluginHelper pluginHelper;

    @Inject
    public ScriptedSpAccountMapper(ScriptEvaluatorFactory evaluatorFactory, ScriptStoreFactory scriptStoreFactory,
                                   ValidationHelper validationHelper, RealmLookup realmLookup,
                                   Saml2AccountMapperScriptContext samlScriptContext, Client httpClient) {
        this.scriptEvaluator = evaluatorFactory.create(samlScriptContext);
        this.scriptStoreFactory = scriptStoreFactory;
        this.scriptEvaluationHelper = scriptEvaluator.getScriptEvaluationHelper();
        this.validationHelper = validationHelper;
        this.realmLookup = realmLookup;
        this.httpClient = httpClient;
        this.pluginHelper = new AccountMapperPluginHelper();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity(Assertion assertion, String hostEntityID, String realm) throws SAML2Exception {
        validationHelper.validateRealm(realm);
        validationHelper.validateHostedEntity(hostEntityID);
        validationHelper.validateAssertion(assertion);

        try {
            Script script = getScript(realm, hostEntityID);
            SamlSpAccountMapperBindings scriptBindings = SamlSpAccountMapperBindings.builder()
                    .withAssertion(mapper.convertValue(assertion, new TypeReference<>(){}))
                    .withHostedEntityId(hostEntityID)
                    .withSpAccountMapperScriptHelper(new SpAccountMapperScriptHelper(assertion, hostEntityID, realm))
                    .build();

            return scriptEvaluationHelper.evaluateScript(script, scriptBindings, String.class, getRealm(realm))
                    .orElse(null);
        } catch (ScriptException | org.forgerock.openam.scripting.domain.ScriptException e) {
            throw new SAML2Exception(e);
        }
    }

    @Override
    public String getIdentity(ManageNameIDRequest manageNameIDRequest, String hostEntityID, String realm)
            throws SAML2Exception {
        throw new NotImplementedException("Code is not implemented");
    }

    @Override
    public boolean shouldPersistNameIDFormat(
            String realm, String hostEntityID, String remoteEntityID, String nameIDFormat) {
        return pluginHelper.shouldPersistNameIDFormat(realm, hostEntityID);
    }

    private Script getScript(String realm, String hostEntityId)
            throws org.forgerock.openam.scripting.domain.ScriptException, SAML2Exception {
        String accountMappingScript = SAML2Utils.getAttributeValueFromSSOConfig(realm, hostEntityId,
                SAML2Constants.SP_ROLE, SAML2Constants.SP_ACCOUNT_MAPPER_SCRIPT);
        return scriptStoreFactory.create(getRealm(realm)).get(accountMappingScript);
    }

    private Realm getRealm(String realm) throws SAML2Exception {
        try {
            return realmLookup.lookup(realm);
        } catch (RealmLookupException e) {
            throw new SAML2Exception(e);
        }
    }
}
