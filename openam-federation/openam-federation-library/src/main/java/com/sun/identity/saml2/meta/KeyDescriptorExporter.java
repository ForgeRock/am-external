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
package com.sun.identity.saml2.meta;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static org.apache.xml.security.algorithms.JCEMapper.getAlgorithmClassFromURI;
import static org.apache.xml.security.algorithms.JCEMapper.getJCEKeyAlgorithmFromURI;
import static org.apache.xml.security.algorithms.JCEMapper.getKeyLengthFromURI;
import static org.forgerock.openam.saml2.meta.KeyDescriptorUtil.createKeyInfoFromCertificate;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.EncryptionConstants;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.saml2.meta.KeyDescriptorUtil;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.jaxb.metadata.KeyDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.KeyDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.KeyTypes;
import com.sun.identity.saml2.jaxb.metadata.ObjectFactory;
import com.sun.identity.saml2.jaxb.xmlenc.EncryptionMethodType;
import com.sun.identity.saml2.jaxb.xmlenc11.MGFType;
import com.sun.identity.saml2.jaxb.xmlsig.DigestMethodType;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

class KeyDescriptorExporter {

    private static final Logger logger = LoggerFactory.getLogger(KeyDescriptorExporter.class);

    private static final ObjectFactory metaDataObjectFactory = new ObjectFactory();
    private static final com.sun.identity.saml2.jaxb.xmlsig.ObjectFactory sigObjectFactory =
            new com.sun.identity.saml2.jaxb.xmlsig.ObjectFactory();
    private static final com.sun.identity.saml2.jaxb.xmlenc.ObjectFactory encObjectFactory =
            new com.sun.identity.saml2.jaxb.xmlenc.ObjectFactory();
    private static final com.sun.identity.saml2.jaxb.xmlenc11.ObjectFactory enc11ObjectFactory =
            new com.sun.identity.saml2.jaxb.xmlenc11.ObjectFactory();

    private final Saml2CredentialResolver saml2CredentialResolver;

    @Inject
    public KeyDescriptorExporter(Saml2CredentialResolver saml2CredentialResolver) {
        this.saml2CredentialResolver = saml2CredentialResolver;
    }

    List<KeyDescriptorElement> createKeyDescriptors(String realm, String entityId, Saml2EntityRole role,
            Map<String, List<String>> roleExtended) throws SAML2Exception {
        return Stream.concat(createSigningKeyDescriptors(realm, entityId, role),
                createEncryptionKeyDescriptors(realm, entityId, role, roleExtended))
                .collect(Collectors.toList());
    }

    private Stream<KeyDescriptorElement> createSigningKeyDescriptors(String realm, String entityId,
            Saml2EntityRole role) throws SAML2Exception {
        return saml2CredentialResolver.resolveValidSigningCredentials(realm, entityId, role)
                .stream()
                .map(this::createSigningKeyDescriptor);
    }

    private KeyDescriptorElement createSigningKeyDescriptor(X509Certificate certificate) {
        KeyDescriptorType keyDescriptor = metaDataObjectFactory.createKeyDescriptorType();
        keyDescriptor.setKeyInfo(createKeyInfoFromCertificate(certificate));
        keyDescriptor.setUse(KeyTypes.SIGNING);
        return metaDataObjectFactory.createKeyDescriptorElement(keyDescriptor);
    }

    private Stream<KeyDescriptorElement> createEncryptionKeyDescriptors(String realm, String entity,
            Saml2EntityRole entityRole, Map<String, List<String>> roleExtended) throws SAML2Exception {
        Set<X509Certificate> certificates = saml2CredentialResolver.resolveValidEncryptionCredentials(realm, entity,
                entityRole);

        Map<Boolean, List<String>> partitionedEncryptionAlgorithms =
                Optional.ofNullable(roleExtended.get("encryptionAlgorithms"))
                        .orElseGet(Collections::emptyList)
                        .stream()
                        .distinct()
                        .collect(partitioningBy(this::isKeyTransportAlgorithm, toList()));

        List<String> keyTransportAlgorithms = partitionedEncryptionAlgorithms.get(true);
        List<String> dataEncryptionAlgorithms = partitionedEncryptionAlgorithms.get(false);

        return certificates.stream()
                .map(certificate -> createKeyDescriptorFromKeyAlias(certificate,
                        keyTransportAlgorithms, dataEncryptionAlgorithms))
                .map(metaDataObjectFactory::createKeyDescriptorElement);
    }

