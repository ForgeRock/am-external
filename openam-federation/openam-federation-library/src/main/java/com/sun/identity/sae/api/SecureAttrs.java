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
 * $Id: SecureAttrs.java,v 1.12 2009/03/31 17:18:10 exu Exp $
 *
 * Portions Copyrighted 2016-2025 Ping Identity Corporation.
 */

package com.sun.identity.sae.api;


import static org.forgerock.openam.utils.Time.newDate;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.forgerock.openam.annotations.Supported;

import com.sun.identity.security.DataEncryptor;
import com.sun.identity.shared.encode.Base64;

/**
 * <code>SecureAttrs</code> class forms the core api of "Secure Attributes
 * Exchange" (SAE) feature. The class uses off the shelf digital
 * signing and encryption algorithms to generate tamperproof/nonrepudiable
 * strings representing attribute maps and to verify these strings.
 * Typical SAE usage is to securely send attributes (authentication &amp;
 * use profile data) from an asserting application (eg running on an IDP) to
 * a relying application (eg running on an SP). In this scenario the
 * asserting party uses the "signing" interfaces to generate secure
 * data and the relying application uses "verification" interfaces
 * to ascertain the authenticity of the data.
 * Current implementation provides two mechanisms to secure attributes :
 * Symmetric  : uses simple shared secrets between the two ends.
 * Asymmetric : uses PKI based signing using public-private keys.
 * Freshness is provided by a varying seed generated from the
 * current timestamp and a configurable expiry period within which
 * the relying party must validate the token.
 */
@Supported
public class SecureAttrs {
    /**
     * HTTP parameter name used to send and receive secure attribute data.
     * IDP : sends secure attrs in this parameter.
     * SP  : receives secure attrs in this parameter.
     */
    @Supported
    public static final String SAE_PARAM_DATA = "sun.data";

    /**
     * SAE Parameter representing a command.
     * Currently only "logout" needs to be explicitly provided. SSO is implied.
     * IDP  : Uses this parameter to instruct FM to issue a global logout.
     * SP   : Receives this parameter from FM.
     */
    @Supported
    public static final String SAE_PARAM_CMD = "sun.cmd";

    /**
     * SAE Parameter representing the authenticated user.
     * IDP  : Uses this parameter to send authenticated userid to FM.
     * SP   : Receives userid in this parameter.
     */
    @Supported
    public static final String SAE_PARAM_USERID = "sun.userid";

    /**
     * SAE Parameter representing the session's authentication level.
     * IDP  : Uses this parameter to send authentication level to FM.
     * SP   : Receives authentication level in this parameter.
     */
    @Supported
    public static final String SAE_PARAM_AUTHLEVEL = "sun.authlevel";

    /**
     * SAE Parameter used to pass IDP entity ID to SP app.
     * IDP: Not Applicable
     * SP: populates this parameter to identify IDP used in SSO.
     */
    public static final String SAE_PARAM_IDPENTITYID = "sun.idpentityid";

    /**
     * SAE Parameter used to pass SP entity ID to SP app.
     * IDP: Not Applicable
     * SP: populates this parameter to identify SP used in SSO.
     */
    public static final String SAE_PARAM_SPENTITYID = "sun.spentityid";

    /**
     * SAE Parameter representing the requested SP app to be invoked.
     * IDP  : populates this parameter with SP side app to be invoked.
     * SP   : Not Applicable.
     */
    @Supported
    public static final String SAE_PARAM_SPAPPURL = "sun.spappurl";

    /**
     * SAE Parameter used to identify the IDP app (Asserting party)
     * IDP  : populates this parameter to identify itself.
     * SP   : Not Applicable.
     */
    @Supported
    public static final String SAE_PARAM_IDPAPPURL = "sun.idpappurl";

    /**
     * SAE Parameter internally used by FM for storing token timestamp.
     */
    @Supported
    public static final String SAE_PARAM_TS = "sun.ts";

    /**
     * SAE Parameter internally used by FM for storing signature data.
     */
    @Supported
    public static final String SAE_PARAM_SIGN = "sun.sign";

    /**
     * SAE Parameter used to comunicate errors.
     */
    @Supported
    public static final String SAE_PARAM_ERROR = "sun.error";

