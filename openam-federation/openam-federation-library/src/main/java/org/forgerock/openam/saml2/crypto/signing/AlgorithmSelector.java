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
 * Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.crypto.signing;

import java.math.BigInteger;
import java.security.Key;
import java.security.interfaces.DSAKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Singleton;
import javax.crypto.SecretKey;
import javax.xml.bind.JAXBIntrospector;

import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.signature.XMLSignature;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.util.Pair;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.ExtensionsType;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.algsupport.DigestMethodType;
import com.sun.identity.saml2.jaxb.metadata.algsupport.SigningMethodType;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * This class helps selecting cryptographic algorithms based on the remote entity provider's capabilities.
 *
 * @since AM 7.0.0
 */
@Singleton
class AlgorithmSelector {

    private static final Logger logger = LoggerFactory.getLogger(AlgorithmSelector.class);

    /**
     * Selects the signing algorithm and the digest method to be used based on the remote entity provider's preference.
     * The process is as follows:
     * <ul>
     *     <li>
     *         Look for the role descriptor's &lt;Extensions&gt; element, and find all &lt;alg:SigningMethod&gt; and
     *         &lt;alg:DigestMethod&gt; elements.
     *     </li>
     *     <li>
     *         If either of those elements could not be found, try to retrieve the Extensions from the top level entity
     *         descriptor's &lt;Extensions&gt;.
     *     </li>
     *     <li>
     *         If we still couldn't find a preferred signing method, or digest method, fall back to the globally
     *         configured defaults.
     *     </li>
     *     <li>
     *         Otherwise, go through the list of signing methods and find the first one that is suitable for the signing
     *         key that we have (verify the algorithm matches the key's algorithm, and the key matches the requirements
     *         of minimum and maximum key size).
     *     </li>
     *     <li>
     *         For digest methods, use the first one in the list.
     *     </li>
     * </ul>
     *
     * @param signingKey The signing key.
     * @param remoteEntityDescriptor The remote entity's descriptor.
     * @param entityRole The remote entity's role.
     * @param signingAlgorithmSelector The function that retrieves the default signing algorithm for the signing key.
     * @return The pair of the signing algorithm (first), and the digest method (second).
     * @throws SAML2Exception If there is no signing algorithm that can be used with the signing key provided.
     */
    Pair<String, String> selectSigningAlgorithms(Key signingKey, EntityDescriptorElement remoteEntityDescriptor,
            Saml2EntityRole entityRole, Function<Key, String> signingAlgorithmSelector) throws SAML2Exception {
        RoleDescriptorType roleDescriptor = entityRole.getStandardMetadata(remoteEntityDescriptor);
        ExtensionsType extensions = roleDescriptor.getExtensions();
        List<SigningMethodType> signingMethodTypes = getExtension(extensions, SigningMethodType.class);
        List<DigestMethodType> digestMethodTypes = getExtension(extensions, DigestMethodType.class);
        // If we couldn't find supported algorithms at the role level, we need to check for them at the provider level
        EntityDescriptorType remoteEntity = remoteEntityDescriptor.getValue();
        if (signingMethodTypes.isEmpty()) {
            signingMethodTypes = getExtension(remoteEntity.getExtensions(), SigningMethodType.class);
        }
        if (digestMethodTypes.isEmpty()) {
            digestMethodTypes = getExtension(remoteEntity.getExtensions(), DigestMethodType.class);
        }

        // If we couldn't find supported algorithms anywhere, we should fallback to the default algorithms.
        // Otherwise we should use the first algorithm that can be used with the provided signing key.
        String signingAlgorithm;
        if (signingMethodTypes.isEmpty()) {
            signingAlgorithm = signingAlgorithmSelector.apply(signingKey);
        } else {
            signingAlgorithm = signingMethodTypes.stream()
                .filter(signingMethodType -> checkKeyMatchesSigningAlgorithm(signingKey, signingMethodType))
                .map(SigningMethodType::getAlgorithm)
                .findFirst()
                .orElseThrow(() -> new SAML2Exception(SAML2SDKUtils.BUNDLE_NAME, "algNotSupported"));
        }

        String digestMethod;
        if (digestMethodTypes.isEmpty()) {
            digestMethod = getDefaultDigestMethod();
        } else {
            digestMethod = digestMethodTypes.stream()
                    .map(DigestMethodType::getAlgorithm)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unable to determine digest method algorithm"));

        }
        return Pair.of(signingAlgorithm, digestMethod);
    }

