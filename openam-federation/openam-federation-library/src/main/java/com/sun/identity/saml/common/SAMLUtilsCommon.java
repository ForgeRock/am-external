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
 * $Id: SAMLUtilsCommon.java,v 1.4 2008/11/10 22:57:00 veiming Exp $
 *
 * Portions Copyrighted 2019-2020 ForgeRock AS.
 */
package com.sun.identity.saml.common;

import java.security.SecureRandom;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml.xmlsig.PasswordDecoder;
import com.sun.identity.shared.locale.Locale;

/**
 * This class contains a set of generic common utility methods.
 *
 */
public class SAMLUtilsCommon {
    private static final Logger logger = LoggerFactory.getLogger(SAMLUtilsCommon.class);

    /**
     * <code>SecureRandom</code> instance.
     */
    public static SecureRandom random = new SecureRandom();

    /**
     * Prefix for ids used in SAML service.
     */
    public static final String SAMLID_PREFIX = "s";

    /**
     * SAML resource bundle object.
     */
    public static ResourceBundle bundle = Locale.getInstallResourceBundle("libSAML");

    /**
     * Generates an ID String with length of SAMLConstants.ID_LENGTH.
     * @return string the ID String; or null if it fails.
     */
    public static String generateID() {
        if (random == null) {
            return null;
        }
        byte[] bytes = new byte[SAMLConstants.ID_LENGTH];
        random.nextBytes(bytes);
        String encodedID = SAMLID_PREFIX + byteArrayToHexString(bytes);
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "SAMLUtils.generated ID is: " + encodedID);
        }

        return encodedID;
    }

    /**
     * Converts a byte array to a hex string.
     */
    private static String byteArrayToHexString(byte[] byteArray) {
        StringBuilder hexData = new StringBuilder();
        int onebyte;
        for (byte b : byteArray) {
            onebyte = ((0x000000ff & b) | 0xffffff00);
            hexData.append(Integer.toHexString(onebyte).substring(6));
        }
        return hexData.toString();
    }

    /**
     * Generates end element tag.
     * It takes in the name of element and produces a String 
     * as output which is in XML format. For example given
     * "SubjectConfirmation", It produces output like
     * &lt;/saml:SubjectConfirmation&gt;
     * if includeNS is  true  else produces &lt;/SubjectConfirmation&gt;
     * @param elementName name of an element
     * @param includeNS true to include namespace prefix; false otherwise.
     * @return String which is an xml element end tag.
     */
    public static String makeEndElementTagXML(String elementName, 
                        boolean includeNS) 
    {

        StringBuilder xml = new StringBuilder(100);
        String appendNS="";
        if (includeNS)  {
            appendNS="saml:";
        }
        xml.append(SAMLConstants.START_END_ELEMENT).append(appendNS).append(elementName).
            append(SAMLConstants.RIGHT_ANGLE).append(SAMLConstants.NL);
        return xml.toString();
    }

    /**
     * Generates xml element start tag.
     * This utility method takes in the name fo element and produces a
     * String as output which is in XML format. For example given
     * "SubjectConfirmation". It produces output like
     * &lt;saml:SubjectConfirmation xmlns:saml=
     * "http://www.oasis-open.org/committees/security/docs/
     * draft-sstc-schema-assertion-16.xsd"&gt; where nameSpace is defined in 
     * <code>AssertionBase</code> class if declareNS and includeNS are true.
     * @param elementName name of the element.
     * @param includeNS true to include namespace prefix; false otherwise.
     * @param declareNS true to include namespace declaration; false otherwise.
     * @return xml element start tag.
     */
    public static String makeStartElementTagXML(String elementName, 
        boolean includeNS, boolean declareNS) 
    {
        StringBuilder xml = new StringBuilder(1000);
        String appendNS="";
        String NS="";
        if (includeNS) {
            appendNS="saml:";
        }
        if (declareNS)  {
            NS = SAMLConstants.assertionDeclareStr;
        }
        xml.append(SAMLConstants.LEFT_ANGLE).append(appendNS).append(elementName).
            append(NS).append(SAMLConstants.RIGHT_ANGLE);
        return xml.toString();
    }

    /**
     * Verifies if an element is a type of a specific statement.
     * Currently, this method is used by class AuthenticationStatement, AuthorizationDecisionStatement and
     * AttributeStatement.
     *
     * @param element a DOM Element which needs to be verified.
     * @param statementName A specific name of a statement, for example, AuthenticationStatement,
     * AuthorizationDecisionStatement or AttributeStatement
     * @return true if the element is of the specified type; false otherwise.
     */
    public static boolean checkStatement(Element element, String statementName){
        String tag = element.getLocalName();
        if (tag == null) {
            return false;
        } else if (tag.equals("Statement") || tag.equals("SubjectStatement")) {
            NamedNodeMap nm = element.getAttributes();
            int len = nm.getLength();
            String attrName;
            Attr attr;
            for (int j = 0; j < len; j++) {
                attr = (Attr) nm.item(j);
                attrName = attr.getLocalName();
                if (attrName != null && attrName.equals("type") && attr.getNodeValue().equals(statementName + "Type")) {
                    return true;
                }
            }
            return false;
        } else {
            return tag.equals(statementName);
        }
    }
    
    /**
     * Decodes a password. 
     * The value passed is the value to be decoded using the decoder class
     * defined in FederationConfig.properties. The decoded value
     * will be returned unless the decoder class is not defined, or cannot
     * be located. In that case, the original value will be returned.
     *
     * @param password original password.
     * @return decoded password.
     */
    public static String decodePassword(String password)  {
        String decodePwdSpi = SystemConfigurationUtil.getProperty("com.sun.identity.saml.xmlsig.passwordDecoder",
                "com.sun.identity.saml.xmlsig.FMPasswordDecoder");
        String decoPasswd;
        try { 
            PasswordDecoder pwdDecoder = Class.forName(decodePwdSpi)
                    .asSubclass(PasswordDecoder.class)
                    .getDeclaredConstructor()
                    .newInstance();
            decoPasswd = pwdDecoder.getDecodedPassword(password);
        } catch (Throwable t) {
            decoPasswd = password;
        }                   
        return decoPasswd;                     
    }
    
    /**
     * Removes new line charactors.
     * @param s A String to be checked.
     * @return a String with new line charactor removed.
     */
    public static String removeNewLineChars(String s) {
        String retString;
        if ((s != null) && (s.length() > 0) && (s.indexOf('\n') != -1)) {
            char[] chars = s.toCharArray();
            int len = chars.length;
            StringBuilder sb = new StringBuilder(len);
            for (char c : chars) {
                if (c != '\n') {
                    sb.append(c);
                }
            }
            retString = sb.toString();
        } else {
            retString = s;
        }
        return retString;
    }
}
