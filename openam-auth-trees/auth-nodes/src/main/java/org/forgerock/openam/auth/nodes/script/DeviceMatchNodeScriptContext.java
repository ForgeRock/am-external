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

package org.forgerock.openam.auth.nodes.script;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PSource;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.domain.nextgen.NextGenScriptContext;
import org.forgerock.openam.scripting.persistence.config.defaults.GlobalScriptsProvider;
import org.forgerock.util.promise.NeverThrowsException;
import org.slf4j.Logger;

import com.google.auto.service.AutoService;
import com.google.inject.Singleton;

/**
 * Definition of {@link ScriptContext} for device match nodes.
 */
@Singleton
@AutoService({ScriptContext.class, GlobalScriptsProvider.class})
public class DeviceMatchNodeScriptContext extends NextGenScriptContext<DeviceMatchNodeBindings> {

    /**
     * The name of the device match node script context.
     */
    public static final String DEVICE_MATCH_NODE_NAME = "DEVICE_MATCH_NODE";
    /**
     * The default script ID of the device match node script context.
     */
    public static final String DEVICE_MATCH_NODE_DEFAULT_SCRIPT_ID = "11e1a3c0-038b-4c16-956a-6c9d89328d00";

    private final Script defaultScript;

    /**
     * Constructs a new instance of the device match node context.
     * Do not call this constructor directly, use Guice to inject the Singleton instance of this class.
     */
    public DeviceMatchNodeScriptContext() throws ScriptException {
        super(DeviceMatchNodeBindings.class);
        this.defaultScript = createDefaultScript();
    }

    @Override
    protected Script getDefaultScript() {
        return defaultScript;
    }

    @Override
    protected List<String> getContextWhiteList() {
        return List.of(
                Byte.class.getName(),
                Character.class.getName(),
                Character.Subset.class.getName(),
                Character.UnicodeBlock.class.getName(),
                Float.class.getName(),
                Long.class.getName(),
                Math.class.getName(),
                Number.class.getName(),
                Short.class.getName(),
                StrictMath.class.getName(),
                Void.class.getName(),
                anyInnerClassOf(AbstractMap.class),
                ArrayList.class.getName(),
                Collections.class.getName(),
                TimeUnit.class.getName(),
                anyInnerClassOf(Collections.class),
                HashSet.class.getName(),
                innerClass(HashMap.class, "KeyIterator"),
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
                SecretKeyFactory.class.getName(),
                OAEPParameterSpec.class.getName(),
                PBEKeySpec.class.getName(),
                PSource.class.getName(),
                anyInnerClassOf(PSource.class),
                JsonValue.class.getName(),
                NeverThrowsException.class.getName(),
                ExecutionException.class.getName(),
                TimeoutException.class.getName(),
                // Using a code reference to the callbackhandlers package creates a circular dependency
                "org.forgerock.openam.core.rest.authn.callbackhandlers.*",
                "com.sun.crypto.provider.PBKDF2KeyImpl", // Cannot access internal JDK classes by name
                "org.forgerock.openam.scripting.api.PrefixedScriptPropertyResolver", // located in openam-scripting
                innerClass(Collections.class, "UnmodifiableRandomAccessList"),
                innerClass(Collections.class, "UnmodifiableCollection$1"),
                "sun.security.ec.ECPrivateKeyImpl", // This module isn't exported from the JDK
                Logger.class.getName(),
                "com.sun.proxy.$*",
                Date.class.getName(),
                InvalidKeySpecException.class.getName(),
                "org.forgerock.openam.auth.nodes.VerifyTransactionsHelper" // located in pingone-nodes
        );
    }

    @Override
    public DeviceMatchNodeBindings getExampleBindings() {
        return DeviceMatchNodeBindings.builder()
                .withDeviceProfilesDao(null)
                .withSharedState(null)
                .withTransientState(null)
                .withNodeState(null)
                .withCallbacks(new ArrayList<>())
                .withHeaders(new HashMap<>())
                .withQueryParameters(new HashMap<>())
                .withScriptedIdentityRepository(null)
                .withResumedFromSuspend(false)
                .withExistingSession(null)
                .withRequestCookies(new HashMap<>())
                .build();
    }

    @Override
    public String name() {
        return DEVICE_MATCH_NODE_NAME;
    }

    @Override
    public String getI18NKey() {
        return "next-gen-script-type-02";
    }

    private Script createDefaultScript() throws ScriptException {
        return Script.defaultScriptBuilder()
                .setId(DEVICE_MATCH_NODE_DEFAULT_SCRIPT_ID)
                .setName("Next Generation Device Match Node Script")
                .setDescription("Default global script for a device match node")
                .setContext(this)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setEvaluatorVersion(EvaluatorVersion.V2_0)
                .setScript(loadScript("scripts/next-gen-deviceProfileMatch-decision-node.js"))
                .build();
    }
}
