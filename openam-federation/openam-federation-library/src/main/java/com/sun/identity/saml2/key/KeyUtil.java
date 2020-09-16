/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: KeyUtil.java,v 1.10 2009/08/28 23:42:14 exu Exp $
 *
 * Portions Copyrighted 2013-2019 ForgeRock AS.
 */
package com.sun.identity.saml2.key;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.namespace.QName;

import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.EncryptionConstants;
import org.forgerock.openam.federation.util.XmlSecurity;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.w3c.dom.Element;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.metadata.KeyDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.KeyDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.KeyTypes;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.XACMLPDPDescriptorType;
import com.sun.identity.saml2.jaxb.xmlenc.EncryptionMethodType;
import com.sun.identity.saml2.jaxb.xmlsig.DigestMethodType;
import com.sun.identity.saml2.jaxb.xmlsig.KeyInfoType;
import com.sun.identity.saml2.jaxb.xmlsig.X509DataType;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * The <code>KeyUtil</code> provides methods to obtain
 * the hosting entity's signing key and decryption key, and
 * to obtain a partner entity's signature verification key
 * and encryption related information
 */
public class KeyUtil {

    private static final QName DIGEST_METHOD = new QName("http://www.w3.org/2000/09/xmldsig#", "DigestMethod");
    private static final QName MGF = new QName("http://www.w3.org/2009/xmlenc11#", "MGF");
    private static final QName OAEP_PARAMS = new QName("http://www.w3.org/2001/04/xmlenc#", "OAEPparams");

    private static KeyProvider keyProvider = null;

    private static Map<CacheKey, EncryptionConfig> encHash = new Hashtable<>();
    private static Map<CacheKey, Set<X509Certificate>> sigHash = new Hashtable<>();

    static {
        XmlSecurity.init();
        try {
            keyProvider = (KeyProvider)Class.forName(SystemConfigurationUtil.getProperty(
                    SAMLConstants.KEY_PROVIDER_IMPL_CLASS,
                    SAMLConstants.JKS_KEY_PROVIDER)).newInstance();
        } catch (ClassNotFoundException cnfe) {
            SAML2SDKUtils.debug.error(
                    "KeyUtil static block:" +
                            " Couldn't find the class.",
                    cnfe);
            keyProvider = null;
        } catch (InstantiationException ie) {
            SAML2SDKUtils.debug.error(
                    "KeyUtil static block:" +
                            " Couldn't instantiate the key provider instance.",
                    ie);
            keyProvider = null;
        } catch (IllegalAccessException iae) {
            SAML2SDKUtils.debug.error(
                    "KeyUtil static block:" +
                            " Couldn't access the default constructor.",
                    iae);
            keyProvider = null;
        }
    }

    private KeyUtil() {
    }

    /**
     * Returns the instance of <code>KeyProvider</code>.
     * @return <code>KeyProvider</code>
     */
    public static KeyProvider getKeyProviderInstance() {
        return keyProvider;
    }

    /**
     * Returns the host entity's signing certificate alias.
     * @param baseConfig <code>BaseConfigType</code> for the host entity
     * @return <code>String</code> for host entity's signing
     * certificate alias
     */
    public static String getSigningCertAlias(JAXBElement<BaseConfigType> baseConfig) {

        Map map = SAML2MetaUtils.getAttributes(baseConfig);
        List list = (List)map.get(SAML2Constants.SIGNING_CERT_ALIAS);
        if (list != null && !list.isEmpty()) {
            String alias = (String)list.get(0);
            if (alias != null && alias.length() != 0 && keyProvider != null) {
                return alias;
            }
        }
        return null;
    }

    /**
     * Returns the hosted entity's decryption keys.
     *
     * @param realm The realm the hosted entity belongs to.
     * @param entityID The entity ID.
     * @param role The role of the hosted entity.
     * @return The Set of <code>PrivateKey</code>s for decrypting a message received by the hosted entity.
     */
    public static Set<PrivateKey> getDecryptionKeys(String realm, String entityID, String role) {
        return getDecryptionKeys(SAML2Utils.getEncryptionCertAliases(realm, entityID, role));
    }