    private KeyDescriptorType createKeyDescriptorFromKeyAlias(X509Certificate certificate,
            List<String> keyTransportAlgorithms, List<String> dataEncryptionAlgorithms) {

        String keyAlgorithm = certificate.getPublicKey().getAlgorithm();
        List<String> compatibleKeyTransportAlgorithms =
                identifyCompatibleAlgorithms(keyAlgorithm, keyTransportAlgorithms);

        if (compatibleKeyTransportAlgorithms.isEmpty()) {
            // Defaults to RSA for encryption algorithm, though this may be incompatible with the key type
            compatibleKeyTransportAlgorithms = singletonList(
                    SystemPropertiesManager.get(SAML2Constants.RSA_KEY_TRANSPORT_ALGORITHM, XMLCipher.RSA_OAEP));
        }

        if (dataEncryptionAlgorithms.isEmpty()) {
            dataEncryptionAlgorithms = singletonList(XMLCipher.AES_128);
        }

        KeyDescriptorType keyDescriptor = metaDataObjectFactory.createKeyDescriptorType();
        keyDescriptor.getEncryptionMethod()
                .addAll(createEncryptionMethods(compatibleKeyTransportAlgorithms, this::fillOutKeyTransportDetails));
        keyDescriptor.getEncryptionMethod()
                .addAll(createEncryptionMethods(dataEncryptionAlgorithms, this::fillOutDataEncryptionDetails));
        keyDescriptor.setKeyInfo(KeyDescriptorUtil.createKeyInfoFromCertificate(certificate));
        keyDescriptor.setUse(KeyTypes.ENCRYPTION);

        return keyDescriptor;
    }

    private boolean isKeyTransportAlgorithm(String algorithm) {
        String algorithmClass = getAlgorithmClassFromURI(algorithm);

        if (algorithmClass == null) {
            // Unable to identify the algorithm class, check that the JCEMapper
            // library and xml-security-config.xml algorithm lists are in sync
            logger.warn("Unable to identify the algorithm class for algorithm {}", algorithm);
        }

        return "KeyTransport".equals(algorithmClass) || "SymmetricKeyWrap".equals(algorithmClass);
    }

    private List<EncryptionMethodType> createEncryptionMethods(List<String> encryptionAlgorithms,
            Consumer<EncryptionMethodType> encryptionPopulator) {

        return encryptionAlgorithms.stream()
                .map(this::createEncryptionMethod)
                .peek(encryptionPopulator)
                .collect(toList());
    }

    private EncryptionMethodType createEncryptionMethod(String encryptionAlgorithm) {
        EncryptionMethodType encryptionMethod = encObjectFactory.createEncryptionMethodType();
        encryptionMethod.setAlgorithm(encryptionAlgorithm);
        return encryptionMethod;
    }

    private void fillOutKeyTransportDetails(EncryptionMethodType encryptionMethod) {
        DigestMethodType digestMethodType = sigObjectFactory.createDigestMethodType();
        digestMethodType.setAlgorithm(SystemPropertiesManager.get(SAML2Constants.DIGEST_ALGORITHM, XMLCipher.SHA256));
        encryptionMethod.getContent().add(sigObjectFactory.createDigestMethod(digestMethodType));

        if (XMLCipher.RSA_OAEP_11.equals(encryptionMethod.getAlgorithm())) {
            MGFType mgfType = enc11ObjectFactory.createMGFType();
            mgfType.setAlgorithm(SystemPropertiesManager
                    .get(SAML2Constants.MASK_GENERATION_FUNCTION, EncryptionConstants.MGF1_SHA256));
            encryptionMethod.getContent().add(enc11ObjectFactory.createMGF(mgfType));
        }
    }

    private void fillOutDataEncryptionDetails(EncryptionMethodType encryptionMethod) {
        BigInteger keySize = BigInteger.valueOf(getKeyLengthFromURI(encryptionMethod.getAlgorithm()));
        encryptionMethod.getContent().add(encObjectFactory.createEncryptionMethodTypeKeySize(keySize));
    }

    private List<String> identifyCompatibleAlgorithms(String keyAlgorithm, List<String> encryptionAlgorithms) {
        return encryptionAlgorithms
                .stream()
                .filter(encryptionAlgorithm -> keyAlgorithm.equals(getJCEKeyAlgorithmFromURI(encryptionAlgorithm)))
                .collect(toList());
    }
}
