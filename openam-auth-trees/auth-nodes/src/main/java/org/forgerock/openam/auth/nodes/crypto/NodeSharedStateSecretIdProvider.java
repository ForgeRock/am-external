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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.crypto;

import static org.forgerock.openam.shared.secrets.Labels.NODE_SHARED_STATE_ENCRYPTION;

import org.forgerock.openam.secrets.SecretIdProvider;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * This provider exposes the secret IDs used by nodes, which require to protect their shared state payloads, to the
 * {@link org.forgerock.openam.secrets.config.SecretIdRegistry}.
 */
public class NodeSharedStateSecretIdProvider implements SecretIdProvider {

    @Override
    public Multimap<String, String> getGlobalSingletonSecretIds() {
        return ImmutableMultimap.<String, String>builder()
                .putAll("Node Shared State",
                        NODE_SHARED_STATE_ENCRYPTION)
                .build();
    }

}
