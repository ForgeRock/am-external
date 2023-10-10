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
import static org.forgerock.openam.scripting.domain.EvaluatorVersion.V1_0;
import static org.forgerock.openam.scripting.domain.EvaluatorVersion.V2_0;
import static org.forgerock.openam.scripting.domain.ScriptingLanguage.GROOVY;
import static org.forgerock.openam.scripting.domain.ScriptingLanguage.JAVASCRIPT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;
import org.forgerock.openam.scripting.domain.EvaluatorVersionAllowList;
import org.forgerock.openam.scripting.domain.ScriptContextDetails;
import org.forgerock.openam.scripting.persistence.config.defaults.AnnotatedServiceRegistryScriptContextDetailsProvider;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;
import org.forgerock.util.promise.Promises;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.JavaScriptException;

import com.google.inject.Inject;

import ch.qos.logback.classic.Logger;

/**
 * Responsible for providing the auth node script contexts.
 */
public class AuthNodesScriptContextProvider extends AnnotatedServiceRegistryScriptContextDetailsProvider {

    @Inject
    AuthNodesScriptContextProvider(AnnotatedServiceRegistry annotatedServiceRegistry) {
        super(annotatedServiceRegistry);
    }

    private List<String> whiteListV1() {
        return List.of(
                "java.lang.Boolean", "java.lang.Byte", "java.lang.Character",
                "java.lang.Character$Subset", "java.lang.Character$UnicodeBlock", "java.lang.Double",
                "java.lang.Float", "java.lang.Integer", "java.lang.Long", "java.lang.Math", "java.lang.Number",
                "java.lang.Object", "java.lang.Short", "java.lang.StrictMath", "java.lang.String",
                "java.lang.Void", "java.util.AbstractMap$*", "java.util.ArrayList", "java.util.Collections",
                "java.util.Collections$*", "java.util.concurrent.TimeUnit", "java.util.concurrent.ExecutionException",
                "java.util.concurrent.TimeoutException", "java.util.HashSet", "java.util.HashMap",
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
                "sun.security.ec.ECPrivateKeyImpl",
                "org.forgerock.openam.authentication.callbacks.BooleanAttributeInputCallback",
                "org.forgerock.openam.authentication.callbacks.NumberAttributeInputCallback",
                "org.forgerock.openam.authentication.callbacks.StringAttributeInputCallback"
        );
    }

    private List<String> whiteListV2() {
        return List.of(
                Boolean.class.getName(),
                Byte.class.getName(),
                Character.class.getName(),
                Character.Subset.class.getName(),
                Character.UnicodeBlock.class.getName(),
                Double.class.getName(),
                Float.class.getName(),
                Integer.class.getName(),
                Long.class.getName(),
                Math.class.getName(),
                Number.class.getName(),
                Object.class.getName(),
                Short.class.getName(),
                StrictMath.class.getName(),
                String.class.getName(),
                Void.class.getName(),
                anyInnerClassOf(AbstractMap.class),
                ArrayList.class.getName(),
                Collections.class.getName(),
                TimeUnit.class.getName(),
                anyInnerClassOf(Collections.class),
                HashSet.class.getName(),
                privateInnerClass(HashMap.class, "KeyIterator"),
                LinkedHashSet.class.getName(),
                LinkedList.class.getName(),
                TreeSet.class.getName(),
                KeyPair.class.getName(),
                KeyPairGenerator.class.getName(),
                anyInnerClassOf(KeyPairGenerator.class),
                PrivateKey.class.getName(),
                PublicKey.class.getName(),
                X509EncodedKeySpec.class.getName(),
                MGF1ParameterSpec.class.getName(),
                OAEPParameterSpec.class.getName(),
                PSource.class.getName(),
                anyInnerClassOf(PSource.class),
                JsonValue.class.getName(),
                NeverThrowsException.class.getName(),
                Promise.class.getName(),
                ExecutionException.class.getName(),
                TimeoutException.class.getName(),
                PromiseImpl.class.getName(),
                // Using a code reference to the callbackhandlers package creates a circular dependency
                "org.forgerock.openam.core.rest.authn.callbackhandlers.*",
                DeviceProfilesDao.class.getName(),
                "org.forgerock.openam.scripting.api.PrefixedScriptPropertyResolver", // located in openam-scripting
                List.class.getName(),
                ConsString.class.getName(),
                privateInnerClass(Collections.class, "UnmodifiableRandomAccessList"),
                privateInnerClass(Collections.class, "UnmodifiableCollection$1"),
                JavaScriptException.class.getName(),
                "sun.security.ec.ECPrivateKeyImpl", // This module isn't exported from the JDK
                Logger.class.getName(),
                anyInnerClassOf(Promises.class),
                "com.sun.proxy.$*",
                Date.class.getName()
        );
    }

    @Override
    public List<ScriptContextDetails> get() {
        List<ScriptContextDetails> scriptContexts = new ArrayList<>();

        scriptContexts.add(ScriptContextDetails.builder()
                .withContextReference(AUTHENTICATION_TREE_DECISION_NODE)
                .withI18NKey("script-type-05")
                .withDefaultScript(DECISION_NODE_SCRIPT.getId())
                .withBindings(ScriptedDecisionNodeBindings.signature(), DeviceMatchNodeBindings.signature())
                .withWhiteList(EvaluatorVersionAllowList.builder()
                        .v1AllowList(whiteListV1())
                        .v2AllowList(whiteListV2())
                        .build())
                .withEvaluatorVersions(Map.of(
                        GROOVY, EnumSet.of(V1_0),
                        JAVASCRIPT, EnumSet.of(V1_0, V2_0))
                )
                .build());

        scriptContexts.add(ScriptContextDetails.builder()
                .withContextReference(CONFIG_PROVIDER_NODE)
                .withI18NKey("script-type-14")
                .withDefaultScript(CONFIG_PROVIDER_NODE_SCRIPT.getId())
                .withBindings(ConfigProviderNodeBindings.signature())
                .overrideDefaultWhiteList(whiteListV1().toArray(new String[0])).build());

        return scriptContexts;
    }

    private String anyInnerClassOf(Class clazz) {
        return clazz.getName() + "$*";
    }

    private String anyClassFromPackageContaining(Class clazz) {
        return clazz.getPackageName() + ".*";
    }

    private String privateInnerClass(Class clazz, String innerClassName) {
        return clazz.getName() + "$" + innerClassName;
    }
}
