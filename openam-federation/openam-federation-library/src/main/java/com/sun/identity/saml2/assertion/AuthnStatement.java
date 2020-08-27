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
 * $Id: AuthnStatement.java,v 1.2 2008/06/25 05:47:40 qcheng Exp $
 *
 * Portions Copyrighted 2015-2019 ForgeRock AS.
 */

package com.sun.identity.saml2.assertion;

import java.util.Date;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sun.identity.saml2.assertion.impl.AuthnStatementImpl;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The <code>AuthnStatement</code> element describes a statement by the
 * SAML authority asserting that the assertion subject was authenticated
 * by a particular means at a particular time. It is of type 
 * <code>AuthnStatementType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="AuthnStatementType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:assertion}
 *     StatementAbstractType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         SubjectLocality" minOccurs="0"/&gt;
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         AuthnContext"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="AuthnInstant" use="required"
 *       type="{http://www.w3.org/2001/XMLSchema}dateTime" /&gt;
 *       &lt;attribute name="SessionIndex"
 *       type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="SessionNotOnOrAfter"
 *       type="{http://www.w3.org/2001/XMLSchema}dateTime" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@SupportedAll
@JsonDeserialize(as=AuthnStatementImpl.class)
public interface AuthnStatement extends Statement {

    /**
     * Returns the value of the <code>AuthnContext</code> property.
     *
     * @return <code>AuthnContext</code> of the statement.
     * @see #setAuthnContext(AuthnContext)
     */
    AuthnContext getAuthnContext();

    /**
     * Sets the value of the <code>AuthnContext</code> property.
     *
     * @param value new <code>AuthnContext</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAuthnContext()
     */
    void setAuthnContext(AuthnContext value)
        throws SAML2Exception;

    /**
     * Returns the value of the <code>AuthnInstant</code> attribute.
     *
     * @return the value of the <code>AuthnInstant</code> attribute.
     * @see #setAuthnInstant(Date)
     */
    Date getAuthnInstant();

    /**
     * Sets the value of the <code>AuthnInstant</code> attribute.
     *
     * @param value new value of <code>AuthnInstant</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAuthnInstant()
     */
    void setAuthnInstant(Date value)
        throws SAML2Exception;

    /**
     * Returns the value of the <code>SubjectLocality</code> property.
     *
     * @return <code>SubjectLocality</code> of the statement.
     * @see #setSubjectLocality(SubjectLocality)
     */
    SubjectLocality getSubjectLocality();

    /**
     * Sets the value of the <code>SubjectLocality</code> property.
     *
     * @param value the new value of <code>SubjectLocality</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getSubjectLocality()
     */
    void setSubjectLocality(SubjectLocality value)
        throws SAML2Exception;

    /**
     * Returns the value of the <code>SessionIndex</code> attribute.
     *
     * @return the value of the <code>SessionIndex</code> attribute.
     * @see #setSessionIndex(String)
     */
    String getSessionIndex();

    /**
     * Sets the value of the <code>SessionIndex</code> attribute.
     *
     * @param value new value of <code>SessionIndex</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getSessionIndex()
     */
    void setSessionIndex(String value)
        throws SAML2Exception;

    /**
     * Returns the value of the <code>SessionNotOnOrAfter</code> attribute.
     *
     * @return the value of <code>SessionNotOnOrAfter</code> attribute.
     * @see #setSessionNotOnOrAfter(Date)
     */
    Date getSessionNotOnOrAfter();

    /**
     * Sets the value of the <code>SessionNotOnOrAfter</code> attribute.
     *
     * @param value new <code>SessionNotOnOrAfter</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getSessionNotOnOrAfter()
     */
    void setSessionNotOnOrAfter(Date value)
        throws SAML2Exception;
}
