/**
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
 * $Id: BinarySecurityToken.java,v 1.5 2008/08/06 17:28:07 exu Exp $
 *
 * Portions Copyrighted 2018-2025 Ping Identity Corporation.
 */


package com.sun.identity.liberty.ws.common.wsse;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;

import org.forgerock.openam.annotations.Supported;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import com.sun.identity.liberty.ws.soapbinding.SOAPBindingConstants;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The class <code>BinarySecurityToken</code> provides interface to parse and
 * create X.509 Security Token depicted by Web Service Security : X.509 
 * Certificate Token Profile and Liberty ID-WSF Security Mechanisms 
 * specifications.
 *  <p>The following schema fragment specifies the expected content within the BinarySecurityToken object.
 * <p>
 * <pre>
 * &lt;element name="BinarySecurityToken" type="wsse:BinarySecurityTokenType/&gt;
 * &lt;complexType name="BinarySecurityTokenType"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://schemas.xmlsoap.org/ws/2003/06/secext&gt;EncodedString"&gt;
 *       &lt;attribute name="ValueType" type="{http://www.w3.org/2001/XMLSchema}QName" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * &lt;xsd:complexType name="EncodedString"&gt;
 *   &lt;xsd:simpleContent&gt;
 *     &lt;xsd:extension base="wsse:AttributedString"&gt;
 *       &lt;xsd:attribute name="EncodingType" type="xsd:QName"/&gt;
 *     &lt;/xsd:extension&gt;
 *   &lt;/xsd:simpleContent&gt;
 * &lt;/xsd:complexType&gt;
 * &lt;xsd:complexType name="AttributedString"&gt;
 *   &lt;xsd:simpleContent&gt;
 *     &lt;xsd:extension base="xsd:string"&gt;
 *       &lt;xsd:attribute ref="wsu:Id"/&gt;
 *         &lt;xsd:anyAttribute namespace="##other" processContents="lax"/&gt;
 *     &lt;/xsd:extension&gt;
 *   &lt;/xsd:simpleContent&gt;
 * &lt;/xsd:complexType&gt;
 * </pre>
 * 
 */
@Supported
public class BinarySecurityToken {

    private String value = null;
    private QName valueType = null;
    private QName encodingType = null;
    private String id = null;
    private String xmlString = null;
    private static ResourceBundle bundle = Locale.getInstallResourceBundle(
                                            "libBinarySecurityToken");
    private static Logger debug = LoggerFactory.getLogger(BinarySecurityToken.class);

    private static final String WSSE = "wsse";
    private static final String WSU = "wsu";
    private static final String BINARY_SECURITY_TOKEN = "BinarySecurityToken";
    private static final String ENCODING_TYPE = "EncodingType";
    private static final String VALUE_TYPE = "ValueType";
    private static final String ID = "Id";
    private static final String XML_NS = "xmlns";

    private String wsfVersion = null;
    private String wsseNS = null;
    private String wsuNS = null;

