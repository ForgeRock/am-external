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
package com.sun.identity.saml2.plugins.scripted;

import javax.script.Bindings;

import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;
import org.forgerock.util.Reject;

import com.google.inject.Inject;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;

/**
 * Class that holds common script execution routines.
 */
class ScriptExecutor {

    private final ScriptStoreFactory scriptStoreFactory;
    private final RealmLookup realmLookup;
    private final ScriptEvaluationHelper scriptEvaluationHelper;


    @Inject
    public ScriptExecutor(ScriptStoreFactory scriptStoreFactory, RealmLookup realmLookup,
            ScriptEvaluationHelper scriptEvaluationHelper) {
        this.scriptStoreFactory = scriptStoreFactory;
        this.realmLookup = realmLookup;
        this.scriptEvaluationHelper = scriptEvaluationHelper;
    }

    void evaluateVoidScriptFunction(ScriptEvaluator scriptEvaluator, Script script, String realm,
            Bindings scriptVariables, String functionName) throws SAML2Exception {
        Reject.ifNull(script);
        try {
            scriptEvaluationHelper.evaluateFunction(scriptEvaluator, script, scriptVariables, functionName,
                    realmLookup.lookup(realm));
        } catch (javax.script.ScriptException | RealmLookupException e) {
            throw new SAML2Exception(e);
        }
    }

    boolean evaluateScriptFunction(ScriptEvaluator scriptEvaluator, Script script, String realm,
            Bindings scriptVariables, String functionName) throws SAML2Exception {
        Reject.ifNull(script);
        try {
            return scriptEvaluationHelper.evaluateFunction(scriptEvaluator, script, scriptVariables, Boolean.class,
                    functionName, realmLookup.lookup(realm)).orElse(false);
        } catch (javax.script.ScriptException | RealmLookupException e) {
            throw new SAML2Exception(e);
        }
    }

    Script getScriptConfiguration(String realm, String hostedEntityID, String role, String attribute)
            throws SAML2Exception {
        String script = SAML2Utils.getAttributeValueFromSSOConfig(realm, hostedEntityID, role, attribute);
        try {
            return scriptStoreFactory.create(realmLookup.lookup(realm)).get(script);
        } catch (RealmLookupException e) {
            throw new IllegalArgumentException("Cannot find realm " + realm, e);
        } catch (ScriptException e) {
            throw new SAML2Exception(e);
        }
    }
}
