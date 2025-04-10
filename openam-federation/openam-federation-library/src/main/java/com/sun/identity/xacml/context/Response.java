/**
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
 * $Id: Response.java,v 1.2 2008/06/25 05:48:12 qcheng Exp $
 *
 * Portions Copyrighted 2019-2025 Ping Identity Corporation.
 */
package com.sun.identity.xacml.context;

import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.forgerock.openam.annotations.SupportedAll;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.common.XACMLException;

/**
 * The <code>Response</code> element is a container of 
 * one or more <code>Result</code>s issued by policy decision point
 */
@SupportedAll
public interface Response extends XmlSerializable {

    /**
     * Returns the <code>Result</code>s of this object
     *
     * @return the <code>Result</code>s of this object
     */
    public List getResults();

    /**
     * Adds a <code>Result</code> to this object
     *
     * @param result the <code>Result</code> to add
     *
     * @exception XACMLException if the object is immutable
     */
    public void addResult(Result result) throws XACMLException;

    /**
     * Sets the <code>Result</code>s of this object
     *
     * @param results the <code>Result</code>s of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setResults(List results) throws XACMLException;

    /**
     * Returns a string representation
     *
     * @return a string representation
     * @exception XACMLException if conversion fails for any reason
     */
    default String toXMLString() throws XACMLException {
        return toXMLString(true, false);
    }

    /**
     * Returns a string representation
     * @param includeNSPrefix Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return a string representation
     * @exception XACMLException if conversion fails for any reason
     */
    default String toXMLString(boolean includeNSPrefix, boolean declareNS)
            throws XACMLException {
        try {
            Document document = XMLUtils.newDocument();
            DocumentFragment fragment = toDocumentFragment(document, includeNSPrefix, declareNS);
            return XMLUtils.print(fragment);
        } catch (ParserConfigurationException | SAML2Exception e) {
            throw new XACMLException(e);
        }
    }

   /**
    * Checks if the object is mutable
    *
    * @return <code>true</code> if the object is mutable,
    *         <code>false</code> otherwise
    */
    public boolean isMutable();
    
   /**
    * Makes the object immutable
    */
    public void makeImmutable();

}