    /**
     * Constructor.
     * @param token Binary Security Token Element
     * @exception Exception if token Element is not a valid binary 
     *     security token 
     */
    @Supported
    public BinarySecurityToken(Element token) 
        throws Exception {
        if (token == null) {
            debug.error("BinarySecurityToken: null input token");
            throw new Exception(bundle.getString("nullInputParameter")) ;
        }

        // check element name            
        String elementName = token.getLocalName();
        if (elementName == null)  {
            debug.error("BinarySecurityToken: local name missing");
            throw new Exception(bundle.getString("nullInput")) ;
        }
        if (!(elementName.equals(BINARY_SECURITY_TOKEN)))  {
            debug.error("BinarySecurityToken: invalid root element");
            throw new Exception(bundle.getString("invalidElement") + 
                ":" + elementName) ;   
        }

        wsseNS = token.getNamespaceURI();
        if ((wsseNS != null) && WSSEConstants.NS_WSSE_WSF11.equals(wsseNS)) {
            wsfVersion = SOAPBindingConstants.WSF_11_VERSION;
        } else if((wsseNS != null) &&  WSSEConstants.NS_WSSE.equals(wsseNS)) {
            wsfVersion = SOAPBindingConstants.WSF_10_VERSION;
        } else {
            throw new Exception(bundle.getString("invalidNameSpace"));
        }

        // check attributes
        NamedNodeMap nm = token.getAttributes();
        if (nm == null) {
            debug.error("BinarySecurityToken: missing attr in element");
            throw new Exception(bundle.getString("missingAttribute"));
        }
        int len = nm.getLength();
        for (int i = 0; i < len; i++) {
            Attr attr = (Attr) nm.item(i);
            String localName = attr.getLocalName();
            if (localName == null) {
                // exception?? Ignore for now
                if (debug.isDebugEnabled()) {
                    debug.debug("BST.Elemement, invalid attr " + localName);
                } 
                continue;
            }
            // check Id/EncodingType/ValueType attribute
            if (localName.equals(ID)) {
                this.id = attr.getValue();
                wsuNS = attr.getNamespaceURI();
            } else if (localName.equals(ENCODING_TYPE)) {
                // no namespace match done here
                encodingType = (QName) 
                    encodingMap.get(trimPrefix(attr.getValue()));
            } else if (localName.equals(VALUE_TYPE)) {
                // no namespace match done here
                valueType = (QName) valueMap.get(trimPrefix(attr.getValue()));
            }
        }
 
        if (id == null || id.length() == 0) {
            debug.error("BinarySecurityToken: ID missing");
            throw new Exception(bundle.getString("missingAttribute") +
                    " : " + ID);
        }

        if (encodingType == null) {
            debug.error("BinarySecurityToken: encoding type missing");
            throw new Exception(bundle.getString("missingAttribute") +
                " : " + ENCODING_TYPE);
        }

        if (valueType == null) {
            debug.error("BinarySecurityToken: valueType missing");
            throw new Exception(bundle.getString("missingAttribute") +
            " : " + VALUE_TYPE);
        }

        // get X509 certificate value
        try {
            this.value = token.getFirstChild().getNodeValue().trim();
        } catch (Exception e) {
            debug.error("BinarySecurityToken: unable to get value", e);
            this.value = null;
        }

        if (value == null) {
            debug.error("BinarySecurityToken: value missing");
            throw new Exception(bundle.getString("missingValue"));
        }
 
        // save the original string for toString()
        xmlString = XMLUtils.print(token);

    }

    /**
     * trim prefix and get the value, e.g, for wsse:X509v3 will return X509v3 
     */
    private String trimPrefix(String val) {
        if ((val != null) &&
            (val.startsWith(WSSEConstants.NS_X509) ||
            val.startsWith(WSSEConstants.NS_SMS))) {
            return val;
        }

        int pos = val.indexOf(":");
        if (pos == -1) {
            return val;
        } else if (pos == val.length()) {
            return "";
        } else {
            return val.substring(pos+1);
        } 
    }

    /**
     * Gets encoding type for the token.
     *
     * @return encoding type for the token. 
     */
    @Supported
    public QName getEncodingType() {
        return encodingType;
    }

    /**
     * Gets value type for the token.
     *
     * @return value type for the token. 
     */
    @Supported
    public QName getValueType() {
        return valueType;
    }

    /**
     * Gets id attribute for the tokens.
     *
     * @return id attribute for the token.
     */
    @Supported
    public String getId() {
        return id;
    }

    /**
     * Gets value of the token.
     *
     * @return value of the token.
     */
    @Supported
    public String getTokenValue() { 
        return value;
    }

    /**
     * Returns a String representation of the token.
     * @return A string containing the valid XML for this element
     */
    @Supported
    public String toString() {
        if (xmlString == null) {
            StringBuffer sb = new StringBuffer();

            sb.append("<").append(WSSE).append(":")
                .append(BINARY_SECURITY_TOKEN).append(" ").append(XML_NS)
                .append(":").append(WSSE).append("=\"").append(wsseNS)
                .append("\" ").append(XML_NS).append(":").append(WSU)
                .append("=\"").append(wsuNS).append("\" ").append(WSU)
                .append(":").append(ID).append("=\"").append(id).append("\" ")
                .append(VALUE_TYPE).append("=\"");
            if (SOAPBindingConstants.WSF_11_VERSION.equals(wsfVersion)) {
                sb.append(WSSEConstants.NS_X509).append("#")
                    .append(valueType.getLocalPart()).append("\" ")
                    .append(ENCODING_TYPE).append("=\"")
                    .append(WSSEConstants.NS_SMS).append("#")
                    .append(encodingType.getLocalPart()).append("\">\n");
            } else {
                sb.append(WSSE).append(":").append(valueType.getLocalPart())
                    .append("\" ").append(ENCODING_TYPE).append("=\"")
                    .append(WSSE).append(":")
                    .append(encodingType.getLocalPart()).append("\">\n");
            }
            sb.append(value.toString()).append("\n").append("</").append(WSSE)
                .append(":").append(BINARY_SECURITY_TOKEN).append(">\n");
            xmlString = sb.toString();
        }
        return xmlString;
    }

