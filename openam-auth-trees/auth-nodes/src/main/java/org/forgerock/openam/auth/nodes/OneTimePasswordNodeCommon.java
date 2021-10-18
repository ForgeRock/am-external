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
package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD_ENCRYPTED;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.crypto.NodeSharedStateCrypto;
import org.forgerock.util.Reject;

/**
 * Base class for common OTP node functionality.
 */
public abstract class OneTimePasswordNodeCommon extends SingleOutcomeNode {

    private final NodeSharedStateCrypto nodeSharedStateCrypto;

    /**
     * Constructor.
     *
     * @param nodeSharedStateCrypto the crypto operations for encrypting/decrypting payloads
     */
    public OneTimePasswordNodeCommon(NodeSharedStateCrypto nodeSharedStateCrypto) {
        this.nodeSharedStateCrypto = nodeSharedStateCrypto;
    }

    /**
     * Retrieves the OTP in cleartext form.
     *
     * @param context the tree context containing a shared state with OTP details, cannot be {@code null}
     * @return the cleartext OTP which can be found in the shared state either as cleartext or in an encrypted
     * form; the latter will require decryption before it is returned
     */
    protected String getClearTextOtp(TreeContext context) {
        Reject.ifNull(context, "tree context must not be null");
        JsonValue encryptedOtp = context.sharedState.get(ONE_TIME_PASSWORD_ENCRYPTED);
        if (encryptedOtp.isNotNull()) {
            JsonValue decryptedOtp = nodeSharedStateCrypto.decrypt(encryptedOtp.asString());
            return decryptedOtp.get(ONE_TIME_PASSWORD).asString();
        }
        return context.sharedState.get(ONE_TIME_PASSWORD).asString();
    }

}
