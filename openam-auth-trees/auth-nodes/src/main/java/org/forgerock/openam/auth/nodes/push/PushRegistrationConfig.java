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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.push;

import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.DEFAULT_TIMEOUT;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorCommonConfig;

/**
 * The Push Registration node configuration.
 */
public interface PushRegistrationConfig extends MultiFactorCommonConfig {

    /**
     * Specifies the timeout of the registration node.
     *
     * @return the length of time to wait for a device to register.
     */
    @Attribute(order = 70)
    default int timeout() {
        return DEFAULT_TIMEOUT;
    }

}
