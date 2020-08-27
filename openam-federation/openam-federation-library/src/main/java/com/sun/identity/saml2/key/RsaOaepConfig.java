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
 * Copyright 2019-2020 ForgeRock AS.
 */
package com.sun.identity.saml2.key;

import java.util.Optional;
import java.util.function.Supplier;

import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.EncryptionConstants;
import org.forgerock.util.Reject;

import com.google.common.base.Suppliers;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * Contains the settings necessary to completely set up RSA OAEP based encryption.
 */
public class RsaOaepConfig {

    private static final Supplier<RsaOaepConfig> DEFAULT_INSTANCE_SUPPLIER = Suppliers.memoize(() -> new RsaOaepConfig(
            SystemPropertiesManager.get(SAML2Constants.DIGEST_ALGORITHM, XMLCipher.SHA256),
            SystemPropertiesManager.get(SAML2Constants.MASK_GENERATION_FUNCTION, EncryptionConstants.MGF1_SHA256),
            null));
    private final String digestMethod;
    private final String maskGenerationFunction;
    private final byte[] oaepParams;

    /**
     * Constructor.
     *
     * @param digestMethod The digest method to use during encryption.
     * @param maskGenerationFunction The mask generation function to use.
     * @param oaepParams The OAEP parameters to use (also known as the label, or PSource).
     */
    public RsaOaepConfig(String digestMethod, String maskGenerationFunction, byte[] oaepParams) {
        Reject.ifNull(digestMethod, "Digest method must not be null");
        Reject.ifNull(maskGenerationFunction, "Mask Generation Function must not be null");
        this.digestMethod = digestMethod;
        this.maskGenerationFunction = maskGenerationFunction;
        this.oaepParams = oaepParams;
    }

    /**
     * Returns the digest method's algorithm URI.
     *
     * @return The digest method's algorithm URI.
     */
    public String getDigestMethod() {
        return digestMethod;
    }

    /**
     * Returns the mask generation function's algorithm URI.
     *
     * @return The mask generation function's algorithm URI.
     */
    public String getMaskGenerationFunction() {
        return maskGenerationFunction;
    }

    /**
     * Returns the OAEP parameters as bytes. The OAEP parameters are also known as {@literal PSource}, or
     * {@literal label}.
     *
     * @return The OAEP parameters as bytes.
     */
    public byte[] getOaepParams() {
        return oaepParams;
    }

    /**
     * Returns the default RSA OAEP configuration for the provided key transport algorithm.
     *
     * @param keyTransportAlgorithm The key transport algorithm.
     * @return The default RSA OAEP configuration for the key transport algorithm.
     */
    public static Optional<RsaOaepConfig> getDefaultConfigForKeyTransportAlgorithm(String keyTransportAlgorithm) {
        if (XMLCipher.RSA_OAEP.equals(keyTransportAlgorithm)) {
            return Optional.of(new RsaOaepConfig(
                    SystemPropertiesManager.get(SAML2Constants.DIGEST_ALGORITHM, XMLCipher.SHA256),
                    EncryptionConstants.MGF1_SHA1,
                    null));
        } else if (XMLCipher.RSA_OAEP_11.equals(keyTransportAlgorithm)) {
            return Optional.of(DEFAULT_INSTANCE_SUPPLIER.get());
        } else {
            return Optional.empty();
        }
    }
}
