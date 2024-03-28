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
 * $Id: NameIDImplWithoutSPNameQualifier.java,v 1.2 2008/06/25 05:47:44 qcheng Exp $
 *
 * Portions Copyrighted 2015-2023 ForgeRock AS.
 */

package com.sun.identity.saml2.assertion.impl;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 *  The <code>NameID</code> is used in various SAML assertion constructs
 *  such as <code>Subject</code> and <code>SubjectConfirmation</code>
 *  elements, and in various protocol messages.
 */
@SupportedAll(scriptingApi = true, javaApi = false)
public class NameIDImplWithoutSPNameQualifier extends NameIDImpl
    implements NameID {

    private static final Logger logger = LoggerFactory.getLogger(NameIDImplWithoutSPNameQualifier.class);

    /** 
     * Default constructor
     */
    public NameIDImplWithoutSPNameQualifier() {
    }

    /**
     * This constructor is used to build <code>NameID</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>NameID</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public NameIDImplWithoutSPNameQualifier(String xml) throws SAML2Exception {
        super(xml);
    }

    /**
     * This constructor is used to build <code>NameID</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>NameID</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public NameIDImplWithoutSPNameQualifier(Element element) 
        throws SAML2Exception 
    {
        super(element);
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = super.toDocumentFragment(document, includeNSPrefix, declareNS);

        Element nameIdElement = (Element) fragment.getFirstChild();
        nameIdElement.removeAttribute(SP_NAME_QUALIFIER_ATTR);

        return fragment;
    }
}
