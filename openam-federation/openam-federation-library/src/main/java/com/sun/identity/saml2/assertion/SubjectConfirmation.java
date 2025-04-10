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
 * $Id: SubjectConfirmation.java,v 1.2 2008/06/25 05:47:42 qcheng Exp $
 *
 * Portions Copyrighted 2018-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.assertion;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sun.identity.saml2.assertion.impl.SubjectConfirmationImpl;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;

/**
 *  The <code>SubjectConfirmation</code> provides the means for a relying 
 *  party to verify the correspondence of the subject of the assertion
 *  with the party with whom the relying party is communicating.
 *
 */
 @SupportedAll
@JsonDeserialize(as=SubjectConfirmationImpl.class)
public interface SubjectConfirmation extends XmlSerializable {

    /**
     * Returns the encrypted ID
     *
     * @return the encrypted ID 
     */
    EncryptedID getEncryptedID();

    /**
     * Sets the encrypted ID
     *
     * @param value the encrypted ID 
     * @exception SAML2Exception if the object is immutable
     */
    void setEncryptedID(EncryptedID value) throws SAML2Exception;

    /**
     * Returns the subject confirmation data
     *
     * @return the subject confirmation data 
     */
    SubjectConfirmationData getSubjectConfirmationData();

    /**
     * Sets the subject confirmation data
     *
     * @param value the subject confirmation data 
     * @exception SAML2Exception if the object is immutable
     */
    void setSubjectConfirmationData(SubjectConfirmationData value)
        throws SAML2Exception;

    /**
     * Returns the name identifier 
     *
     * @return the name identifier 
     */
    NameID getNameID();

    /**
     * Sets the name identifier 
     *
     * @param value the name identifier 
     * @exception SAML2Exception if the object is immutable
     */
    void setNameID(NameID value) throws SAML2Exception;

    /**
     * Returns the base ID 
     *
     * @return the base ID 
     */
    BaseID getBaseID();

    /**
     * Sets the base ID 
     *
     * @param value the base ID 
     * @exception SAML2Exception if the object is immutable
     */
    void setBaseID(BaseID value) throws SAML2Exception;

    /**
     * Returns the confirmation method 
     *
     * @return the confirmation method 
     */
    String getMethod();

    /**
     * Sets the confirmation method 
     *
     * @param value the confirmation method 
     * @exception SAML2Exception if the object is immutable
     */
    void setMethod(String value) throws SAML2Exception;

   /**
    * Makes the object immutable
    */
   void makeImmutable();

   /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
   boolean isMutable();

}
