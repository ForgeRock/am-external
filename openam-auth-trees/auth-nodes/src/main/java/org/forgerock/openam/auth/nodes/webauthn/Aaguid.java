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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

/**
 * A class that represents an immutable Authenticator Attestation Globally Unique Identifier.
 * A relying party can use this to identify a metadata statement via the FIDO metadata service.
 */
public class Aaguid {
    // Underlying representation
    private final UUID uuid;

    /**
     * Constructor which wraps a UUID to construct a new AAGUID.
     *
     * @param uuid a UUID
     */
    public Aaguid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Constructor which uses a string to construct a new AAGUID.
     *
     * @param aaguid the AAGUID
     */
    public Aaguid(String aaguid) {
        this.uuid = UUID.fromString(aaguid);
    }

    /**
     * Constructor which uses a byte array to construct a new AAGUID.
     *
     * @param aaguidBytes the bytes
     */
    public Aaguid(byte[] aaguidBytes) {
        try {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(aaguidBytes);
            this.uuid = new UUID(byteBuffer.getLong(), byteBuffer.getLong());
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Insufficient bytes provided for a valid AAGUID", e);
        }
    }

    /**
     * Return the UUID representation.
     *
     * @return the AAGUID as a UUID Object
     */
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Aaguid aaguid = (Aaguid) obj;
        return Objects.equals(uuid, aaguid.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return uuid.toString();
    }
}
