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
package org.forgerock.openam.auth.node.api;

import java.util.Map;

import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.core.realms.Realm;

/**
 * Script API wrapper for LocalizedMessageProvider to expose to scripts.
 */
@Supported(scriptingApi = true, javaApi = false)
public interface ScriptedLocalizedMessage {

    /**
     * Get the localized message based on the provided localizations.
     * @param localizations map of locale keys to localized messages.
     * @return the localized message.
     */
    @Supported(scriptingApi = true, javaApi = false)
    String getLocalizedMessage(Map<String, String> localizations);

    /**
     * Factory for creating ScriptedLocalizedMessage instances.
     */
    interface Factory {

        /**
         * Create a new ScriptedLocalizedMessage.
         * @param realm the realm.
         * @param context the tree context.
         * @return the scripted localized message.
         */
        ScriptedLocalizedMessage create(Realm realm, TreeContext context);
    }
}
