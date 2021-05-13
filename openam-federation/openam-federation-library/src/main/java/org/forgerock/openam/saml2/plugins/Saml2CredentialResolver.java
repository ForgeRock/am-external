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
 * Copyright 2020 ForgeRock AS.
 */
package org.forgerock.openam.saml2.plugins;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Set;

import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.saml2.crypto.signing.Saml2SigningCredentials;

import com.sun.identity.saml2.common.SAML2Exception;

/**
 * This extension point allows having different implementations for resolving SAML2 credentials for signing and
 * decryption. This is to allow having a keystore based secret resolution for the fedlet, while also making it possible
 * to use other mechanisms to resolve secrets.
 */
public interface Saml2CredentialResolver {

    /**
     * Resolves the active credential to be used for signing SAML2 XMLs for the provided SAML2 entity provider.
     *
     * @param realm The realm the entity provider belongs to.
     * @param entityId The entity provider's entity ID.
     * @param role The entity provider role that issues the signed document.
     * @return The active signing credentials.
     * @throws SAML2Exception If there was an error while resolving the secrets.
     */
    Saml2SigningCredentials resolveActiveSigningCredential(String realm, String entityId,
            Saml2EntityRole role) throws SAML2Exception;

    /**
     * Returns the (ordered) set of valid signing credentials that remote entity providers should accept. This is to
     * allow exporting SAML2 metadata with the complete set of signing key descriptors.
     *
     * @param realm The realm the entity provider belongs to.
     * @param entityId The entity provider's entity ID.
     * @param role The entity provider role for which we need the valid signing credentials.
     * @return The valid signing certificates.
     * @throws SAML2Exception If there was an error while resolving the secrets.
     */
    Set<X509Certificate> resolveValidSigningCredentials(String realm, String entityId, Saml2EntityRole role)
            throws SAML2Exception;

    /**
     * Returns the (ordered) set of valid credentials that the entity provider can use to decrypt incoming messages.
     *
     * @param realm The realm the entity provider belongs to.
     * @param entityId The entity provider's entity ID.
     * @param role The entity provider role for which we need the valid decryption credentials.
     * @return The valid decryption keys.
     * @throws SAML2Exception If there was an error while resolving the secrets.
     */
    Set<PrivateKey> resolveValidDecryptionCredentials(String realm, String entityId, Saml2EntityRole role)
            throws SAML2Exception;

    /**
     * Returns the (ordered) set of valid credentials that remote entity providers can use when they encrypt messages
     * for the hosted entity provider.
     *
     * @param realm The realm the entity provider belongs to.
     * @param entityId The entity provider's entity ID.
     * @param role The entity provider role for which we need the valid encryption credentials.
     * @return The public certificates of the valid encryption keys.
     * @throws SAML2Exception If there was an error while resolving the secrets.
     */
    Set<X509Certificate> resolveValidEncryptionCredentials(String realm, String entityId, Saml2EntityRole role)
            throws SAML2Exception;
}
