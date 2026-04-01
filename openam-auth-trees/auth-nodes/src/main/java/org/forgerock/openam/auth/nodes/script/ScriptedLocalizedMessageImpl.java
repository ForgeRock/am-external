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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.script;

import java.util.Map;

import jakarta.inject.Inject;

import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.ScriptedLocalizedMessage;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.locale.Locale;

/**
 * Implementation of ScriptedLocalizedMessage.
 */
@Supported(javaApi = false, scriptingApi = true)
class ScriptedLocalizedMessageImpl implements ScriptedLocalizedMessage {

    private final LocalizedMessageProvider messageProvider;
    private final TreeContext context;

    /**
     * Constructor.
     * @param realm the realm.
     * @param context the tree context.
     * @param factory the localized message provider factory.
     */
    @Inject
    ScriptedLocalizedMessageImpl(@Assisted Realm realm, @Assisted TreeContext context,
            LocalizedMessageProvider.LocalizedMessageProviderFactory factory) {
        this.messageProvider = factory.create(realm);
        this.context = context;
    }

    @Override
    @Supported(javaApi = false, scriptingApi = true)
    public String getLocalizedMessage(Map<String, String> localizations) {
        var locales = localizations.keySet().stream()
                .map(key -> Map.entry(Locale.getLocale(key), localizations.get(key)))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return messageProvider.getLocalizedMessage(context, null, locales, null);
    }
}
