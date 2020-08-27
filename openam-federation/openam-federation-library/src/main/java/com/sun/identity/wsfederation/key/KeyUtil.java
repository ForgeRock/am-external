/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: KeyUtil.java,v 1.4 2009/10/28 23:58:58 exu Exp $
 *
 * Portions Copyrighted 2019 ForgeRock AS.
 */


package com.sun.identity.wsfederation.key;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;

/**
 * The <code>KeyUtil</code> provides methods to obtain
 * the hosting entity's signing key and decryption key, and
 * to obtain a partner entity's signature verification key
 * and encryption related information
 */
public class KeyUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyUtil.class);

    private static KeyProvider kp = null;

    // key is EntityID|Role
    // value is EncInfo
    protected static Hashtable encHash = new Hashtable();

    private static Map<CacheKey, X509Certificate> sigHash = new Hashtable<>();
    
    static {
        try {
            kp = (KeyProvider)Class.forName(SystemConfigurationUtil.getProperty(
                SAMLConstants.KEY_PROVIDER_IMPL_CLASS)).newInstance();
        } catch (ClassNotFoundException cnfe) {
            logger.error(
                "KeyUtil static block:" +
                " Couldn't find the class.",
                cnfe);
            kp = null;
        } catch (InstantiationException ie) {
            logger.error(
                "KeyUtil static block:" +
                " Couldn't instantiate the key provider instance.",
                ie);
            kp = null;
        } catch (IllegalAccessException iae) {
            logger.error(
                "KeyUtil static block:" +
                " Couldn't access the default constructor.",
                iae);
            kp = null;
        }            
    }

    private KeyUtil() {
    }

    /**
     * Returns the instance of <code>KeyProvider</code>.
     * @return <code>KeyProvider</code>
     */
    public static KeyProvider getKeyProviderInstance() {
        return kp;
    }

    /**
     * Returns the host entity's signing certificate alias.
     * @param baseConfig <code>BaseConfigType</code> for the host entity
     * @return <code>String</code> for host entity's signing
     * certificate alias
     */    
    public static String getSigningCertAlias(BaseConfigType baseConfig) {

        Map<String,List<String>> map = 
            WSFederationMetaUtils.getAttributes(baseConfig);
        List<String> list = map.get(SAML2Constants.SIGNING_CERT_ALIAS);
        if (list != null && !list.isEmpty()) {
            String alias = list.get(0);
            if (alias != null && alias.length() != 0 && kp != null) {
                return alias;
            }
        }
        return null;
    }

    /**
     * Returns the host entity's decryption key.
     * @param baseConfig <code>BaseConfigType</code> for the host entity
     * @return <code>PrivateKey</code> for decrypting a message received
     * by the host entity
     */    
    public static PrivateKey getDecryptionKey(BaseConfigType baseConfig) {

        Map map = WSFederationMetaUtils.getAttributes(baseConfig);
        List list = (List)map.get(SAML2Constants.ENCRYPTION_CERT_ALIAS);
        PrivateKey decryptionKey = null;
        if (list != null && !list.isEmpty()) {
            String alias = (String)list.get(0);
            if (alias != null && alias.length() != 0 && kp != null) {
                decryptionKey = kp.getPrivateKey(alias);
            }
        }
        return decryptionKey;
    }

    /**
     * Returns the partner entity's signature verification certificate.
     * @param fed <code>FederationElement</code> for the partner entity
     * @param entityID partner entity's ID
     * @param realm partner entity's realm
     * @param isIDP whether partner entity's role is IDP or SP 
     * @return <code>X509Certificate</code> for verifying the partner
     * entity's signature
     */    
    public static X509Certificate getVerificationCert(
        FederationElement fed,
        String entityID,
        String realm,
        boolean isIDP
    ) {
        String classMethod = "KeyUtil.getVerificationCert: ";
        String role = (isIDP) ? "idp":"sp";        
        if (logger.isDebugEnabled()) {
            logger.debug("{}Entering... \nEntityID={}\nRole={}\nRealm={}",
                classMethod, entityID, role, realm);
        }
        // first try to get it from cache
        CacheKey index = new CacheKey(entityID, role, realm);
        X509Certificate cert = sigHash.get(index);
        if (cert != null) {
            return cert;
        }
        // else get it from meta
        if (fed == null) {
            logger.error("{}Null SSODescriptorType input for entityID={} in realm {} with {} role.",
                    classMethod, entityID, realm, role);
            return null;
        }
        cert = getCert(fed);
        if (cert == null) {
            logger.error("{}No signing cert for entityID={} in realm {} with {} role.",
                    classMethod, entityID, realm, role);
            return null;
        }
        sigHash.put(index, cert);
        return cert;
    }
    
    /**
     * Returns certificate stored in <code>FederationElement</code>.
     * @param fed <code>FederationElement</code> which contains certificate info
     * @return X509Certificate contained in <code>FederationElement</code>; or
     *                <code>null</code> if no certificate is included.
     */
    public static java.security.cert.X509Certificate getCert(
        FederationElement fed ) {
        String classMethod = "KeyUtil.getCert: ";
        
        byte[] bt = WSFederationUtils.getMetaManager().
            getTokenSigningCertificate(fed);

        CertificateFactory cf = null;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (java.security.cert.CertificateException ce) {
            logger.error(
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
            logger.error(
                classMethod +
                "Unable to generate certificate from byte "+
                "array input stream.", ce);
            return null;
        }
        return retCert;
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
