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
package org.forgerock.openam.saml2.crypto.signing;

import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.util.Pair;
import org.forgerock.util.Reject;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;

/**
 * This factory allows creation of {@link SigningConfig} instances and makes sure that the required signing algorithms
 * and digest methods are suitable for the remote entity (if one was provided), and the signing key in question.
 *
 * @since AM 7.0.0
 */
@Singleton
public class SigningConfigFactory {

    private final AlgorithmSelector algorithmSelector;

    /**
     * Creates a new instance of {@link SigningConfigFactory}.
     *
     * @param algorithmSelector The algorithm selector implementation.
     */
    @Inject
    public SigningConfigFactory(AlgorithmSelector algorithmSelector) {
        this.algorithmSelector = algorithmSelector;
    }

    /**
     * A factory method to obtain the Guice injected singleton instance.
     *
     * @return The {@link SigningConfigFactory} singleton instance.
     */
    public static SigningConfigFactory getInstance() {
        return InjectorHolder.getInstance(SigningConfigFactory.class);
    }

    /**
     * Creates the signing configuration for signing query strings based on the provided details.
     *
     * @param signingKey The signing key.
     * @param remoteEntity The remote entity provider's descriptor.
     * @param remoteRole The remote entity's role.
     * @return The signing configuration created based on the provided details.
     * @throws SAML2Exception If the signing key is null, or if none of the algorithms can be used with the provided
     * signing key.
     */
    public SigningConfig createQuerySigningConfig(Key signingKey, EntityDescriptorElement remoteEntity,
            Saml2EntityRole remoteRole) throws SAML2Exception {
        return create(signingKey, null, remoteEntity, remoteRole,
                algorithmSelector::getDefaultQuerySigningAlgorithmForKey);
    }

    /**
     * Creates the signing configuration for signing XMLs based on the provided details.
     *
     * @param signingKey The signing key.
     * @param certificate The certificate to include in the signed document. May be null if the cert should not be
     * included.
     * @return The signing configuration created based on the provided details.
     * @throws SAML2Exception If the signing key is null, or if none of the algorithms can be used with the provided
     * signing key.
     */
    public SigningConfig createXmlSigningConfig(Key signingKey, X509Certificate certificate) throws SAML2Exception {
        return create(signingKey, certificate, null, null, algorithmSelector::getDefaultXmlSigningAlgorithmForKey);
    }

    /**
     * Creates the signing configuration for signing XMLs based on the provided details.
     *
     * @param signingKey The signing key.
     * @param certificate The certificate to include in the signed document. May be null if the cert should not be
     * included.
     * @param remoteEntity The remote entity provider's descriptor.
     * @param remoteRole The remote entity's role.
     * @return The signing configuration created based on the provided details.
     * @throws SAML2Exception If the signing key is null, or if none of the algorithms can be used with the provided
     * signing key.
     */
    public SigningConfig createXmlSigningConfig(Key signingKey, X509Certificate certificate,
            EntityDescriptorElement remoteEntity, Saml2EntityRole remoteRole) throws SAML2Exception {
        Reject.ifNull(remoteEntity, remoteRole);
        return create(signingKey, certificate, remoteEntity, remoteRole,
                algorithmSelector::getDefaultXmlSigningAlgorithmForKey);
    }

    private SigningConfig create(Key signingKey, X509Certificate certificate, EntityDescriptorElement remoteEntity,
            Saml2EntityRole remoteRole, Function<Key, String> signingAlgorithmSelector) throws SAML2Exception {
        if (signingKey == null) {
            throw new SAML2Exception(SAML2SDKUtils.BUNDLE_NAME, "nullInputMessage", "private key");
        }
        if (remoteEntity != null) {
            Pair<String, String> algorithms = algorithmSelector.selectSigningAlgorithms(signingKey, remoteEntity,
                    remoteRole, signingAlgorithmSelector);
            return new SigningConfig(signingKey, certificate, algorithms.getFirst(), algorithms.getSecond());
        }
        return new SigningConfig(signingKey, certificate, signingAlgorithmSelector.apply(signingKey),
                algorithmSelector.getDefaultDigestMethod());
    }
}
