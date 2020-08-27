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
 * $Id: SAMLConstants.java,v 1.17 2009/06/12 22:21:39 mallas Exp $
 *
 * Portions Copyrighted 2010-2020 ForgeRock AS.
 */
package com.sun.identity.saml.common;

import java.util.HashSet;
import java.util.Set;

import org.forgerock.openam.annotations.Supported;

/**
 * This is a common class defining some constants common to all SAML elements.
 */
@Supported
public final class SAMLConstants 
{
    public static Set<String> passwordAuthMethods;
    public static Set<String> tokenAuthMethods;
    static {
        passwordAuthMethods = new HashSet<>();
        passwordAuthMethods.add("nt");
        passwordAuthMethods.add("ldap");
        passwordAuthMethods.add("membership");
        passwordAuthMethods.add("anonymous");
        tokenAuthMethods = new HashSet<>();
        tokenAuthMethods.add("radius");
    }

    /**
     * String to identify a new line charactor.
     */
    public static final String NL                       = "\n";

    /**
     * String to identify a left angle.
     */
    public static final String LEFT_ANGLE              = "<";

    /**
     * String to identify a right angle.
     */
    public static final String RIGHT_ANGLE              = ">";

    /**
     * String to identify "/&gt;".
     */
    public static final String END_ELEMENT              = "/>";

    /**
     * String to identify "&lt;/".
     */
    public static final String START_END_ELEMENT = "</";

    /**
     * String to identify a space charactor.
     */
    public static final String SPACE                = " ";

    /**
     * SAML assertion namespace URI.
     *
     */
    @Supported
    public static final String assertionSAMLNameSpaceURI = 
                "urn:oasis:names:tc:SAML:1.0:assertion";
    /**
     * SOAP 1.1 namespace URI.
     *
     */
    @Supported
    public static final String SOAP_URI =
                "http://schemas.xmlsoap.org/soap/envelope/";

    /**
     * SOAP envelope prefix.
     */
    public static final String SOAP_ENV_PREFIX = "soap-env";

    /**
     * XML Digital Signature namespace.
     *
     */
    @Supported
    public static final String XMLSIG_NAMESPACE_URI =
                        "http://www.w3.org/2000/09/xmldsig#";

    /**
     * Element name for xml signature.
     */
    public static final String XMLSIG_ELEMENT_NAME = "Signature";

    /**
     * String which gets incorporated into
     * <code>toString(includeNS, declareNS)</code> when 
     * <code>declareNS</code> is true for any assertion element.
     */
    public static final String assertionDeclareStr = 
        " xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\"";

    /**
     * String used in the <code>ActionNamespace</code> attribute to refer to
     * common sets of actions to perform on resources. 
     *
     * Title: Read/Write/Execute/Delete/Control
     * Defined actions: <code>Read Write Execute Delete Control</code>
     * These actions are interpreted in the normal manner, i.e. 
     * <ul>
     * <li><code>Read</code>: The subject may read the resource </li>
     * <li><code>Write</code>: The subject may modify the resource </li>
     * <li><code>Execute</code>: The subject may execute the resource </li>
     * <li><code>Delete</code>: The subject may delete the resource </li>
     * <li><code>Control</code>: The subject may specify the access control
     *     policy for the resource.</li>
     * </ul>
     *
     */
    @Supported
    public static final String ACTION_NAMESPACE = 
        "urn:oasis:names:tc:SAML:1.0:action:rwedc";

    /**
     * String used in the <code>ActionNamespace</code> attribute to refer to
     * common sets of actions to perform on resources. 
     *
     * Title: Read/Write/Execute/Delete/Control with Negation
     * Defined actions:
     * <code>Read Write Execute Delete Control ~Read ~Write ~Execute ~Delete
     * ~Control</code>
     * <ul>
     * <li><code>Read</code>: The subject may read the resource </li>
     * <li><code>Write</code>: The subject may modify the resource </li>
     * <li><code>Execute</code>: The subject may execute the resource </li>
     * <li><code>Delete</code>: The subject may delete the resource </li>
     * <li><code>Control</code>: The subject may specify the access control
     *     policy for the resource </li>
     * <li><code>~Read</code>:  The subject may NOT read the resource </li>
     * <li><code>~Write</code>: The subject may NOT modify the resource </li>
     * <li><code>~Execute</code>: The subject may NOT execute the resource </li>
     * <li><code>~Delete</code>: The subject may NOT delete the resource </li>
     * <li><code>~Control</code>: The subject may NOT specify the access
     *     control policy for the resource </li>
     * </ul>
     * An application MUST NOT authorize both an action and its negated form.
     *
     */
    @Supported
    public static final String ACTION_NAMESPACE_NEGATION = 
                "urn:oasis:names:tc:SAML:1.0:action:rwedc-negation";

