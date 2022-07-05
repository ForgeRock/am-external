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
 * $Id: SubjectLocalityImpl.java,v 1.2 2008/06/25 05:47:44 qcheng Exp $
 *
 * Portions Copyrighted 2019-2021 ForgeRock AS.
 */



package com.sun.identity.saml2.assertion.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.sun.identity.saml2.assertion.SubjectLocality;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class implements interface <code>SubjectLocality</code>.
 *
 * The <code>SubjectLocality</code> element specifies the DNS domain name
 * and IP address for the system entity that performed the authentication.
 * It exists as part of <code>AuthenticationStatement</code> element.
 * <p>
 * <pre>
 * &lt;complexType name="SubjectLocalityType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attribute name="Address" 
 *       type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="DNSName" 
 *       type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public class SubjectLocalityImpl implements SubjectLocality {

    private static final Logger logger = LoggerFactory.getLogger(SubjectLocalityImpl.class);
    private String address = null;
    private String dnsName = null;
    private boolean mutable = true;

    // used by the constructors.
    private void parseElement(org.w3c.dom.Element element)
                throws SAML2Exception
    {
        // make sure that the input xml block is not null
        if (element == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("SubjectLocalityImpl.parseElement: "
                    + "Input element is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is a SubjectLocality.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("SubjectLocality"))) {
            if (logger.isDebugEnabled()) {
                logger.debug("SubjectLocalityImpl.parseElement: "
                    + "input is not SubjectLocality");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the attribute of <SubjectLocality> element
        NamedNodeMap atts = ((Node)element).getAttributes();
        if (atts != null) {
            Node att = atts.getNamedItem("Address");
            if (att != null) {
                address = ((Attr)att).getValue().trim();
            }
            att = atts.getNamedItem("DNSName");
            if (att != null) {
                dnsName = ((Attr)att).getValue().trim();
            }

        }
        mutable = false;
    }

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public SubjectLocalityImpl() {
    }

    /**
     * Class constructor with <code>SubjectLocality</code> in
     * <code>Element</code> format.
     */
    public SubjectLocalityImpl(org.w3c.dom.Element element)
        throws SAML2Exception
    {
        parseElement(element);
    }

    /**
     * Class constructor with <code>SubjectLocality</code> in xml string format.
     */
    public SubjectLocalityImpl(String xmlString)
        throws SAML2Exception
    {
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
    }


    /**
     * Makes the object immutable.
     */
    public void makeImmutable() {
        mutable = false;
    }

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable; 
     *                <code>false</code> otherwise.
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Returns the value of the <code>DNSName</code> attribute.
     *
     * @return the value of the <code>DNSName</code> attribute.
     * @see #setDNSName(String)
     */
    public String getDNSName() {
        return dnsName;
    }

    /**
     * Sets the value of the <code>DNSName</code> attribute.
     *
     * @param value new value of the <code>DNSName</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getDNSName()
     */
    public void setDNSName(String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        dnsName = value;
    }

    /**
     * Returns the value of the <code>Address</code> attribute.
     *
     * @return the value of the <code>Address</code> attribute.
     * @see #setAddress(String)
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the value of the <code>Address</code> attribute.
     *
     * @param value new value of <code>Address</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAddress()
     */
    public void setAddress(String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        address = value;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        Element subjectLocalityElement = XMLUtils.createRootElement(document, ASSERTION_PREFIX,
                ASSERTION_NAMESPACE_URI, "SubjectLocality", includeNSPrefix, declareNS);
        fragment.appendChild(subjectLocalityElement);

        if (isNotBlank(address)) {
            subjectLocalityElement.setAttribute("Address", address);
        }
        if (isNotBlank(dnsName)) {
            subjectLocalityElement.setAttribute("DNSName", dnsName);
        }

        return fragment;
    }
}