    /**
     * Returns the host entity's decryption keys.
     *
     * @param baseConfig <code>BaseConfigType</code> for the host entity.
     * @return The Set of <code>PrivateKey</code>s for decrypting a message received by the hosted entity.
     */
    public static Set<PrivateKey> getDecryptionKeys(JAXBElement<BaseConfigType> baseConfig) {
        Map<String, List<String>> attrs = SAML2MetaUtils.getAttributes(baseConfig);
        List<String> aliases = attrs.get(SAML2Constants.ENCRYPTION_CERT_ALIAS);
        return getDecryptionKeys(aliases);
    }

    private static Set<PrivateKey> getDecryptionKeys(List<String> aliases) {
        final String classMethod = "KeyUtil.getDecryptionKeys: ";
        final Set<PrivateKey> decryptionKeys = new LinkedHashSet<>(3);
        if (aliases != null) {
            if (keyProvider != null) {
                for (String alias : aliases) {
                    if (StringUtils.isNotEmpty(alias)) {
                        PrivateKey decryptionKey = keyProvider.getPrivateKey(alias);
                        if (decryptionKey != null) {
                            decryptionKeys.add(decryptionKey);
                        } else {
                            SAML2SDKUtils.debug.error(classMethod + "No decryptionKey found for alias: {}", alias);
                        }
                    } else {
                        SAML2SDKUtils.debug.error(classMethod + "alias was empty.");
                    }
                }
            } else {
                SAML2SDKUtils.debug.error(classMethod + "keyProvider was null.");
            }
        } else {
            SAML2SDKUtils.debug.message("{}passed aliases list was null.", classMethod);
        }

        return decryptionKeys;
    }
    /**
     * Returns the host entity's decryption key.
     * @param baseConfig <code>BaseConfigType</code> for the host entity
     * @return <code>PrivateKey</code> for decrypting a message received
     * by the host entity
     */
    public static PrivateKey getDecryptionKey(JAXBElement<BaseConfigType> baseConfig) {
        return CollectionUtils.getFirstItem(getDecryptionKeys(baseConfig), null);
    }

    /**
     * Returns the partner entity's signature verification certificate.
     *
     * @param roleDescriptor <code>RoleDescriptor</code> for the partner entity.
     * @param entityID Partner entity's ID.
     * @param role Entity's role.
     * @param realm Entity's realm
     * @return The set of signing {@link X509Certificate} for verifying the partner entity's signature.
     */
    public static Set<X509Certificate> getVerificationCerts(RoleDescriptorType roleDescriptor, String entityID,
            String role, String realm) {
        String classMethod = "KeyUtil.getVerificationCerts: ";

        // first try to get it from cache
        CacheKey index = new CacheKey(entityID, role, realm);
        Set<X509Certificate> certificates = sigHash.get(index);
        if (certificates != null) {
            return certificates;
        }

        certificates = new LinkedHashSet<>(3);
        // else get it from meta
        if (roleDescriptor == null) {
            SAML2SDKUtils.debug.error("{}Null RoleDescriptorType input for entityID={} in realm {} with {} role.",
                    classMethod, entityID, realm, role);
            return null;
        }
        List<KeyDescriptorElement> keyDescriptors = getKeyDescriptors(roleDescriptor, SAML2Constants.SIGNING);
        if (keyDescriptors.isEmpty()) {
            SAML2SDKUtils.debug.error("{}No signing KeyDescriptor for entityID={} in realm {} with {} role.", classMethod, entityID, realm, role);
            return certificates;
        }

        for (KeyDescriptorElement keyDescriptor : keyDescriptors) {
            certificates.add(getCert(keyDescriptor));
        }
        if (certificates.isEmpty()) {
            SAML2SDKUtils.debug.error("{} No signing cert for entityID={} in realm {} with {} role.", classMethod, entityID, realm, role);
            return null;
        }
        sigHash.put(index, certificates);
        return certificates;
    }