    /**
     * SAE Parameter used to communicate to SP to return to specified url
     * upon Logout completion.
     * IDP : Not applicable
     * SP  : expected to redirect to the value upon processing logout req.
     */
    @Supported
    public static final String SAE_PARAM_APPSLORETURNURL = "sun.returnurl";

    /**
     * SAE Parameter used to comunicate to FM where to redirect after a
     * global logout is completed.
     * IDP : sends this param as part of logout command.
     * SP  : N/A.
     */
    @Supported
    public static final String SAE_PARAM_APPRETURN = "sun.appreturn";

    /**
     * SAE command <code>SAE_PARAM_CMD</code>
     */
    @Supported
    public static final String SAE_CMD_LOGOUT = "logout";

    /**
     * Crypto types supported.
     */
    @Supported
    public static final String SAE_CRYPTO_TYPE = "type";

    /**
     * Crypto type : Symmetric : shared secret based trust between parties.
     */
    @Supported
    public static final String SAE_CRYPTO_TYPE_ASYM = "asymmetric";

    /**
     * Crypto type : Asymmetric : PKI based trust.
     */
    @Supported
    public static final String SAE_CRYPTO_TYPE_SYM = "symmetric";

    /**
     * SAE Config : classame implementing <code>Cert</code>.
     * If not specified, a JKS keystore default impl is used.
     */
    public static final String SAE_CONFIG_CERT_CLASS = "certclassimpl";

    /**
     * SAE Config : Location of the keystore to access keys from for
     * asymmetric crypto.
     */
    @Supported
    public static final String SAE_CONFIG_KEYSTORE_FILE = "keystorefile";

    /**
     * SAE Config : keystore type. Default : JKS
     */
    @Supported
    public static final String SAE_CONFIG_KEYSTORE_TYPE = "keystoretype";

    /**
     * SAE Config : Password to open the keystrore.
     */
    @Supported
    public static final String SAE_CONFIG_KEYSTORE_PASS = "keystorepass";

    /**
     * SAE Config : Private key alias for asymmetric signing. Alias
     * is used to retrive the key from the keystore.
     */
    @Supported
    public static final String SAE_CONFIG_PRIVATE_KEY_ALIAS = "privatekeyalias";

    /**
     * SAE Config : Public key for asymmetric signature verification. Alias
     * is used to retrive the key from the keystore.
     */
    @Supported
    public static final String SAE_CONFIG_PUBLIC_KEY_ALIAS = "pubkeyalias";

    /**
     * SAE Config : Private key for asymmetric signing.
     */
    @Supported
    public static final String SAE_CONFIG_PRIVATE_KEY = "privatekey";

    /**
     * SAE Config : Password to access the private key.
     */
    @Supported
    public static final String SAE_CONFIG_PRIVATE_KEY_PASS = "privatekeypass";

    /**
     * SAE Config : Flag to indicate whether keys should be cached in memory
     * once retrieved from the keystore.
     */
    @Supported
    public static final String SAE_CONFIG_CACHE_KEYS = "cachekeys";

    /**
     * SAE Config : shared secret constant - used internally in FM.
     */
    @Supported
    public static final String SAE_CONFIG_SHARED_SECRET = "secret";

    /**
     * SAE Config : data encryption algorithm.
     */
    @Supported
    public static final String SAE_CONFIG_DATA_ENCRYPTION_ALG = "encryptionalgorithm";

    /**
     * SAE Config : data encryption key strength.
     */
    @Supported
    public static final String SAE_CONFIG_ENCRYPTION_KEY_STRENGTH = "encryptionkeystrength";

    /**
     * SAE Config :  Signature validity : since timetamp on signature.
     */
    @Supported
    public static final String SAE_CONFIG_SIG_VALIDITY_DURATION = "saesigvalidityduration";
    /**
     * Debug : true | false
     */
    public static boolean dbg = false;

    private Certs certs;

    private static final HashMap<String, SecureAttrs> instances = new HashMap<>();
    private int tsDuration = 120000; // 2 minutes
    private boolean asymsigning = false;
    private boolean asymencryption = false;
    private String dataEncAlg = "DES";
    private int encKeyStrength = 56;