    /**
     * String used in the <code>ActionNamespace</code> attribute to refer to
     * common sets of actions to perform on resources. 
     *
     * Title: <code>Get/Head/Put/Post</code>
     * Defined actions: 
     *          <code>GET HEAD PUT POST</code>
     * These actions bind to the corresponding HTTP operations. For example a
     * subject authorized to perform the GET action on a resource is authorized
     * to retrieve it. The GET and HEAD actions loosely correspond to the 
     * conventional read permission and the PUT and POST actions to the write 
     * permission. The correspondence is not exact however since a HTTP GET 
     * operation may cause data to be modified and a POST operation may cause
     * modification to a resource other than the one specified in the request. 
     * For this reason a separate Action URI specifier is provided. 
     *
     */
    @Supported
    public static final String ACTION_NAMESPACE_GHPP = 
                "urn:oasis:names:tc:SAML:1.0:ghpp";

    /**
     * String used in the <code>ActionNamespace</code> attribute to refer to
     * common sets of actions to perform on resources. 
     *
     * Title: UNIX File Permissions
     * Defined actions: 
     * The defined actions are the set of UNIX file access permissions
     * expressed in the numeric (octal) notation. The action string is a four
     * digit numeric code: extended user group world 
     * Where the extended access permission has the value  
     * <ul>
     * <li><code>+2 if sgid is set</code></li>
     * <li><code>+4 if suid is set</code></li>
     * </ul>
     * The user group and world access permissions have the value
     * <ul>
     * <li><code>+1 if execute permission is granted</code></li>
     * <li><code>+2 if write permission is granted</code></li>
     * <li><code>+4 if read permission is granted</code></li>
     * </ul>
     * For example 0754 denotes the UNIX file access permission: user read,
     * write and execute, group read and execute and world read. 
     *
     */
    @Supported
    public static final String ACTION_NAMESPACE_UNIX = 
                "urn:oasis:names:tc:SAML:1.0:action:unix";

    /**
     * saml namespace prefix with ":".
     */
    public static final String ASSERTION_PREFIX = "saml:";

    /**
     * Major version of assertion.
     */
    public static final int ASSERTION_MAJOR_VERSION = 1;
    /**
     * Default Assertion minor version.
     */
    public static int ASSERTION_MINOR_VERSION = 1;

    /**
     * Assertion minor version 0.
     */
    public static final int ASSERTION_MINOR_VERSION_ZERO = 0;

    /**
     * Assertion minor version 1.
     */
    public static final int ASSERTION_MINOR_VERSION_ONE = 1;

    /**
     * Assertion handle, request id, and response id have this length.
     * If server id cannot be found, assertion id has this length also.
     * request id, response id, and assertion id will be base64 encoded for
     * printing.
     */
    public static final int ID_LENGTH = 20;

    /**
     * SAML Bearer confirmation method identifier URI.
     *
     */
    @Supported
    public static final String CONFIRMATION_METHOD_BEARER =
                "urn:oasis:names:tc:SAML:1.0:cm:bearer";

    /**
     * SAML "Holder of Key" confirmation method identifier URI.
     *
     */
    @Supported
    public static final String CONFIRMATION_METHOD_HOLDEROFKEY =
                "urn:oasis:names:tc:SAML:1.0:cm:holder-of-key";

    /**
     * Kerberos authentication method.
     */
    public static final String AUTH_METHOD_KERBEROS = "Kerberos";

    /**
     * Certificate authentication method.
     */
    public static final String AUTH_METHOD_CERT = "Cert";

    /**
     * Certificate authentication method URI.
     */
    public static final String AUTH_METHOD_CERT_URI = "urn:ietf:rfc:2246";

    /**
     * Password authentication method URI.
     */
    public static final String AUTH_METHOD_PASSWORD_URI = 
        "urn:oasis:names:tc:SAML:1.0:am:password";

    /**
     * Hardware token authentication method uri.
     */
    public static final String AUTH_METHOD_HARDWARE_TOKEN_URI = 
        "urn:oasis:names:tc:SAML:1.0:am:HardwareToken";

    /**
     * Kerberos authentication method uri.
     */
    public static final String AUTH_METHOD_KERBEROS_URI = "urn:ietf:rfc:1510";

    /**
     * Private authentication method prefix.
     */
    public static final String AUTH_METHOD_URI_PREFIX =
                                "urn:com:sun:identity:";
 
