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
 * Copyright 2021-2023 ForgeRock AS.
 */

package com.sun.identity.saml2.plugins.scripted;

import static java.util.Collections.emptyList;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ATTRIBUTE_MAPPER;

import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptBindings;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;

import com.google.inject.Inject;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.IDPAttributeMapper;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpAttributeMapperBindings;
import com.sun.identity.saml2.profile.IDPSSOUtil;

/**
 * Scripted implementation of the IDPAttributeMapper
 */
public class ScriptedIdpAttributeMapper implements IDPAttributeMapper {

    private final ScriptEvaluator scriptEvaluator;
    private final ScriptStoreFactory scriptStoreFactory;
    private final ScriptEvaluationHelper scriptEvaluationHelper;
    private final ValidationHelper validationHelper;
    private final RealmLookup realmLookup;

    @Inject
    public ScriptedIdpAttributeMapper(ScriptEvaluatorFactory scriptEvaluatorFactory,
            ScriptStoreFactory scriptStoreFactory, ScriptEvaluationHelper scriptEvaluationHelper,

            ValidationHelper validationHelper, RealmLookup realmLookup) {
        this.scriptEvaluator = scriptEvaluatorFactory.create(SAML2_IDP_ATTRIBUTE_MAPPER);
        this.scriptStoreFactory = scriptStoreFactory;
        this.scriptEvaluationHelper = scriptEvaluationHelper;
        this.validationHelper = validationHelper;
        this.realmLookup = realmLookup;
    }

    @Override
    public List<Attribute> getAttributes(Object session, String hostedEntityId, String remoteEntityId, String realm)
            throws SAML2Exception {
        validationHelper.validateRealm(realm);
        validationHelper.validateHostedEntity(hostedEntityId);
        validationHelper.validateSession(session);

        try {
            Script script = getScript(realm, hostedEntityId);

            ScriptBindings scriptBindings = SamlIdpAttributeMapperBindings.builder()
                    .withHostedEntityId(hostedEntityId)
                    .withIdpAttributeMapperScriptHelper(new IdpAttributeMapperScriptHelper())
                    .withRealm(realm)
                    .withRemoteEntityId(remoteEntityId)
                    .withSession(session)
                    .withLoggerReference(String.format("scripts.%s.%s.(%s)", SAML2_IDP_ATTRIBUTE_MAPPER.name(),
                            script.getId(), script.getName()))
                    .withScriptName(script.getName())
                    .build();

            Bindings scriptVariables = scriptBindings.convert(script.getEvaluatorVersion());

            return scriptEvaluationHelper.evaluateScript(scriptEvaluator, script, scriptVariables, List.class,
                    realmLookup.lookup(realm)).orElse(emptyList());
        } catch (ScriptException | org.forgerock.openam.scripting.domain.ScriptException | RealmLookupException e) {
            throw new SAML2Exception(e);
        }
    }

    private Script getScript(String realm, String hostedEntityID)
            throws org.forgerock.openam.scripting.domain.ScriptException {
        String idpAttributeMapperScript = IDPSSOUtil.getAttributeValueFromIDPSSOConfig(realm, hostedEntityID,
                SAML2Constants.IDP_ATTRIBUTE_MAPPER_SCRIPT);
        try {
            return scriptStoreFactory.create(realmLookup.lookup(realm)).get(idpAttributeMapperScript);
        } catch (RealmLookupException e) {
            throw new IllegalArgumentException("Cannot find realm " + realm, e);
        }
    }
}
