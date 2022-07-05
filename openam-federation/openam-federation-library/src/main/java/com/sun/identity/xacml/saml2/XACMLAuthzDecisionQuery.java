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
 * $Id: XACMLAuthzDecisionQuery.java,v 1.3 2008/06/25 05:48:14 qcheng Exp $
 *
 * Portions Copyrighted 2017-2021 ForgeRock AS.
 */
package com.sun.identity.xacml.saml2;

import org.forgerock.openam.annotations.SupportedAll;

import com.sun.identity.saml2.protocol.RequestAbstract;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.context.Request;

/**
 * The <code>XACMLAuthzDecisionQuery</code> element is a SAML Query that 
 * extends SAML Protocol schema type <code>RequestAbstractType</code>.
 * It allows an XACML PEP to submit an XACML Request Context in a  SAML
 * Query along with other information. This element is an alternative to 
 * SAML defined <code>&lt;samlp:AuthzDecisionQuery&gt;</code> that allows an
 * XACML PEP  to communicate with an XACML PDP using SAML2 protocol.
 * <p>
 * Schema:
 * <pre>
 *&lt;xs:element name="XACMLAuthzDecisionQuery"
 *         type="XACMLAuthzDecisionQueryType"/&gt;
 *&lt;xs:complexType name="XACMLAuthzDecisionQueryType"&gt;
 *  &lt;xs:complexContent&gt;
 *    &lt;xs:extension base="samlp:RequestAbstractType"&gt;
 *      &lt;xs:sequence&gt;
 *        &lt;xs:element ref="xacml-context:Request"/&gt;
 *      &lt;xs:sequence&gt;
 *      &lt;xs:attribute name="InputContextOnly"
 *                    type="boolean"
 *                    use="optional"
 *                    default="false"/&gt;
 *      &lt;xs:attribute name="ReturnContext"
 *                    type="boolean"
 *                    use="optional"
 *                    default="false"/&gt;
 *    &lt;xs:extension&gt;
 *  &lt;xs:complexContent&gt;
 *&lt;xs:complexType&gt;
 * </pre>
 *
 * Schema for base:
 * <pre>
 *  &lt;complexType name="RequestAbstractType" abstract="true"&gt;
 *      &lt;sequence&gt;
 *          &lt;element ref="saml:Issuer" minOccurs="0"/&gt;
 *          &lt;element ref="ds:Signature" minOccurs="0"/&gt;
 *          &lt;element ref="samlp:Extensions" minOccurs="0"/&gt;
 *      &lt;sequence&gt;
 *      &lt;attribute name="ID" type="ID" use="required"/&gt;
 *      &lt;attribute name="Version" type="string" use="required"/&gt;
 *      &lt;attribute name="IssueInstant" type="dateTime" use="required"/&gt;
 *      &lt;attribute name="Destination" type="anyURI" use="optional"/&gt;
 *  	&lt;attribute name="Consent" type="anyURI" use="optional"/&gt;
 *  &lt;complexType&gt;
 * </pre>
 *
 */
@SupportedAll
public interface XACMLAuthzDecisionQuery extends RequestAbstract {

    /**
     * Returns the XML attribute boolean value which governs the
     * source of information that the PDP is allowed to use in 
     * making an authorization decision. If this attribute is "true"
     * then it indiactes that the authorization decision has been made 
     * solely on the basis of information contained in the <code>
     * XACMLAuthzDecisionQuery</code>; no external attributes have been
     * used. If this value is "false" then the decision may have been made
     * on the basis of external attributes not conatined in the <code>
     * XACMLAuthzDecisionQuery</code>.
     * @return <code>boolean</code> indicating the value
     * of this attribute.
     */
    public boolean getInputContextOnly();

    /**
     * Sets the XML attribute boolean value which governs the
     * source of information that the PDP is allowed to use in
     * making an authorization decision. If this attribute is "true"
     * then it indicates to the PDP  that the authorization decision has to be
     * made solely on the basis of information contained in the <code>
     * XACMLAuthzDecisionQuery</code>; no external attributes may be 
     * used. If this value is "false" then the decision can be  made
     * on the basis of external attributes not conatined in the <code>
     * XACMlAuthzDecisionQuery</code>.
     * @param inputContextOnly <code>boolean</code> indicating the value
     * of this attribute.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setInputContextOnly(boolean inputContextOnly) throws 
            XACMLException;

    /**
     * Returns the XML attribute boolean value which provides means
     * to PEP to request that an <code>xacml-context:Request</code>
     * element be included in the <code>XACMlAuthzDecisionStatement</code>
     * resulting from the request. It also governs the contents of that
     * <code>Request</code> element. If this attribute is "true" then the
     * PDP SHALL include the <code>xacml-context:Request</code> element in the
     * <code>XACMLAuthzDecisionStatement</code> element in the 
     * <code>XACMLResponse</code>.  The <code>xacml-context:Request</code> SHALL
     * include all the attributes supplied by the PEP in the
     * <code>AuthzDecisionQuery</code> which were used in making 
     * the authz decision. Other additional attributes which may have been used
     * by the PDP may be included.
     * If this attribute is "false" then the PDP SHALL NOT include the 
     * <code>xacml-context:Request</code> element in the
     * <code>XACMLAuthzDecisionStatement</code>.
     * 
     * @return <code>boolean</code> indicating the value
     * of this attribute.
     */
    public boolean getReturnContext();

    /**
     * Sets the boolean value for this XML attribute
     * @see #getReturnContext()
     *
     * @param returnContext <code>boolean</code> indicating the value
     * of this attribute.
     *
     * @throws XACMLException If the object is immutable.
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setReturnContext(boolean returnContext) throws XACMLException;

    /**
     * Returns the <code>xacml-context:Request</code> element of this object
     *
     * @return the <code>xacml-context:Request</code> elements of this object
     */
    public Request getRequest();

    /**
     * Sets the <code>xacml-context:Request</code> element of this object
     *
     * @param request the <code>xacml-context:Request</code> element of this 
     * object.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setRequest(Request request) throws XACMLException;

   /**
    * Makes the object immutable
    */
    public void makeImmutable();

   /**
    * Checks if the object is mutable
    *
    * @return <code>true</code> if the object is mutable,
    *         <code>false</code> otherwise
    */
    public boolean isMutable();
    
}