    /**
     * Returns the encryption information which will be used in
     * encrypting messages intended for the partner entity.
     *
     * @param roleDescriptor <code>RoleDescriptor</code> for the partner entity
     * @param entityID partner entity's ID
     * @param role entity's role
     * @param realm the entity's realm
     * @return <code>EncryptionConfig</code> which includes partner entity's
     * public key for wrapping the secret key, data encryption algorithm,
     * and data encryption strength
     */
    public static EncryptionConfig getEncryptionConfig(RoleDescriptorType roleDescriptor, String entityID,
            String role, String realm) {
        String classMethod = "KeyUtil.getEncryptionConfig: ";
        SAML2SDKUtils.debug.message("{}Entering... \nEntityID={}\nRole={}\nRealm={}", classMethod, entityID, role, realm);

        // first try to get it from cache
        CacheKey index = new CacheKey(entityID, role, realm);
        EncryptionConfig encryptionConfig = encHash.get(index);
        if (encryptionConfig != null) {
            return encryptionConfig;
        }
        // else get it from meta
        if (roleDescriptor == null) {
            SAML2SDKUtils.debug.error("{}Null RoleDescriptorType input for entityID={} in realm {} with {} role.", classMethod, entityID, realm, role);
            return null;
        }
        KeyDescriptorElement keyDescriptor = getKeyDescriptor(roleDescriptor, SAML2Constants.ENCRYPTION);
        if (keyDescriptor == null) {
            SAML2SDKUtils.debug.error("{}No encryption KeyDescriptor for entityID={} in realm {} with {} role.", classMethod, entityID, realm, role);
            return null;
        }
        return getEncryptionConfig(keyDescriptor, entityID, role, realm);
    }

    /**
     * Returns the {@link KeyDescriptorType}s from {@link RoleDescriptorType} that matches the requested usage.
     * KeyDescriptors without usage defined are also included in this list, as by definition they should be suitable for
     * any purposes.
     *
     * @param roleDescriptor {@link RoleDescriptorType} which contains {@link KeyDescriptorType}s.
     * @param usage Type of the {@link KeyDescriptorType}s to be retrieved. Its value is "encryption" or "signing".
     * @return {@link KeyDescriptorType}s in {@link RoleDescriptorType} that matched the usage type.
     */
    public static List<KeyDescriptorElement> getKeyDescriptors(RoleDescriptorType roleDescriptor, String usage) {
        List<KeyDescriptorElement> keyDescriptors = roleDescriptor.getKeyDescriptor();
        List<KeyDescriptorElement> matches = new ArrayList<>(keyDescriptors.size());
        List<KeyDescriptorElement> keyDescriptorsWithoutUsage = new ArrayList<>(keyDescriptors.size());

        for (KeyDescriptorElement keyDescriptor : keyDescriptors) {
            KeyTypes use = keyDescriptor.getValue().getUse();
            if (use == null) {
                keyDescriptorsWithoutUsage.add(keyDescriptor);
            } else if (use.value().equals(usage)) {
                matches.add(keyDescriptor);
            }
        }

        matches.addAll(keyDescriptorsWithoutUsage);
        return matches;
    }

    /**
     * Returns <code>KeyDescriptorType</code> from
     * <code>RoleDescriptorType</code>.
     * @param roled <code>RoleDescriptorType</code> which contains
     *                <code>KeyDescriptor</code>s.
     * @param usage type of the <code>KeyDescriptorType</code> to be retrieved.
     *                Its value is "encryption" or "signing".
     * @return KeyDescriptorType in <code>RoleDescriptorType</code> that matched
     *                the usage type.
     */
    public static KeyDescriptorElement getKeyDescriptor(
            RoleDescriptorType roled,
            String usage
    ) {
        final List<KeyDescriptorElement> keyDescriptors = getKeyDescriptors(roled, usage);
        return CollectionUtils.getFirstItem(keyDescriptors, null);
    }

