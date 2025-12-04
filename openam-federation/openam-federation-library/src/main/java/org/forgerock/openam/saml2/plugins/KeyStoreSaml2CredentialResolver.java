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
 * Copyright 2020-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.plugins;

import static org.forgerock.openam.utils.StringUtils.isEmpty;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.saml2.crypto.signing.Saml2SigningCredentials;
import org.forgerock.util.Reject;

import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaManager;

/**
 * Keystore based SAML2 credential resolver implementation. This credential resolver can be used with fedlet as well as
 * with entity providers that have not migrated to the secrets framework yet.
 */
public class KeyStoreSaml2CredentialResolver implements Saml2CredentialResolver {

    private final SAML2MetaManager metaManager;
    private final KeyProvider keyProvider;

    /**
     * Constructor.
     *
     * @param metaManager SAML2 metadata manager.
     * @param keyProvider The key provider instance.
     */
    @Inject
    public KeyStoreSaml2CredentialResolver(SAML2MetaManager metaManager, KeyProvider keyProvider) {
        this.metaManager = metaManager;
        this.keyProvider = keyProvider;
    }

    @Override
    public Saml2SigningCredentials resolveActiveSigningCredential(String realm, String entityId,
            Saml2EntityRole role) {
        String alias = SAML2Utils.getSigningCertAlias(realm, entityId, role.getName());

        String encryptedKeyPass = SAML2Utils.getSigningCertEncryptedKeyPass(realm, entityId, role.getName());
        PrivateKey signingKey = isEmpty(encryptedKeyPass) ?
                keyProvider.getPrivateKey(alias) : keyProvider.getPrivateKey(alias, encryptedKeyPass);

        X509Certificate signingCert = keyProvider.getX509Certificate(alias);
        return new Saml2SigningCredentials(signingKey, signingCert);
    }

    @Override
    public Set<X509Certificate> resolveValidSigningCredentials(String realm, String entityId, Saml2EntityRole role)
            throws SAML2Exception {
        Reject.ifNull(realm, entityId, role);
        EntityDescriptorElement entityDescriptor = metaManager.getEntityDescriptor(realm, entityId);
        return KeyUtil.getVerificationCerts(role.getStandardMetadata(entityDescriptor), entityId, role.getName(),
                realm);
    }

    @Override
    public Set<PrivateKey> resolveValidDecryptionCredentials(String realm, String entityId, Saml2EntityRole role) {
        return KeyUtil.getDecryptionKeys(realm, entityId, role.getName());
    }

    @Override
    public Set<X509Certificate> resolveValidEncryptionCredentials(String realm, String entityId, Saml2EntityRole role) {
        return SAML2Utils.getEncryptionCertAliases(realm, entityId, role.getName())
                .stream()
                .map(keyProvider::getX509Certificate)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