    /**
     * Returns an instance to perform crypto operations.
     *
     * @param name The name of the instance to return.
     * @return <code>SecureAttrs</code> instance.
     */
    @Supported
    public static synchronized SecureAttrs getInstance(String name) {
        return instances.get(name);
    }

    /**
     * Initializes a SecureAttrs instance specified by <code>name</code>.
     * If the instance already exists, it replaces it with the new instance.
     * Use <code>SecureAttrs.getIstance(name)</code> to obtain the instance.
     *
     * @param name       Name of the <code>SecureAttrs</code> instance.
     * @param type       Cryptographic key type. Possible values are
     *                   <code>SecureAttrs.SAE_CRYPTO_TYPE_SYM</code>, and
     *                   <code>SecureAttrs.SAE_CRYPTO_TYPE_ASYM</code>
     * @param properties : please see SAE_CONFIG_* constants for configurable
     *                   values.
     * @throws Exception rethrows underlying exception.
     */
    @Supported
    synchronized public static void init(String name, String type, Properties properties) throws Exception {
        SecureAttrs sa = new SecureAttrs(type, properties);
        instances.put(name, sa);
    }

    /**
     * Creates two instances of <code>SecureAttrs</code> named
     * "symmetric" and "asymmetric" representing the two suppported
     * crytp types.
     *
     * @param properties : please see SAE_CONFIG_* constants for configurable
     *                   values.
     * @throws Exception rethrows underlying exception.
     */
    @Supported
    synchronized public static void init(Properties properties) throws Exception {
        init(SAE_CRYPTO_TYPE_ASYM, SAE_CRYPTO_TYPE_ASYM, properties);
        init(SAE_CRYPTO_TYPE_SYM, SAE_CRYPTO_TYPE_SYM, properties);
    }

