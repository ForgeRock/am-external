/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.jwtpop;

/**
 * Type of confirmation key.
 */
public enum ConfirmationKeyType {
    /**
     * The confirmation key is a JSON Web Key representation of the public key.
     */
    JWK,
    /**
     * The confirmation key is a key id.
     */
    KID
}
