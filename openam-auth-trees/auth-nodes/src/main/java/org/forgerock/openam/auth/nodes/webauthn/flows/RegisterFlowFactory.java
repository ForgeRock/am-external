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
package org.forgerock.openam.auth.nodes.webauthn.flows;

import org.forgerock.openam.auth.nodes.webauthn.WebAuthnRegistrationNode;
import org.forgerock.openam.core.realms.Realm;

/**
 * Factory for creating {@link RegisterFlow} instances.
 */
public interface RegisterFlowFactory {

    /**
     * Creates a new {@link RegisterFlow} instance.
     *
     * @param realm  the realm
     * @param config the configuration of the node which is creating this flow
     * @return the newly created {@link RegisterFlow} instance
     */
    RegisterFlow create(Realm realm, WebAuthnRegistrationNode.Config config);
}