    // Used for xml digital signing
    public static final String CANONICALIZATION_METHOD =
        "com.sun.identity.saml.xmlsig.c14nMethod";
    public static final String TRANSFORM_ALGORITHM=
        "com.sun.identity.saml.xmlsig.transformAlg";
    public static final String XMLSIG_ALGORITHM =
        "com.sun.identity.saml.xmlsig.xmlSigAlgorithm";    
    public static final String DIGEST_ALGORITHM =
        "com.sun.identity.saml.xmlsig.digestAlgorithm";
    public static final String JKS_KEY_PROVIDER = 
        "com.sun.identity.saml.xmlsig.JKSKeyProvider"; 
    public static final String KEY_PROVIDER_IMPL_CLASS =
        "com.sun.identity.saml.xmlsig.keyprovider.class";
    public static final String SIGNATURE_PROVIDER_IMPL_CLASS =
        "com.sun.identity.saml.xmlsig.signatureprovider.class";
    public static final String AM_SIGNATURE_PROVIDER =
        "com.sun.identity.saml.xmlsig.AMSignatureProvider";
   
    // constants for XML Signature SignatureMethodURI

    /**
     * MAC Algorithm HMAC-SHA1 URI - Required.
     *
     */
    @Supported
    public static final String ALGO_ID_MAC_HMAC_SHA1 = 
                                "http://www.w3.org/2000/09/xmldsig#hmac-sha1";

    /**
     * Signature Algorithm DSAwithSHA1 URI - Required.
     *
     */
    @Supported
    public static final String ALGO_ID_SIGNATURE_DSA =
                                "http://www.w3.org/2000/09/xmldsig#dsa-sha1";

    /**
     * Signature Algorithm DSAwithSHA1 URI - Required.
     *
     */
    @Supported
    public static final String ALGO_ID_SIGNATURE_DSA_256 =
                                "http://www.w3.org/2009/xmldsig11#dsa-sha256";

    /**
     * Signature Algorithm RSAwithSHA1 URI - Recommended.
     *
     */
    @Supported
    public static final String ALGO_ID_SIGNATURE_RSA = 
                                "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    /**
     * Signature Algorithm RSAwithSHA1 URI.
     *
     */
    @Supported
    public static final String ALGO_ID_SIGNATURE_RSA_SHA1 =
                                "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

    /**
     * Signature Algorithm RSA-MD5 URI.
     *
     */
    @Supported
    public static final String ALGO_ID_SIGNATURE_NOT_RECOMMENDED_RSA_MD5 =
                              "http://www.w3.org/2001/04/xmldsig-more#rsa-md5";

    /**
     * Signature Algorithm RSA-RIPEMD160 URI.
     *
     */
    @Supported
    public static final String ALGO_ID_SIGNATURE_RSA_RIPEMD160 = 
                        "http://www.w3.org/2001/04/xmldsig-more#rsa-ripemd160";

    /**
     * Signature Algorithm RSA-SHA256 URI.
     *
     */
    @Supported
    public static final String ALGO_ID_SIGNATURE_RSA_SHA256 =
                        "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    /**
     * Signature Algorithm RSA-SHA384 URI.
     *
     */
    @Supported
    public static final String ALGO_ID_SIGNATURE_RSA_SHA384 = 
                        "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384";

    /**
     * Signature Algorithm RSA-SHA512 URI.
     *
     */
    @Supported
    public static final String ALGO_ID_SIGNATURE_RSA_SHA512 = 
                        "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

    /**
     * MAC Algorithm HMAC-MD5 URI.
     *
     */
    @Supported
    public static final String ALGO_ID_MAC_HMAC_NOT_RECOMMENDED_MD5 = 
                        "http://www.w3.org/2001/04/xmldsig-more#hmac-md5";

    /**
     * MAC Algorithm HMAC-RIPEMD160 URI.
     *
     */
    @Supported
    public static final String ALGO_ID_MAC_HMAC_RIPEMD160 = 
                       "http://www.w3.org/2001/04/xmldsig-more#hmac-ripemd160";

    /**
     * MAC Algorithm HMAC-SHA256 URI.
     *
     */
    @Supported
    public static final String ALGO_ID_MAC_HMAC_SHA256 = 
                        "http://www.w3.org/2001/04/xmldsig-more#hmac-sha256";

    /**
     * MAC Algorithm HMAC-SHA384 URI.
     *
     */
    @Supported
    public static final String ALGO_ID_MAC_HMAC_SHA384 =
                        "http://www.w3.org/2001/04/xmldsig-more#hmac-sha384";