    /**
     * Returns certificate stored in <code>KeyDescriptorType</code>.
     * @param kd <code>KeyDescriptorType</code> which contains certificate info
     * @return X509Certificate contained in <code>KeyDescriptorType</code>; or
     *                <code>null</code> if no certificate is included.
     */
    public static java.security.cert.X509Certificate getCert(
            KeyDescriptorElement kd
    ) {

        String classMethod = "KeyUtil.getCert: ";
        KeyInfoType ki = kd.getValue().getKeyInfo();
        if (ki == null) {
            SAML2SDKUtils.debug.error(classMethod +
                    "No KeyInfo.");

            return null;
        }
        //iterate and search the X509DataElement node
        Iterator<Object> it = ki.getContent().iterator();
        X509DataType data = null;
        while ((data == null) && it.hasNext()) {
            Object content = JAXBIntrospector.getValue(it.next());
            if (content instanceof X509DataType) {
                data = (X509DataType) content;
            }
        }
        if (data == null) {
            SAML2SDKUtils.debug.error(classMethod + "No X509DataElement.");
            return null;
        }
        //iterate and search the X509Certificate node
        it = data.getX509IssuerSerialOrX509SKIOrX509SubjectName().iterator();
        byte[] cert = null;
        while ((cert == null) && it.hasNext()) {
            Object content = it.next();
            if (content instanceof JAXBElement
                    && ((JAXBElement) content).getName().getLocalPart().equals(SAMLConstants.TAG_X509CERTIFICATE)) {
                cert = (byte[]) JAXBIntrospector.getValue(content);
            }
        }
        if (cert == null) {
            SAML2SDKUtils.debug.error(classMethod + "No X509Certificate.");
            return null;
        }
        byte[] bt = cert;
        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (java.security.cert.CertificateException ce) {
            SAML2SDKUtils.debug.error(
                    classMethod +
                            "Unable to get CertificateFactory "+
                            "for X.509 type", ce);
            return null;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(bt);
        java.security.cert.X509Certificate retCert = null;
        try {
            while (bais.available() > 0) {
                retCert = (java.security.cert.X509Certificate)
                        cf.generateCertificate(bais);
            }
        } catch (java.security.cert.CertificateException ce) {
            SAML2SDKUtils.debug.error(
                    classMethod +
                            "Unable to generate certificate from byte "+
                            "array input stream.", ce);
            return null;
        }
        return retCert;
    }

    /**
     * Returns the partner entity's signature verification certificates.
     *
     * @param pepDescriptor <code>XACMLAuthzDecisionQueryDescriptorElement</code> for the partner entity.
     * @param entityID Policy Enforcement Point (PEP) entity identifier.
     * @param realm the realm
     * @return The Set of signing {@link X509Certificate}s for verifying the partner entity's signature.
     */
    public static Set<X509Certificate> getPEPVerificationCerts(XACMLAuthzDecisionQueryDescriptorType pepDescriptor,
            String entityID, String realm) {
        return getVerificationCerts(pepDescriptor, entityID, SAML2Constants.PEP_ROLE, realm);
    }

    /**
     * Returns the encryption information which will be used in
     * encrypting messages intended for the partner entity.
     *
     * @param pepDesc <code>XACMLAuthzDecisionQueryDescriptorElement</code>
     * for the partner entity
     * @param pepEntityID partner entity's ID
     * @param realm the realm
     * @return <code>EncryptionConfig</code> which includes partner entity's
     * public key for wrapping the secret key, data encryption algorithm,
     * and data encryption strength
     */
    public static EncryptionConfig getPepEncryptionConfig(XACMLAuthzDecisionQueryDescriptorType pepDesc,
            String pepEntityID, String realm) {
        String classMethod = "KeyUtil.getEncryptionConfig: ";
        String role = SAML2Constants.PEP_ROLE;

        SAML2SDKUtils.debug.message("{}Entering... \nEntityID={}\nRole={}\nRealm={}", classMethod, pepEntityID, role, realm);

        // first try to get it from cache
        CacheKey index = new CacheKey(pepEntityID, role, realm);
        EncryptionConfig encryptionConfig = encHash.get(index);
        if (encryptionConfig != null) {
            return encryptionConfig;
        }
        // else get it from meta
        if (pepDesc == null) {
            SAML2SDKUtils.debug.error("{}Null PEP Descriptor input for entityID={} in realm {} with {} role.",
                    classMethod, pepEntityID, realm, role);
            return null;
        }
        KeyDescriptorElement keyDescriptor = getKeyDescriptor(pepDesc, SAML2Constants.ENCRYPTION);
        if (keyDescriptor == null) {
            SAML2SDKUtils.debug.error("{}No encryption KeyDescriptor for entityID={} in realm {} with {} role.",
                    classMethod, pepEntityID, realm, role);
            return null;
        }
        return getEncryptionConfig(keyDescriptor, pepEntityID, role, realm);
    }

    /**
     * Returns the <code>EncryptionConfig</code> from the <code>KeyDescriptor</code>.
     *
     * @param keyDescriptor the M<code>KeyDescriptor</code> object.
     * @param entityID the entity identifier
     * @param role the role of the entity . Value can be PEP or PDP.
     * @param realm the realm.
     * @return <code>EncryptionConfig</code> the encryption info.
     */
    private static EncryptionConfig getEncryptionConfig(KeyDescriptorElement keyDescriptor, String entityID,
            String role, String realm) {
        String classMethod = "KeyUtil:getEncryptionConfig:";
        X509Certificate cert = getCert(keyDescriptor);
        if (cert == null) {
            SAML2SDKUtils.debug.error("{}No encryption cert for entityID={} in realm {} with {} role.", classMethod, entityID, realm, role);
            return null;
        }
        List<EncryptionMethodType> encryptionMethods = keyDescriptor.getValue().getEncryptionMethod();
        String dataEncryptionAlgorithm = null;
        String keyTransportAlgorithm = null;
        RsaOaepConfig rsaOaepConfig = null;
        int keySize = 0;

        for (EncryptionMethodType encryptionMethod : encryptionMethods) {
            String alg = encryptionMethod.getAlgorithm();
            String algorithmClass = JCEMapper.getAlgorithmClassFromURI(alg);
            if (keyTransportAlgorithm == null
                    && ("KeyTransport".equals(algorithmClass) || "SymmetricKeyWrap".equals(algorithmClass))) {
                keyTransportAlgorithm = alg;
                if (XMLCipher.RSA_OAEP.equals(alg)) {
                    rsaOaepConfig = getRsaOaepConfig(encryptionMethod);
                } else if (XMLCipher.RSA_OAEP_11.equals(alg)) {
                    rsaOaepConfig = getRsaOaep11Config(encryptionMethod);
                }
            } else if (dataEncryptionAlgorithm == null && "BlockEncryption".equals(algorithmClass)) {
                dataEncryptionAlgorithm = alg;
                keySize = SAML2Utils.getKeySizeFromEncryptionMethod(encryptionMethod);
            } else if (dataEncryptionAlgorithm != null && keyTransportAlgorithm != null) {
                break;
            }
        }

        if (StringUtils.isEmpty(dataEncryptionAlgorithm)) {
            dataEncryptionAlgorithm = XMLCipher.AES_128;
            keySize = 128;
        }
        PublicKey publicKey = cert.getPublicKey();
        EncryptionConfig encryptionConfig = null;
        if (publicKey != null) {
            encryptionConfig = new EncryptionConfig(publicKey, dataEncryptionAlgorithm, keySize, keyTransportAlgorithm,
                    Optional.ofNullable(rsaOaepConfig));
        }
        if (encryptionConfig != null) {
            CacheKey index = new CacheKey(entityID, role, realm);
            encHash.put(index, encryptionConfig);
        }
        return encryptionConfig;
    }

    private static RsaOaepConfig getRsaOaepConfig(EncryptionMethodType encryptionMethod) {
        return getRsaOaepConfig(encryptionMethod, element -> EncryptionConstants.MGF1_SHA1);
    }

    private static RsaOaepConfig getRsaOaep11Config(EncryptionMethodType encryptionMethod) {
        return getRsaOaepConfig(encryptionMethod,
                element -> Optional.ofNullable(element)
                        .map(e -> e.getAttribute("Algorithm"))
                        .orElseGet(() -> SystemPropertiesManager.get(SAML2Constants.MASK_GENERATION_FUNCTION,
                                EncryptionConstants.MGF1_SHA256)));
    }

    private static RsaOaepConfig getRsaOaepConfig(EncryptionMethodType encryptionMethod,
            Function<Element, String> mgfFunction) {
        Optional<String> digestMethod = Optional.empty();
        Element mgfElement = null;
        byte[] oaepParams = null;
        if (encryptionMethod != null) {
            List<Object> encryptionSettings = encryptionMethod.getContent();
            for (Object encryptionSetting : encryptionSettings) {
                if (encryptionSetting instanceof JAXBElement) {
                    JAXBElement jaxbElement = (JAXBElement) encryptionSetting;
                    if (DIGEST_METHOD.equals(jaxbElement.getName())) {
                        digestMethod = Optional.of(jaxbElement)
                                .map(JAXBIntrospector::getValue)
                                .map(DigestMethodType.class::cast)
                                .map(DigestMethodType::getAlgorithm);
                    } else if (OAEP_PARAMS.equals(jaxbElement.getName())) {
                        oaepParams = (byte[]) JAXBIntrospector.getValue(jaxbElement);
                    }
                } else if (encryptionSetting instanceof Element) {
                    Element element = (Element) encryptionSetting;
                    if (MGF.getNamespaceURI().equals(element.getNamespaceURI())
                            && MGF.getLocalPart().equals(element.getLocalName())) {
                        mgfElement = element;
                    }
                }
            }
        }
        return new RsaOaepConfig(
                digestMethod.orElseGet(() -> SystemPropertiesManager.get(SAML2Constants.DIGEST_ALGORITHM)),
                mgfFunction.apply(mgfElement),
                oaepParams);
    }

    /**
     * Returns the partner entity's signature verification certificates.
     *
     * @param pdpDescriptor <code>XACMLPDPDescriptorElement</code> of partner entity.
     * @param entityID partner entity's ID.
     * @return The Set of signing {@link X509Certificate}s for verifying the partner entity's signature.
     */
    public static Set<X509Certificate> getPDPVerificationCerts(XACMLPDPDescriptorType pdpDescriptor,
            String entityID, String realm) {
        return getVerificationCerts(pdpDescriptor, entityID, SAML2Constants.PDP_ROLE, realm);
    }

    /**
     * Clears the cache. This method is called when metadata is updated.
     */
    public static void clear() {
        sigHash.clear();
        encHash.clear();
    }

    /**
     * Composite key class comprising (EntityID,Role,Realm). The EntityID is trimmed
     * and null/empty realm is normalized to "/".
     */
    private static final class CacheKey {
        private final String entityId;
        private final String role;
        private final String realm;

        private CacheKey(String entityId, String role, String realm) {
            this.entityId = entityId.trim();
            this.role = role;
            if (StringUtils.isEmpty(realm)) {
                realm = "/";
            }
            this.realm = realm;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(entityId, cacheKey.entityId) &&
                    Objects.equals(role, cacheKey.role) &&
                    realm.equalsIgnoreCase(cacheKey.realm);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityId, role, realm.toLowerCase());
        }
    }
} 
