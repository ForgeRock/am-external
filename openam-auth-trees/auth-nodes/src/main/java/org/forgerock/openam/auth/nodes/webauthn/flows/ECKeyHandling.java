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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows;

import java.nio.ByteBuffer;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.util.encode.Base64url;

/**
 * Used to get key data as a byte array.
 */
final public class ECKeyHandling {

    /**
     * Represents the public key data as uncompressed public key data in bytes.
     *
     * @param keyData the key data.
     * @return the key data in formatted, uncompressed bytes.
     */
    public byte[] getKeyBytes(EcJWK keyData) {
        byte[] x = Base64url.decode(keyData.getX());
        byte[] y = Base64url.decode(keyData.getY());

        ByteBuffer buffer = ByteBuffer.allocate(x.length + y.length + 1);
        buffer.put((byte) 0x04);
        buffer.put(x);
        buffer.put(y);
        return buffer.array();
    }
}