    /**
     * MAC Algorithm HMAC-SHA512 URI.
     *
     */
    @Supported
    public static final String ALGO_ID_MAC_HMAC_SHA512 = 
                        "http://www.w3.org/2001/04/xmldsig-more#hmac-sha512";

    /**
     * Attribute that identifies server protocol in
     * <code>AMConfig.properties</code> file.
     */
    public static final String SERVER_PROTOCOL =
                        "com.iplanet.am.server.protocol";

    /**
     * Attribute that identifies server host in
     * <code>AMConfig.properties</code> file.
     */
    public static final String SERVER_HOST = "com.iplanet.am.server.host";

    /**
     * Attribute that identifies server port in
     * <code>AMConfig.properties</code> file.
     */
    public static final String SERVER_PORT = "com.iplanet.am.server.port";

    /**
     * Attribute that identifies server port in
     * <code>AMConfig.properties</code> file.
     */
    public static final String SERVER_URI =
        "com.iplanet.am.services.deploymentDescriptor";
   
    /**
     * XML canonicalization Algorithm URI.
     *
     */
    @Supported
    public static final String ALGO_ID_C14N_OMIT_COMMENTS =
                        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";

    /**
     * XML canonicalization with comments Algorithm URI.
     *
     */
    @Supported
    public static final String ALGO_ID_C14N_WITH_COMMENTS =
                        ALGO_ID_C14N_OMIT_COMMENTS + "#WithComments";

    /**
     * Exclusive XML canonicalization Algorithm URI.
     *
     */
    @Supported
    public static final String ALGO_ID_C14N_EXCL_OMIT_COMMENTS =
                        "http://www.w3.org/2001/10/xml-exc-c14n#";

    /**
     * Exclusive XML canonicalization with comments Algorithm URI.
     *
     */
    @Supported
    public static final String ALGO_ID_C14N_EXCL_WITH_COMMENTS =
                        ALGO_ID_C14N_EXCL_OMIT_COMMENTS + "WithComments";
   
    //constants for XML Signature -Transform algorithm
    //supported in Apache xml security package 1.0.5
  
    /**
     * XML canonicalization Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_C14N_OMIT_COMMENTS =
                        ALGO_ID_C14N_OMIT_COMMENTS;

    /**
     * XML canonicalization with comments Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_C14N_WITH_COMMENTS =
                         ALGO_ID_C14N_WITH_COMMENTS;

    /**
     * Exclusive XML canonicalization Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_C14N_EXCL_OMIT_COMMENTS =
                         ALGO_ID_C14N_EXCL_OMIT_COMMENTS;

    /**
     * Exclusive XML canonicalization with comments Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_C14N_EXCL_WITH_COMMENTS =
                         ALGO_ID_C14N_EXCL_WITH_COMMENTS;

    /**
     * XSLT Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_XSLT =
                         "http://www.w3.org/TR/1999/REC-xslt-19991116";

    /**
     * Base64 decoding Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_BASE64_DECODE =
                         XMLSIG_NAMESPACE_URI + "base64";

    /**
     * XPath Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_XPATH =
                         "http://www.w3.org/TR/1999/REC-xpath-19991116";

    /**
     * Enveloped Signature Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_ENVELOPED_SIGNATURE =
                         XMLSIG_NAMESPACE_URI + "enveloped-signature";

    /**
     * XPointer Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_XPOINTER =
                         "http://www.w3.org/TR/2001/WD-xptr-20010108";

    /**
     * XPath Filter v2.0 Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_XPATH2FILTER04 =
                         "http://www.w3.org/2002/04/xmldsig-filter2";

    /**
     * XPath Filter v2.0 Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_XPATH2FILTER =
                         "http://www.w3.org/2002/06/xmldsig-filter2";

    /**
     * XPath Filter v2.0 CHGP Transform URI.
     *
     */
    @Supported
    public static final String TRANSFORM_XPATHFILTERCHGP =
          "http://www.nue.et-inf.uni-siegen.de/~geuer-pollmann/#xpathFilter";

    /**
     * XML schema namespace.
     *
     */
    @Supported
    public static final String NS_XMLNS = "http://www.w3.org/2000/xmlns/";

    /**
     * SOAP security namespace.
     *
     */
    @Supported
    public static final String NS_SEC = "urn:liberty:sec:2003-08";

    /**
     * SOAP utility namespace.
     *
     */
    @Supported
    public static final String NS_WSSE =
                         "http://schemas.xmlsoap.org/ws/2003/06/secext";