    /**
     * The <code>X509V3</code> value type indicates that
     * the value name given corresponds to a X509 Certificate.
     */
    @Supported
    public static final QName X509V3 = new QName("X509v3");

    /**
     * The <code>KERBEROSV5TGT</code> value type indicates that
     * the value name given corresponds to a Kerberos V5 TGT.
     */
    public static final QName KERBEROSV5TGT = new QName("Kerberosv5TGT");

    /**
     * The <code>KERBEROSV5ST</code> value type indicates
     * that the value name given corresponds to a Kerberos V5 service ticket.
     */
    public static final QName KERBEROSV5ST = new QName("Kerberosv5ST");

    /**
     * The <code>PKCS7</code> value type indicates
     * that the value name given corresponds to a
     * PKCS7 object.
     */
    @Supported
    public static final QName PKCS7 = new QName("PKCS7");

    /**
     * The <code>PKIPATH</code> value type indicates
     * that the value name given corresponds to a
     * PKI Path object.
     */
    @Supported
    public static final QName PKIPath = new QName("PKIPath");

    // map from string to ValueType object
    static Map valueMap = new HashMap(); 
    static {
        valueMap.put(X509V3.getLocalPart(), X509V3);
        valueMap.put(PKIPath.getLocalPart(), PKIPath);
        valueMap.put(PKCS7.getLocalPart(), PKCS7);
        valueMap.put(KERBEROSV5ST.getLocalPart(), KERBEROSV5ST);
        valueMap.put(KERBEROSV5TGT.getLocalPart(), KERBEROSV5TGT);
        valueMap.put(WSSEConstants.NS_X509 + "#X509v3", X509V3);
    }

    /** 
     * The <code>BASE64BINARY</code> encoding type indicates that the encoding
     * name given corresponds to base64 encoding of a binary value.
     */
    @Supported
    public static final QName BASE64BINARY = new QName("Base64Binary");
        
    /**
     * The <code>HEXBINARY</code> encoding type indicates that
     * the encoding name given corresponds to Hex encoding of
     * a binary value.
     */
    @Supported
    public static final QName HEXBINARY = new QName("HexBinary");
        
    // map from string to EncodingType object 
    static Map encodingMap = new HashMap(); 
    static {
        encodingMap.put(HEXBINARY.getLocalPart(), HEXBINARY);
        encodingMap.put(BASE64BINARY.getLocalPart(), BASE64BINARY);
        encodingMap.put(WSSEConstants.NS_SMS + "#Base64Binary", BASE64BINARY);
    } 

    /**
     * Adds th binary security token to the header element.
     * @param headerE the security header element.
     * @exception Exception if there is a failure in adding to the header.
     */
    public void addToParent(Element headerE) throws Exception {

         Document doc = headerE.getOwnerDocument();
         Element securityE = doc.createElementNS(wsseNS,
                 WSSEConstants.TAG_WSSE + ":" +
                 WSSEConstants.TAG_SECURITYT);
         securityE.setAttributeNS(SOAPBindingConstants.NS_XML,
                    WSSEConstants.TAG_XML_WSSE, wsseNS);
         headerE.appendChild(securityE);

         Document binaryTokenD = XMLUtils.toDOMDocument(toString());

         Element binaryTokenE = binaryTokenD.getDocumentElement();
         securityE.appendChild(doc.importNode(binaryTokenE, true));

    }

    /**
     * Returns the web services version.
     * @return the web services version.
     */
    public String getWSFVersion() {
        return wsfVersion;
    }

    /**
     * Sets the web services version.
     * @param version the web services version.
     */
    public void setWSFVersion(String version) {
        this.wsfVersion = version;
        if(wsfVersion != null &&
                 SOAPBindingConstants.WSF_10_VERSION.equals(wsfVersion)) {
           wsseNS = WSSEConstants.NS_WSSE;
           wsuNS = WSSEConstants.NS_WSU;
        } else {
           wsseNS = WSSEConstants.NS_WSSE_WSF11;
           wsuNS = WSSEConstants.NS_WSU_WSF11;
        }
        xmlString = null;
    }

}