    /**
     * Returns the default signing algorithm for the provided signing key when signing XMLs.
     *
     * @param key The signing key.
     * @return The default signing algorithm determined based on the signing key's algorithm (RSA/DSA).
     */
    String getDefaultQuerySigningAlgorithmForKey(Key key) {
        switch (key.getAlgorithm()) {
        case "RSA":
            return SystemPropertiesManager.get(SAML2Constants.QUERY_SIGNATURE_ALGORITHM_RSA,
                    XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);
        case "DSA":
            return SystemPropertiesManager.get(SAML2Constants.QUERY_SIGNATURE_ALGORITHM_DSA,
                    XMLSignature.ALGO_ID_SIGNATURE_DSA_SHA256);
        case "EC":
            return SystemPropertiesManager.get(SAML2Constants.QUERY_SIGNATURE_ALGORITHM_EC,
                    XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA512);
        default:
            throw new IllegalArgumentException("Unsupported key algorithm");
        }
    }

    /**
     * Returns the default signing algorithm for the provided signing key.
     *
     * @param key The signing key.
     * @return The default signing algorithm determined based on the signing key's algorithm (RSA/DSA).
     */
    String getDefaultXmlSigningAlgorithmForKey(Key key) {
        String defaultAlgorithm;
        switch (key.getAlgorithm()) {
        case "RSA":
            defaultAlgorithm = XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256;
            break;
        case "DSA":
            defaultAlgorithm = XMLSignature.ALGO_ID_SIGNATURE_DSA_SHA256;
            break;
        case "EC":
            defaultAlgorithm = XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA512;
            break;
        default:
            throw new IllegalArgumentException("Unsupported key algorithm");
        }
        return SystemPropertiesManager.get(SAML2Constants.XMLSIG_ALGORITHM, defaultAlgorithm);
    }

    /**
     * Returns the default digest method.
     *
     * @return The default digest method (configured globally).
     */
    String getDefaultDigestMethod() {
        return SystemPropertiesManager.get(SAML2Constants.DIGEST_ALGORITHM,
                MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256);
    }

    private <T> List<T> getExtension(ExtensionsType extensions, Class<T> type) {
        if (extensions != null) {
            return extensions.getAny().stream()
                    .map(JAXBIntrospector::getValue)
                    .filter(type::isInstance)
                    .map(type::cast)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @VisibleForTesting
    boolean checkKeyMatchesSigningAlgorithm(Key key, SigningMethodType signingMethodType) {
        if (!key.getAlgorithm().equals(JCEMapper.getJCEKeyAlgorithmFromURI(signingMethodType.getAlgorithm()))) {
            return false;
        }
        int minKeySize = Optional.ofNullable(signingMethodType.getMinKeySize())
                .map(BigInteger::intValue)
                .orElse(0);
        int maxKeySize = Optional.ofNullable(signingMethodType.getMaxKeySize())
                .map(BigInteger::intValue)
                .orElse(Integer.MAX_VALUE);
        try {
            Optional<Integer> keySize = getKeySize(key);
            if (keySize.isEmpty()) {
                return true;
            }
            return keySize.get() >= minKeySize && keySize.get() <= maxKeySize;
        } catch (IllegalArgumentException e) {
            logger.debug("Ignoring unsupported key type: {}", key.getClass());
            return false;
        }
    }

    private Optional<Integer> getKeySize(Key key) {
        if (key instanceof RSAKey) {
            return Optional.of(((RSAKey) key).getModulus().bitLength());
        } else if (key instanceof DSAKey) {
            return Optional.of(((DSAKey) key).getParams().getP().bitLength());
        } else if (key instanceof ECKey) {
            return Optional.of(((ECKey) key).getParams().getCurve().getField().getFieldSize());
        } else if (key instanceof SecretKey) {
            logger.debug("Unsupported symmetric key {}", key);
            throw new IllegalArgumentException("Unsupported symmetric key");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Unable to determine key size for '{}' type {}.", key, key.getClass());
        }
        return Optional.empty();
    }
}