    /**
     * Liberty security namespace.
     *
     */
    @Supported
    public static final String NS_WSU =
                        "http://schemas.xmlsoap.org/ws/2003/06/utility";

    /**
     * String that identifies wsu prefix.
     */
    public static final String PREFIX_WSU = "wsu";

    /**
     * String that identifies ds prefix.
     */
    public static final String PREFIX_DS = "ds";

    /**
     * String that identifies tag name "SecurityTokenReference".
     */
    public static final String TAG_SECURITYTOKENREFERENCE =
                        "SecurityTokenReference";

    /**
     * String that identifies tag xmlns.
     */
    public static final String TAG_XMLNS = "xmlns";

    /**
     * String that identifies "xmlns:sec".
     */
    public static final String TAG_XMLNS_SEC = "xmlns:sec";

    /**
     * Usage tag name.
     */
    public static final String TAG_USAGE = "Usage";

    /**
     * MessageAuthentication tag name with namespace prefix.
     */
    public static final String TAG_SEC_MESSAGEAUTHENTICATION =
                        "sec:MessageAuthentication";

    /**
     * Tag name for <code>Security</code>.
     */
    public static final String TAG_SECURITY = "Security";

    /**
     * Tag name for <code>Assertion</code>.
     */
    public static final String TAG_ASSERTION = "Assertion";

    /**
     * String that identifies <code>AssertionID</code>.
     */
    public static final String TAG_ASSERTION_ID = "AssertionID";

    /**
     * Tag name for <code>BinarySecurityToken</code>.
     */
    public static final String BINARYSECURITYTOKEN = "BinarySecurityToken";

    /**
     * Tag name for "Id".
     */
    public static final String TAG_ID = "Id";

    /**
     * Tag name for <code>Reference</code>.
     */
    public static final String TAG_REFERENCE = "Reference";

    /**
     * Tag name for <code>URI</code>.
     */
    public static final String TAG_URI = "URI";

    /**
     * Tag name for <code>ValueType</code>.
     */
    public static final String TAG_VALUETYPE = "ValueType";

    /**
     * Tag name for <code>KeyInfo</code>.
     */
    public static final String TAG_KEYINFO = "KeyInfo";

    /**
     * Tag name for <code>PKCS7</code> with wsse namespace prefix.
     */
    public static final String TAG_PKCS7 = "wsse:PKCS7";

    /**
     * Tag name for <code>X509Data</code>.
     */
    public static final String TAG_X509DATA = "X509Data";

    /**
     * Tag name for <code>X509Certificate</code>.
     */
    public static final String TAG_X509CERTIFICATE = "X509Certificate";

    /**
     * Beginning of certificate string.
     */
    public static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----\n";

    /**
     * End of certificate string.
     */
    public static final String END_CERT    = "\n-----END CERTIFICATE-----";

    /**
     * <code>DSAKeyValue</code> tag name.
     */
    public static final String TAG_DSAKEYVALUE = "DSAKeyValue";

    /**
     * <code>RSAKeyValue</code> tag name.
     */
    public static final String TAG_RSAKEYVALUE = "RSAKeyValue";

    /**
     * Keyname for escaping special characters in <code>AttributeValue</code>.
     * If true, escaping special characters. Otherwise, will not. Default 
     * value is "true". 
     */
    public static final String ESCAPE_ATTR_VALUE = 
        "com.sun.identity.saml.escapeattributevalue";
    
    /**
     * HTTP POST binding.
     */
    public static final String HTTP_POST = "HTTP-POST";

    /**
     * HTTP Redirect binding.
     */
    public static final String HTTP_REDIRECT = "HTTP-Redirect";

    /**
     * Property to identity the HTTP binding for displaying error page.
     */
    public static final String ERROR_PAGE_HTTP_BINDING =
                       "com.sun.identity.saml.errorpage.httpbinding";

    /**
     * Property to identify the error page url.
     */
    public static final String ERROR_PAGE_URL =
                       "com.sun.identity.saml.errorpage.url";
    /**
     * Default error page url.
     */
    public static final String DEFAULT_ERROR_PAGE_URL =
                                "/saml2/jsp/saml2error.jsp";
    /**
     * HTTP status code.
     */
    public static final String HTTP_STATUS_CODE = "httpstatuscode";

    /**
     * Error message.
     */
    public static final String ERROR_MESSAGE = "errormessage";

    /**
     * Error code.
     */
    public static final String ERROR_CODE = "errorcode";

	/**
	 * Accept Language HTTP header
	 */
	public static final String ACCEPT_LANG_HEADER = "Accept-Language";
}
