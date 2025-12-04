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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.forgerock.openam.annotations.sm.Config.Scope.REALM;
import static org.forgerock.openam.annotations.sm.Config.Scope.SERVICE;

import java.util.Collections;
import java.util.Set;

import org.forgerock.am.config.RealmConfiguration;
import org.forgerock.am.config.ServiceComponentConfig;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.annotations.sm.Config;
import org.forgerock.openam.session.SessionPropertyWhitelistConfig;

import com.google.auto.service.AutoService;

/**
 * Configuration for the WebAuthn Metadata Service.
 */
@Config(scope = SERVICE, name = "WebAuthnMetadataService", i18nFile = "webAuthnMetadataService",
        resourceName = "webAuthnMetadataService",
        descriptionKey = "webauthn-metadata-service-description")
@AutoService(ServiceComponentConfig.class)
public interface WebAuthnMetadataServiceConfig extends ServiceComponentConfig,
        RealmConfiguration<SessionPropertyWhitelistConfig.Realm> {
    /**
     * Realm config.
     */
    @Config(scope = REALM)
    interface Realm {

        /**
         * The set of FIDO Metadata Service URIs to process.
         *
         * @return The set of FIDO Metadata Service URIs
         */
        @Attribute(order = 100, i18nKey = "fidoMetadataServiceUris")
        default Set<String> fidoMetadataServiceUris() {
            return Collections.emptySet();
        }

        /**
         * Whether to enforce certificate revocation checking as part of the metadata service validation.
         *
         * @return true if revocation checking should be enforced, false otherwise.
         */
        @Attribute(order = 110, i18nKey = "enforceRevocationCheck")
        default boolean enforceRevocationCheck() {
            return false;
        }
    }
}
