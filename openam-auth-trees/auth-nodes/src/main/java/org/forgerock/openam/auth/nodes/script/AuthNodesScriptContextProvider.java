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

package org.forgerock.openam.auth.nodes.script;

import static org.forgerock.openam.auth.nodes.script.AuthNodesGlobalScript.CONFIG_PROVIDER_NODE_SCRIPT;
import static org.forgerock.openam.auth.nodes.script.AuthNodesGlobalScript.DECISION_NODE_SCRIPT;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.AUTHENTICATION_TREE_DECISION_NODE;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.CONFIG_PROVIDER_NODE;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.scripting.domain.ScriptContextDetails;
import org.forgerock.openam.scripting.persistence.config.defaults.AnnotatedServiceRegistryScriptContextDetailsProvider;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;

import com.google.inject.Inject;

/**
 * Responsible for providing the auth node script contexts.
 */
public class AuthNodesScriptContextProvider extends AnnotatedServiceRegistryScriptContextDetailsProvider {

    private String[] whiteList = {
        "java.lang.Boolean", "java.lang.Byte", "java.lang.Character",
        "java.lang.Character$Subset", "java.lang.Character$UnicodeBlock", "java.lang.Double",
        "java.lang.Float", "java.lang.Integer", "java.lang.Long", "java.lang.Math", "java.lang.Number",
        "java.lang.Object", "java.lang.Short", "java.lang.StrictMath", "java.lang.String",
        "java.lang.Void", "java.util.AbstractMap$*", "java.util.ArrayList", "java.util.Collections",
        "java.util.Collections$*", "java.util.HashSet", "java.util.HashMap",
        "java.util.HashMap$KeyIterator", "java.util.LinkedHashMap", "java.util.LinkedHashSet",
        "java.util.LinkedList", "java.util.TreeMap", "java.util.TreeSet",
        "java.security.KeyPair", "java.security.KeyPairGenerator", "java.security.KeyPairGenerator$*",
        "java.security.PrivateKey", "java.security.PublicKey",
        "java.security.spec.X509EncodedKeySpec", "java.security.spec.MGF1ParameterSpec",
        "javax.crypto.spec.OAEPParameterSpec", "javax.crypto.spec.PSource",
        "javax.crypto.spec.PSource$*",
        "javax.security.auth.callback.NameCallback",
        "javax.security.auth.callback.PasswordCallback",
        "javax.security.auth.callback.ChoiceCallback",
        "javax.security.auth.callback.ConfirmationCallback",
        "javax.security.auth.callback.LanguageCallback",
        "javax.security.auth.callback.TextInputCallback",
        "javax.security.auth.callback.TextOutputCallback",
        "com.sun.identity.authentication.callbacks.HiddenValueCallback",
        "com.sun.identity.authentication.callbacks.ScriptTextOutputCallback",
        "com.sun.identity.authentication.spi.HttpCallback",
        "com.sun.identity.authentication.spi.MetadataCallback",
        "com.sun.identity.authentication.spi.RedirectCallback",
        "com.sun.identity.authentication.spi.X509CertificateCallback",
        "com.sun.identity.shared.debug.Debug", "org.codehaus.groovy.runtime.GStringImpl",
        "org.codehaus.groovy.runtime.ScriptBytecodeAdapter", "org.forgerock.http.client.*",
        "org.forgerock.http.Client", "org.forgerock.http.Handler", "org.forgerock.http.Context",
        "org.forgerock.http.context.RootContext", "org.forgerock.http.protocol.Cookie",
        "org.forgerock.http.header.*", "org.forgerock.http.header.authorization.*",
        "org.forgerock.http.protocol.Entity", "org.forgerock.http.protocol.Form",
        "org.forgerock.http.protocol.Header", "org.forgerock.http.protocol.Headers",
        "org.forgerock.http.protocol.Message", "org.forgerock.http.protocol.Request",
        "org.forgerock.http.protocol.RequestCookies", "org.forgerock.http.protocol.Response",
        "org.forgerock.http.protocol.ResponseException", "org.forgerock.http.protocol.Responses",
        "org.forgerock.http.protocol.Status", "org.forgerock.json.JsonValue",
        "org.forgerock.util.promise.NeverThrowsException", "org.forgerock.util.promise.Promise",
        "org.forgerock.util.promise.PromiseImpl", "org.forgerock.openam.auth.node.api.Action",
        "org.forgerock.openam.auth.node.api.Action$ActionBuilder",
        "org.forgerock.openam.authentication.callbacks.IdPCallback",
        "org.forgerock.openam.authentication.callbacks.PollingWaitCallback",
        "org.forgerock.openam.authentication.callbacks.ValidatedPasswordCallback",
        "org.forgerock.openam.authentication.callbacks.ValidatedUsernameCallback",
        "org.forgerock.openam.core.rest.authn.callbackhandlers.*",
        "org.forgerock.openam.scripting.api.http.GroovyHttpClient",
        "org.forgerock.openam.scripting.api.http.JavaScriptHttpClient",
        "org.forgerock.openam.scripting.api.identity.ScriptedIdentity",
        "org.forgerock.openam.scripting.api.ScriptedSession", "groovy.json.JsonSlurper",
        "org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao",
        "org.forgerock.openam.scripting.idrepo.ScriptIdentityRepository",
        "org.forgerock.openam.scripting.api.secrets.ScriptedSecrets",
        "org.forgerock.openam.scripting.api.secrets.Secret",
        "org.forgerock.openam.shared.security.crypto.CertificateService",
        "org.forgerock.openam.auth.node.api.NodeState",
        "org.forgerock.openam.scripting.api.PrefixedScriptPropertyResolver",
        "java.util.List", "java.util.Map", "org.mozilla.javascript.ConsString",
        "java.util.Collections$UnmodifiableRandomAccessList",
        "java.util.Collections$UnmodifiableCollection$1",
        "org.mozilla.javascript.JavaScriptException",
        "sun.security.ec.ECPrivateKeyImpl"
    };

    @Inject
    AuthNodesScriptContextProvider(AnnotatedServiceRegistry annotatedServiceRegistry) {
        super(annotatedServiceRegistry);
    }

    @Override
    public List<ScriptContextDetails> get() {
        List<ScriptContextDetails> scriptContexts = new ArrayList<>();

        scriptContexts.add(ScriptContextDetails.builder()
                .withContextReference(AUTHENTICATION_TREE_DECISION_NODE)
                .withI18NKey("script-type-05")
                .withDefaultScript(DECISION_NODE_SCRIPT.getId())
                .overrideDefaultWhiteList(whiteList).build());

        scriptContexts.add(ScriptContextDetails.builder()
                .withContextReference(CONFIG_PROVIDER_NODE)
                .withI18NKey("script-type-14")
                .withDefaultScript(CONFIG_PROVIDER_NODE_SCRIPT.getId())
                .overrideDefaultWhiteList(whiteList).build());

        return scriptContexts;
    }
}
