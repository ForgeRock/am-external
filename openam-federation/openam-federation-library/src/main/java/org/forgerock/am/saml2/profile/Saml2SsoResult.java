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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.am.saml2.profile;

import java.security.PrivateKey;
import java.util.Set;

import com.sun.identity.saml2.assertion.NameID;

/**
 * This POJO contains information collated during SAML2 response processing.
 */
public class Saml2SsoResult {

    private final String universalId;
    private final NameID nameId;
    private final Set<PrivateKey> decryptionKeys;
    private final boolean shouldPersistNameId;

    /**
     * Constructor.
     *
     * @param universalId The user's universal ID. May be null if the account mapping was unsuccessful.
     * @param nameId The the potentially decrypted NameID object.
     * @param decryptionKeys The decryption keys to use when SAML2 attributes need to be decrypted.
     * @param shouldPersistNameId Whether the account link needs to be persisted.
     */
    public Saml2SsoResult(String universalId, NameID nameId, Set<PrivateKey> decryptionKeys,
            boolean shouldPersistNameId) {
        this.universalId = universalId;
        this.nameId = nameId;
        this.decryptionKeys = decryptionKeys;
        this.shouldPersistNameId = shouldPersistNameId;
    }

    /**
     * Returns the user's universal ID.
     *
     * @return The user's universal ID. May be null.
     */
    public String getUniversalId() {
        return universalId;
    }

    /**
     * Returns the NameID object from the assertion.
     *
     * @return The NameID object.
     */
    public NameID getNameId() {
        return nameId;
    }

    /**
     * Returns the decryption keys that can be used to decrypt the attributes in the assertion.
     *
     * @return The decryption keys.
     */
    public Set<PrivateKey> getDecryptionKeys() {
        return decryptionKeys;
    }

    /**
     * Returns whether the NameID mapping should be stored in the user data store.
     *
     * @return Whether the NameID mapping should be stored in the user data store.
     */
    public boolean shouldPersistNameId() {
        return shouldPersistNameId;
    }
}
