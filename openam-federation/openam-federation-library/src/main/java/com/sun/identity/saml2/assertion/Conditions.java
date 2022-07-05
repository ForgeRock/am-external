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
 * $Id: Conditions.java,v 1.2 2008/06/25 05:47:41 qcheng Exp $
 *
 * Portions Copyrighted 2018-2021 ForgeRock AS.
 */

package com.sun.identity.saml2.assertion;

import java.util.Date;
import java.util.List;

import org.forgerock.openam.annotations.SupportedAll;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sun.identity.saml2.assertion.impl.ConditionsImpl;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.XmlSerializable;

/**
 * The <code>Conditions</code> defines the SAML constructs that place
 * constraints on the acceptable use if SAML <code>Assertion</code>s.
 */
@SupportedAll
@JsonDeserialize(as=ConditionsImpl.class)
public interface Conditions extends XmlSerializable {

    /**
     * Returns the time instant at which the subject can no longer 
     *    be confirmed. 
     *
     * @return the time instant at which the subject can no longer 
     *    be confirmed.
     */
    Date getNotOnOrAfter();

    /**
     * Sets the time instant at which the subject can no longer 
     *    be confirmed. 
     *
     * @param value the time instant at which the subject can no longer
     *    be confirmed.
     * @exception SAML2Exception if the object is immutable
     */
    void setNotOnOrAfter(Date value) throws SAML2Exception;

    /** 
     *  Returns a list of <code>Condition</code>
     * 
     *  @return a list of <code>Condition</code>
     */
    List<Condition> getConditions();
 
    /** 
     *  Returns a list of <code>AudienceRestriction</code>
     * 
     *  @return a list of <code>AudienceRestriction</code>
     */
    List<AudienceRestriction> getAudienceRestrictions();
 
    /** 
     *  Returns a list of <code>OneTimeUse</code>
     * 
     *  @return a list of <code>OneTimeUse</code>
     */
    List<OneTimeUse> getOneTimeUses();
 
    /** 
     *  Returns a list of <code>ProxyRestriction</code>  
     * 
     *  @return a list of <code>ProxyRestriction</code>
     */
    List<ProxyRestriction> getProxyRestrictions();
 
    /** 
     *  Sets a list of <code>Condition</code>
     * 
     *  @param conditions a list of <code>Condition</code>
     *  @exception SAML2Exception if the object is immutable
     */
    void setConditions(List<Condition> conditions) throws SAML2Exception;
 
    /** 
     *  Sets a list of <code>AudienceRestriction</code>
     * 
     *  @param ars a list of <code>AudienceRestriction</code>
     *  @exception SAML2Exception if the object is immutable
     */
    void setAudienceRestrictions(List<AudienceRestriction> ars) throws SAML2Exception;
 
    /** 
     *  Sets a list of <code>OneTimeUse</code>
     * 
     *  @param oneTimeUses a list of <code>OneTimeUse</code>
     *  @exception SAML2Exception if the object is immutable
     */
    void setOneTimeUses(List<OneTimeUse> oneTimeUses) throws SAML2Exception;
 
    /** 
     *  Sets a list of <code>ProxyRestriction</code>  
     * 
     *  @param prs a list of <code>ProxyRestriction</code>
     *  @exception SAML2Exception if the object is immutable
     */
    void setProxyRestrictions(List<ProxyRestriction> prs) throws SAML2Exception;
 
    /**
     * Returns the time instant before which the subject cannot be confirmed.
     *
     * @return the time instant before which the subject cannot be confirmed.
     */
    Date getNotBefore();

    /**
     * Sets the time instant before which the subject cannot 
     *     be confirmed.
     *
     * @param value the time instant before which the subject cannot 
     *     be confirmed.
     * @exception SAML2Exception if the object is immutable
     */
    void setNotBefore(Date value) throws SAML2Exception;

    /**
     * Return true if a specific Date falls within the validity 
     * interval of this set of conditions.
     *
     * @param someTime a time in milliseconds. 
     * @return true if <code>someTime</code> is within the valid 
     * interval of the <code>Conditions</code>.     
     */
    boolean checkDateValidity(long someTime);

    /**
     * Return true if a specific Date falls within the validity
     * interval of this set of conditions.
     *
     * @param someTime a time in milliseconds.
     * @param skewTime a skew time in seconds. 
     * @return true if <code>someTime</code> is within the valid
     * interval of the <code>Conditions</code>.
     */
    boolean checkDateValidityWithSkew(long someTime, int skewTime);

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
