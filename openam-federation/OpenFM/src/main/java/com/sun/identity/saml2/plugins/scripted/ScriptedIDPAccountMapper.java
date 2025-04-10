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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import javax.inject.Inject;
import javax.script.ScriptException;

import org.apache.commons.lang.NotImplementedException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.saml2.service.Saml2NameIdMapperContext;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.plugins.IDPAccountMapper;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlNameIdMapperBindings;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;

/**
 * Executes the NameID Mapper script if configured.
 */
public class ScriptedIDPAccountMapper implements IDPAccountMapper {

    private final ScriptEvaluator<SamlNameIdMapperBindings> scriptEvaluator;
    private final ScriptStoreFactory scriptStoreFactory;
    private final ScriptEvaluationHelper<SamlNameIdMapperBindings> scriptEvaluationHelper;
    private final ValidationHelper validationHelper;
    private final RealmLookup realmLookup;

    @Inject
    public ScriptedIDPAccountMapper(ScriptEvaluatorFactory evaluatorFactory, ScriptStoreFactory scriptStoreFactory,
            ValidationHelper validationHelper, RealmLookup realmLookup,
            Saml2NameIdMapperContext saml2NameIdMapperContext) {
        this.scriptEvaluator = evaluatorFactory.create(saml2NameIdMapperContext);
        this.scriptStoreFactory = scriptStoreFactory;
        this.scriptEvaluationHelper = scriptEvaluator.getScriptEvaluationHelper();
        this.validationHelper = validationHelper;
        this.realmLookup = realmLookup;
    }

    @Override
    public NameID getNameID(Object session, String hostEntityID, String remoteEntityID, String realm,
            String nameIDFormat) throws SAML2Exception {
        validationHelper.validateRealm(realm);
        validationHelper.validateRemoteEntity(remoteEntityID);

        try {
            Script script = getScript(realm, remoteEntityID);
            SamlNameIdMapperBindings scriptBindings = SamlNameIdMapperBindings.builder()
                    .withRemoteEntityId(remoteEntityID)
                    .withNameIDFormat(nameIDFormat)
                    .withSession(session)
                    .withNameIdScriptHelper(new NameIDScriptHelper(session, hostEntityID,
                            remoteEntityID, realm, nameIDFormat))
                    .withHostedEntityId(hostEntityID)
                    .build();

            String nameIDValue = scriptEvaluationHelper.evaluateScript(script, scriptBindings,
                    String.class, getRealm(realm)).orElseThrow(() -> new SAML2Exception("Could not generate NameID"));
            NameID nameID = AssertionFactory.getInstance().createNameID();
            nameID.setValue(nameIDValue);
            nameID.setFormat(nameIDFormat);
            nameID.setNameQualifier(hostEntityID);
            nameID.setSPNameQualifier(remoteEntityID);
            nameID.setSPProvidedID(null);
            return nameID;
        } catch (ScriptException | org.forgerock.openam.scripting.domain.ScriptException e) {
            throw new SAML2Exception(e);
        }
    }

    @Override
    public String getIdentity(ManageNameIDRequest manageNameIDRequest, String hostEntityID, String realm)
            throws SAML2Exception {
        throw new NotImplementedException();
    }

    @Override
    public String getIdentity(NameID nameID, String hostEntityID, String remoteEntityID, String realm) throws SAML2Exception {
        throw new NotImplementedException();
    }

    @Override
    public boolean shouldPersistNameIDFormat(String realm, String hostEntityID, String remoteEntityID, String nameIDFormat) {
        throw new NotImplementedException();
    }

    private Script getScript(String realm, String remoteEntityId)
            throws org.forgerock.openam.scripting.domain.ScriptException, SAML2Exception {
        String nameIdMappingScript = SAML2Utils.getAttributeValueFromSSOConfig(realm, remoteEntityId,
                SAML2Constants.SP_ROLE, SAML2Constants.NAMEID_MAPPER_SCRIPT);
        return scriptStoreFactory.create(getRealm(realm)).get(nameIdMappingScript);
    }

    private Realm getRealm(String realm) throws SAML2Exception {
        try {
            return realmLookup.lookup(realm);
        } catch (RealmLookupException e) {
            throw new SAML2Exception(e);
        }
    }
}
