/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AssertionIDRequestImpl.java,v 1.2 2008/06/25 05:47:59 qcheng Exp $
 *
 * Portions Copyrighted 2018-2021 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AssertionIDRef;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.protocol.AssertionIDRequest;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class represents the AssertionIDRequest element in SAMLv2 protocol
 * schema.
 * <p>The following schema fragment specifies the expected
 * content contained within this java content object.
 * <p>
 * <pre>
 * &lt;element name="AssertionIDRequest" type="{urn:oasis:names:tc:SAML:2.0:protocol}AssertionIDRequestType"/&gt;
 * </pre>
 *
 */
public class AssertionIDRequestImpl extends RequestAbstractImpl
    implements AssertionIDRequest {

    private static final Logger logger = LoggerFactory.getLogger(AssertionIDRequestImpl.class);
    protected List<AssertionIDRef> assertionIDRefs;

    /**
     * Constructor to create <code>AssertionIDRequest</code> Object .
     */
    public AssertionIDRequestImpl() {
        super(SAML2Constants.ASSERTION_ID_REQUEST);
        isMutable = true;
    }

    /**
     * Constructor to create <code>AssertionIDRequest</code> Object.
     *
     * @param element the Document Element Object.
     * @throws SAML2Exception if error creating <code>AssertionIDRequest</code>
     *     Object.
     */
    public AssertionIDRequestImpl(Element element) throws SAML2Exception {
        super(SAML2Constants.ASSERTION_ID_REQUEST);
        parseDOMElement(element);
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
    }

    /**
     * Constructor to create <code>AssertionIDRequest</code> Object.
     *
     * @param xmlString the XML String.
     * @throws SAML2Exception if error creating <code>AssertionIDRequest</code>
     *     Object.
     */
    public AssertionIDRequestImpl(String xmlString) throws SAML2Exception {
        super(SAML2Constants.ASSERTION_ID_REQUEST);
        Document xmlDocument = XMLUtils.toDOMDocument(xmlString
        );
        if (xmlDocument == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "errorObtainingElement"));
        }
        parseDOMElement(xmlDocument.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
        }
    }

    /**
     * Returns a list of <code>AssertionIDRef</code> objects.
     *
     * @return list of <code>AssertionIDRef</code> objects.
     * @see #setAssertionIDRefs(List)
     */
    public List getAssertionIDRefs() {
        return assertionIDRefs;
    }

    /**
     * Sets a list of <code>AssertionIDRef</code> Objects.
     *
     * @param assertionIDRefs the list of <code>AssertionIDRef</code> objects.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAssertionIDRefs
     */
    public void setAssertionIDRefs(List<AssertionIDRef> assertionIDRefs) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "objectImmutable"));
        }
        this.assertionIDRefs = assertionIDRefs;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = super.toDocumentFragment(document, includeNSPrefix, declareNS);
        if (isSigned && signedXMLString != null) {
            return fragment;
        }

        Element rootElement = (Element) fragment.getFirstChild();

        if (declareNS) {
            rootElement.setAttribute("xmlns:saml", SAML2Constants.ASSERTION_NAMESPACE_URI);
        }

        for (AssertionIDRef idRef : assertionIDRefs) {
            rootElement.appendChild(idRef.toDocumentFragment(document, includeNSPrefix, declareNS));
        }

        return fragment;
    }

    protected void validateData() throws SAML2Exception {
        super.validateData();
        if ((assertionIDRefs == null) || (assertionIDRefs.isEmpty())) {
            if (logger.isDebugEnabled()) {
                logger.debug("AssertionIDRequestImpl." +
                "getXMLString: AssertionIDRef is expected");
            }

            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "schemaViolation"));
        }
    }

    /**
     * Parses attributes of the Docuemnt Element for this object.
     *
     * @param element the Document Element of this object.
     * @throws SAML2Exception if error parsing the Document Element.
     */
    protected void parseDOMAttributes(Element element) throws SAML2Exception {
        super.parseDOMAttributes(element);
    }

    /**
     * Parses child elements of the Docuemnt Element for this object.
     *
     * @param iter the child elements iterator.
     * @throws SAML2Exception if error parsing the Document Element.
     */
    protected void parseDOMChileElements(ListIterator iter)
        throws SAML2Exception {

        super.parseDOMChileElements(iter);

        AssertionFactory aFactory = AssertionFactory.getInstance();
        while(iter.hasNext()) {
            Element childElement = (Element)iter.next();
            String localName = childElement.getLocalName() ;
            if (SAML2Constants.ASSERTION_ID_REF.equals(localName)) {
                AssertionIDRef assertionIDRef =
                    aFactory.createAssertionIDRef(childElement);
                if (assertionIDRefs == null) {
                    assertionIDRefs = new ArrayList();
                }
                assertionIDRefs.add(assertionIDRef);
            } else {
                iter.previous();
                break;
            }
        }

        if (assertionIDRefs == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "schemaViolation"));
        }
    }
}
