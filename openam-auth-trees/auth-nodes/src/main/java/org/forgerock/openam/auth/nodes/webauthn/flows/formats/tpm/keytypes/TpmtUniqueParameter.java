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
 * Copyright 2023-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.keytypes;

import java.security.PublicKey;

/**
 * Interface to abstract away the validation logic for comparing the pubArea unique value
 * with various algorithm-specific aspects of the public key.
 */
public interface TpmtUniqueParameter {
    /**
     * Verifies that the provided public key matches the unique parameter.
     *
     * @param publicKey The public key in use.
     * @return True if the algorithm-specific conditions are met, as specified in the implementing class.
     */
    boolean verifyUniqueParameter(PublicKey publicKey);
}