    /**
     * Returns a Base64 encoded string comprising a signed set of attributes.
     *
     * @param attrs  Attribute Value pairs to be processed.
     * @param secret Shared secret (symmetric) Private key alias (asymmetric)
     * @return Base64 encoded token String to be passed to a relying party.
     */
    @Supported
    public String getEncodedString(Map<String, String> attrs, String secret) throws Exception {
        String signedAttrs = signAttributes(attrs, secret);
        return Base64.encode(signedAttrs.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns encrypted string for the given attributes. The encrypted
     * data is Base64 encoded string encrypted with supplied encryption
     * secret and signs using shared secret.
     *
     * @param attrs     Attribute Value pairs to be processed.
     * @param secret    Shared secret (symmetric) Private key alias (asymmetric)
     * @param encSecret The encryption secret (symmetric) or Public Key alias (asymmetric)
     * @return Base64 encoded token String to be passed to a relying party.
     */
    @Supported
    public String getEncodedString(Map<String, String> attrs, String secret, String encSecret)
            throws Exception {

        if (encSecret == null) {
            return getEncodedString(attrs, secret);
        }

        String signedString = signAttributes(attrs, secret);
        String encryptedString;
        if (asymencryption) {
            Key encKey = getPublicKey(encSecret).getPublicKey();
            encryptedString = DataEncryptor.encryptWithAsymmetricKey(
                    signedString,
                    dataEncAlg,
                    encKeyStrength,
                    encKey);
        } else {
            encryptedString = DataEncryptor.encryptWithSymmetricKey(
                    signedString,
                    dataEncAlg,
                    secret);
        }
        if (dbg) {
            System.out.println("SAE.getEncodedString: encrypted string" +
                    encryptedString);
        }
        return encryptedString;
    }

    private String signAttributes(Map<String, String> attrs, String secret) throws Exception {
        if (attrs == null || attrs.isEmpty()) {
            return null;
        }

        StringBuffer sb = new StringBuffer(200);
        attrs.keySet().forEach(k -> sb.append(k).append("=").append(attrs.get(k)).append("|"));
        sb.append("Signature=").append(getSignedString(attrs, secret));
        return sb.toString();
    }

    /**
     * Verifies a Base64 encoded string for authenticity based on the
     * shared secret supplied.
     *
     * @param str    Base64 encoded string containing attribute
     * @param secret Shared secret (symmmetric) or Public Key (asymmetric)
     * @return Decoded, verified and parsed attrbute name-valie pairs.
     */
    @Supported
    public Map<String, String> verifyEncodedString(String str, String secret) throws Exception {
        if (str == null) {
            return null;
        }

        Map<String, String> map = getRawAttributesFromEncodedData(str);
        if (dbg)
            System.out.println("SAE:verifyEncodedString() : " + map);
        String signatureValue = map.remove("Signature");
        if (!verifyAttrs(map, signatureValue, secret)) {
            return null;
        }
        return map;
    }

    /**
     * Verifies the encrypted data string using encryption secret and
     * shared secret that was used for signing.
     *
     * @param str       Base64 encoded string containing attribute
     * @param secret    Shared secret (symmmetric) or Public Key (asymmetric)
     * @param encSecret The encryption secret (symmetric) or Public
     *                  Key alias (asymmetric)
     * @return Decoded, verified and parsed attrbute name-valie pairs.
     */
    @Supported
    public Map<String, String> verifyEncodedString(String str, String secret, String encSecret)
            throws Exception {

        if (encSecret == null) {
            return verifyEncodedString(str, secret);
        }

        if (isNotEncrypted(str)) {
            return verifyEncodedString(str, secret);
        }
        if (str.indexOf(' ') > 0) {
            str = str.replace(' ', '+');
        }
        String decryptStr;
        if (asymencryption) {
            Key pKey = certs.getPrivateKey(encSecret);
            decryptStr = DataEncryptor.decryptWithAsymmetricKey(str,
                    dataEncAlg, pKey);
        } else {
            decryptStr = DataEncryptor.decryptWithSymmetricKey(str,
                    dataEncAlg, encSecret);
        }
        if (dbg) {
            System.out.println("SAE:verifyEncodedString() : " +
                    "decrypted string " + decryptStr);
        }
        return verifyEncodedString(decryptStr, secret);

    }

    private boolean isNotEncrypted(String str) {

        if (str.indexOf(' ') > 0) {
            str = str.replace(' ', '+');
        }
        byte[] decoded = Base64.decode(str);
        byte[] encString = new byte[9];
        System.arraycopy(decoded, 0, encString, 0, 9);

        String tmp = new String(encString, StandardCharsets.UTF_8);
        return !tmp.equals("ENCRYPTED");
    }

    /**
     * Returns a decoded <code>Map</code> of attribute-value pairs.
     * No verification is performed. Useful when retrieving data before
     * verifying contents for authenticity.
     *
     * @param str Base64 encoded string containing attribute
     * @return Decoded and parsed attrbute name-value pairs.
     */
    @Supported
    public Map<String, String> getRawAttributesFromEncodedData(String str) {
        if (str == null) {
            return null;
        }

        if (str.indexOf(' ') > 0)
            str = str.replace(' ', '+');
        byte[] bytes = Base64.decode(str);
        String decoded = new String(bytes, StandardCharsets.UTF_8);
        if (!decoded.contains("|")) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(decoded, "|");

        Map<String, String> map = new HashMap<>();
        while (tokenizer.hasMoreTokens()) {
            String st = tokenizer.nextToken();
            int index = st.indexOf("=");
            if (index == -1) {
                continue;
            }
            String attr = st.substring(0, index);
            String value = st.substring(index + 1);
            map.put(attr, value);
        }

        return map;
    }

    /**
     * Returns a decoded <code>Map</code> of attribute-value pairs.
     * No verification is performed. Useful when retrieving data before
     * verifying contents for authenticity.
     *
     * @param str       Base64 encoded string containing attribute
     * @param encSecret The encryption secret (symmetric) or Public
     *                  Key alias (asymmetric)
     * @return Decoded and parsed attrbute name-value pairs.
     */
    @Supported
    public Map<String, String> getRawAttributesFromEncodedData(String str, String encSecret)
            throws Exception {

        if (encSecret == null) {
            return getRawAttributesFromEncodedData(str);
        }
        if (str.indexOf(' ') > 0) {
            str = str.replace(' ', '+');
        }
        if (isNotEncrypted(str)) {
            return getRawAttributesFromEncodedData(str);
        }
        String decryptStr;
        if (asymencryption) {
            Key pKey = certs.getPrivateKey(encSecret);
            decryptStr = DataEncryptor.decryptWithAsymmetricKey(str,
                    dataEncAlg, pKey);
        } else {
            decryptStr = DataEncryptor.decryptWithSymmetricKey(str,
                    dataEncAlg, encSecret);
        }
        if (dbg) {
            System.out.println("SAE.getRawAttributes() decrypted" +
                    " string" + decryptStr);
        }
        return getRawAttributesFromEncodedData(decryptStr);
    }

    /**
     * This interface allows to set the private to be used for signing
     * as an alternative to passing down <code>SAE_CONFIG_PRIVATE_KEY_ALIAS</code>
     * via <code>init</code>. Use this interface if you do not want
     * SecureAttr to obtain the signing key from a configured keystore.
     * To use this key during signing, specify secret as null.
     *
     * @param privatekey The private key.
     */
    @Supported
    public void setPrivateKey(PrivateKey privatekey) {
        certs.setPrivatekey(privatekey);
    }

    /**
     * This interface allows to register a public key to be used for signature
     * verification. Use this interface if you do not want SecureAttrs to
     * obtain public keys from a configured keystore.
     *
     * @param pubkeyalias     The public key alias.
     * @param x509certificate instance.
     */
    @Supported
    public void addPublicKey(String pubkeyalias, X509Certificate x509certificate) {
        certs.addPublicKey(pubkeyalias, x509certificate);
    }

    private X509Certificate getPublicKey(String alias) {
        return certs.getPublicKey(alias);
    }

    /**
     * Returns a String representing data in the attrs argument.
     * The String generated can be one of the following depending
     * on configuration  :
     * SHA1 digest based on a shared secret and current timestamp.
     * or
     * Digital signature based on a configured certificate key.
     *
     * @param attrs  List of attribute Value pairs to be processed.
     * @param secret Shared secret (symmmetric) or Private Key (asymmetric)
     * @return token String to be passed to a relying party.
     */
    @Supported
    public String getSignedString(Map<String, String> attrs, String secret) throws Exception {
        // Normalize     
        StringBuffer str = normalize(attrs);
        // Setup a fresh timestamp
        long timestamp = (newDate()).getTime();

        String signature;

        if (asymsigning) {
            PrivateKey pKey = certs.getPrivateKey(secret);
            signature = signAsym(str.append(timestamp).toString(), pKey);
        } else {
            // Create seed : TIMESTAMP + shared secret
            String seed = secret + timestamp;
            // Encrypt
            signature = encrypt(str + seed);
        }
        if (signature == null) {
            return null;
        }
        return ("TS" + timestamp + "TS" + signature);
    }

    /**
     * Verifies the authenticity of data the attrs argument based
     * on the token presented. Both attrs and token is sent by
     * a asserting party.
     *
     * @param attrs  List of attribute Value pairs to be processed.
     * @param token  token represnting attrs provided by asserting party.
     * @param secret Shared secret (symmmetric) or Public Key (asymmetric)
     * @return true  if attrs and token verify okay, else returns false.
     */
    @Supported
    public boolean verifyAttrs(Map<String, String> attrs, String token, String secret)
            throws Exception {
        // Normalize     
        StringBuffer str = normalize(attrs);
        // Retrieve timestamp
        int idx = token.indexOf("TS", 2);
        String ts = token.substring(2, idx);
        long signts = Long.parseLong(ts);
        long nowts = (newDate()).getTime();

        // Check timestamp validity
        if ((nowts - signts) > tsDuration)
            return false;

        if (asymsigning) {
            String signature = token.substring(idx + 2);
            return verifyAsym(str.append(ts).toString(),
                    signature, getPublicKey(secret));
        }
        // Create seed : TIMESTAMP + shared secret
        String seed = secret + ts;
        // Encrypt
        String newstr = "TS" + ts + "TS" + encrypt(str + seed);
        return token.equals(newstr);
    }


    private SecureAttrs(String type, Properties properties) throws Exception {
        if (SAE_CRYPTO_TYPE_ASYM.equals(type)) {
            asymsigning = true;
            asymencryption = true;
        }
        String dur = properties.getProperty(SAE_CONFIG_SIG_VALIDITY_DURATION);
        if (dur != null)
            tsDuration = Integer.parseInt(dur);

        String clzName = properties.getProperty(SAE_CONFIG_CERT_CLASS);
        if (clzName != null)
            certs = (Certs) Class.forName(clzName).getDeclaredConstructor().newInstance();
        else
            certs = new DefaultCerts();

        certs.init(properties);
        dataEncAlg = (String) properties.get(SAE_CONFIG_DATA_ENCRYPTION_ALG);
        String tmp = (String) properties.get(
                SAE_CONFIG_ENCRYPTION_KEY_STRENGTH);
        if (tmp != null) {
            encKeyStrength = Integer.parseInt(tmp);
        }
    }

    private StringBuffer normalize(Map<String, String> attrs) {
        // Sort the Map
        TreeMap<String, String> smap = new TreeMap<>(attrs);

        // Flatten to a single String
        StringBuffer str = new StringBuffer();
        for (String key : smap.keySet()) {
            str.append(key).append("=").append(smap.get(key)).append("|");
        }
        return str;
    }

    private synchronized String encrypt(String plaintext) throws Exception {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA"); //step 2
        } catch (NoSuchAlgorithmException e) {
            throw new Exception(e.getMessage());
        }
        md.update((plaintext).getBytes(StandardCharsets.UTF_8)); //step 3

        byte[] raw = md.digest(); //step 4

        return Base64.encode(raw); //step 6
    }

    private String signAsym(String s, PrivateKey privatekey) {
        if (s == null || s.length() == 0 || privatekey == null) {
            if (dbg)
                System.out.println("SAE : signAsym: returning since priv key null");
            return null;
        }
        String s1 = privatekey.getAlgorithm();
        Signature signature;
        if (s1.equals("RSA"))
            try {
                signature = Signature.getInstance("SHA1withRSA");
            } catch (Exception exception) {
                System.out.println("SAE:asym sign : RSA failed =" + exception);
                return null;
            }
        else if (s1.equals("DSA")) {
            try {
                signature = Signature.getInstance("SHA1withDSA");
            } catch (Exception exception1) {
                System.out.println("SAE:asym sign : DSA failed =" + exception1);
                return null;
            }
        } else {
            System.out.println("SAE:asym sign : No Algorithm");
            return null;
        }
        try {
            signature.initSign(privatekey);
        } catch (Exception exception2) {
            System.out.println("SAE:asym sign : sig.initSign failed" + exception2);
            return null;
        }
        try {
            System.out.println("Query str:" + s);
            signature.update(s.getBytes());
        } catch (Exception exception3) {
            System.out.println("SAE:asym sign : sig.update failed" + exception3);
            return null;
        }
        byte[] abyte0;
        try {
            abyte0 = signature.sign();
        } catch (Exception exception4) {
            System.out.println("SAE:asym sign : sig.sign failed" + exception4);
            return null;
        }
        if (abyte0 == null || abyte0.length == 0) {
            System.out.println("SAE:asym sign : sigBytes null");
            return null;
        } else {
            String s4 = Base64.encode(abyte0);
            System.out.println("B64 Signature=" + s4);
            return s4;
        }
    }

    private boolean verifyAsym(String s, String s1, X509Certificate x509certificate) {
        if (s == null || s.length() == 0 || x509certificate == null || s1 == null) {
            if (dbg) {
                System.out.println("SAE:asym verify: qstring or cert or signature is null");
            }
            return false;
        }
        byte[] abyte0 = Base64.decode(s1);
        if (dbg) {
            System.out.println("SAE:verifyAsym:signature=" + Arrays.toString(abyte0) + " origstr=" + s1);
        }
        String s2 = x509certificate.getPublicKey().getAlgorithm();
        Signature signature;
        if (s2.equals("DSA"))
            try {
                signature = Signature.getInstance("SHA1withDSA");
            } catch (Exception exception) {
                System.out.println("SAE:asym verify : DSA instance" + exception);
                exception.printStackTrace();
                return false;
            }
        else if (s2.equals("RSA")) {
            try {
                signature = Signature.getInstance("SHA1withRSA");
            } catch (Exception exception1) {
                System.out.println("SAE:asym verify : RSA instance" + exception1);
                exception1.printStackTrace();
                return false;
            }
        } else {
            System.out.println("SAE:asym verify : no instance");
            return false;
        }
        try {
            signature.initVerify(x509certificate);
        } catch (Exception exception2) {
            System.out.println("SAE:asym verify :sig.initVerify" + exception2);
            exception2.printStackTrace();
            return false;
        }
        try {
            signature.update(s.getBytes());
        } catch (Exception exception3) {
            System.out.println("SAE:asym verify :sig.update:" + exception3 + " sig=" + Arrays.toString(abyte0));
            exception3.printStackTrace();
            return false;
        }
        boolean flag;
        try {
            flag = signature.verify(abyte0);
        } catch (Exception exception4) {
            System.out.println("SAE:asym verify :sig.verify:" + exception4 + "sig=" + Arrays.toString(abyte0));
            exception4.printStackTrace();
            return false;
        }
        return flag;
    }

    public interface Certs {
        void init(Properties props) throws Exception;

        PrivateKey getPrivateKey(String alias);

        X509Certificate getPublicKey(String alias);

        void setPrivatekey(PrivateKey privatekey);

        void addPublicKey(String pubkeyalias, X509Certificate x509certificate);
    }

    static class DefaultCerts implements Certs {
        private PrivateKey privateKey = null;
        private KeyStore ks = null;
        private final HashMap<String, X509Certificate> keyTable = new HashMap<>();
        private boolean cacheKeys = true;
        private String pkpass = null;

        public void init(Properties properties) throws Exception {
            String keyfile = properties.getProperty("keystorefile");
            if (keyfile != null) {
                String ktype = properties.getProperty(
                        SAE_CONFIG_KEYSTORE_TYPE, "JKS");
                ks = KeyStore.getInstance(ktype);
                FileInputStream fileinputstream = new FileInputStream(keyfile);
                String kpass = properties.getProperty(SAE_CONFIG_KEYSTORE_PASS);
                pkpass = properties.getProperty(SAE_CONFIG_PRIVATE_KEY_PASS);
                ks.load(fileinputstream, kpass.toCharArray());
                String pkeyalias = properties.getProperty(
                        SAE_CONFIG_PRIVATE_KEY_ALIAS);
                if (pkeyalias != null) {
                    privateKey = (PrivateKey) ks.getKey(pkeyalias,
                            pkpass.toCharArray());
                }
                String pubkeyalias = properties.getProperty(
                        SAE_CONFIG_PUBLIC_KEY_ALIAS);
                if ("false".equals(properties.getProperty(
                        SAE_CONFIG_CACHE_KEYS)))
                    cacheKeys = false;

                if (cacheKeys && pubkeyalias != null)
                    getPublicKeyFromKeystore(pubkeyalias);
            }
        }

        public PrivateKey getPrivateKey(String alias) {
            try {
                if (alias == null)
                    return privateKey;
                return (PrivateKey) ks.getKey(alias,
                        pkpass.toCharArray());
            } catch (Exception ex) {
                return null;
            }
        }

        public X509Certificate getPublicKey(String alias) {
            X509Certificate x509certificate = keyTable.get(alias);
            if (x509certificate == null && ks != null) {
                try {
                    x509certificate = getPublicKeyFromKeystore(alias);
                } catch (Exception exception) {
                    System.out.println("SAE:getPublicKey:Exc:" + exception);
                }
            }
            return x509certificate;
        }

        public void setPrivatekey(PrivateKey privatekey) {
            privateKey = privatekey;
        }

        public void addPublicKey(String pubkeyalias, X509Certificate x509certificate) {
            keyTable.put(pubkeyalias, x509certificate);
        }

        private X509Certificate getPublicKeyFromKeystore(String pubkeyalias)
                throws Exception {
            X509Certificate x509certificate =
                    (X509Certificate) ks.getCertificate(pubkeyalias);
            if (cacheKeys)
                keyTable.put(pubkeyalias, x509certificate);
            return x509certificate;
        }
    }
}
