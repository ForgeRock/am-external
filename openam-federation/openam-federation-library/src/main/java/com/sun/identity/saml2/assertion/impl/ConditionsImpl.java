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
 * $Id: ConditionsImpl.java,v 1.3 2008/06/25 05:47:43 qcheng Exp $
 *
 * Portions Copyrighted 2018-2025 Ping Identity Corporation.
 */


package com.sun.identity.saml2.assertion.impl;

import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_NAMESPACE_URI;
import static com.sun.identity.saml2.common.SAML2Constants.ASSERTION_PREFIX;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.assertion.Condition;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.OneTimeUse;
import com.sun.identity.saml2.assertion.ProxyRestriction;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>Conditions</code> defines the SAML constructs that place
 * constraints on the acceptable use if SAML <code>Assertion</code>s.
 */
public class ConditionsImpl implements Conditions {

    private static final Logger logger = LoggerFactory.getLogger(ConditionsImpl.class);
    private Date notOnOrAfter;
    private List<Condition> conditions = new ArrayList<>();
    private List<AudienceRestriction> audienceRestrictions = new ArrayList<>();
    private List<OneTimeUse> oneTimeUses = new ArrayList<>();
    private List<ProxyRestriction> proxyRestrictions = new ArrayList<>();
    private Date notBefore;
    private boolean isMutable = true;

    public static final String CONDITIONS_ELEMENT = "Conditions"; 
    public static final String CONDITION_ELEMENT = "Condition"; 
    public static final String ONETIMEUSE_ELEMENT = "OneTimeUse"; 
    public static final String AUDIENCE_RESTRICTION_ELEMENT = 
                               "AudienceRestriction"; 
    public static final String PROXY_RESTRICTION_ELEMENT = 
                               "ProxyRestriction"; 
    public static final String NOT_BEFORE_ATTR = "NotBefore"; 
    public static final String NOT_ON_OR_AFTER_ATTR = "NotOnOrAfter"; 

    /** 
     * Default constructor
     */
    public ConditionsImpl() {
    }

    /**
     * This constructor is used to build <code>Conditions</code> object
     * from a XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Conditions</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public ConditionsImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "ConditionsImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>Conditions</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Conditions</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public ConditionsImpl(Element element) throws SAML2Exception {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            logger.error(
                "ConditionsImpl.processElement(): invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
            logger.error(
                "ConditionsImpl.processElement(): local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(CONDITIONS_ELEMENT)) {
            logger.error(
                "ConditionsImpl.processElement(): invalid local name " 
                + elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }

        // starts processing attributes
        String attrValue = element.getAttribute(NOT_BEFORE_ATTR);
        if ((attrValue != null) && (attrValue.trim().length() != 0)) {
            try {
                notBefore = DateUtils.stringToDate(attrValue);   
            } catch (ParseException pe) {
                logger.error("ConditionsImpl.processElement():"
                   + " invalid NotBefore attribute");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "invalid_date_format"));
            } 
        } 
        attrValue = element.getAttribute(NOT_ON_OR_AFTER_ATTR);
        if ((attrValue != null) && (attrValue.trim().length() != 0)) {
            try {
                notOnOrAfter = DateUtils.stringToDate(attrValue);   
            } catch (ParseException pe) {
                logger.error("ConditionsImpl.processElement():"
                   + " invalid NotOnORAfter attribute");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "invalid_date_format"));
            } 
        }
       
        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        int nextElem = 0;

        while (nextElem < numOfNodes) { 
            Node child = (Node) nodes.item(nextElem);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String childName = child.getLocalName();
                if (childName != null) {
                    if (childName.equals(CONDITION_ELEMENT)) {
                        conditions.add(AssertionFactory.
                            getInstance().createCondition(
                            (Element)child));
                    } else if (childName.equals(AUDIENCE_RESTRICTION_ELEMENT)) {
                        audienceRestrictions.add(AssertionFactory.
                            getInstance().createAudienceRestriction(
                            (Element)child));
                    } else if (childName.equals(ONETIMEUSE_ELEMENT)) {
                        oneTimeUses.add(AssertionFactory.getInstance().
                            createOneTimeUse((Element)child));
                    } else if (childName.equals(PROXY_RESTRICTION_ELEMENT)) {
                        proxyRestrictions.add(AssertionFactory.
                            getInstance().createProxyRestriction(
                            (Element)child));
                    } else {
                        logger.error("ConditionsImpl."
                            +"processElement(): unexpected subelement "
                            + childName);
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                           "unexpected_subelement"));
                    }
                }
            }
            nextElem++;
        }
    }

    /**
     * Returns the time instant at which the subject can no longer 
     *    be confirmed. 
     *
     * @return the time instant at which the subject can no longer 
     *    be confirmed.
     */
    public Date getNotOnOrAfter() {
        return notOnOrAfter;
    }

    /**
     * Sets the time instant at which the subject can no longer 
     *    be confirmed. 
     *
     * @param value the time instant at which the subject can no longer
     *    be confirmed.
     * @exception SAML2Exception if the object is immutable
     */
    public void setNotOnOrAfter(Date value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }  
        notOnOrAfter = value;
    }

    /** 
     *  Returns a list of <code>Condition</code>
     * 
     *  @return a list of <code>Condition</code>
     */
    public List<Condition> getConditions() {
        return conditions;
    }
 
    /** 
     *  Returns a list of <code>AudienceRestriction</code>
     * 
     *  @return a list of <code>AudienceRestriction</code>
     */
    public List<AudienceRestriction> getAudienceRestrictions() {
        return audienceRestrictions;
    }
 
    /** 
     *  Returns a list of <code>OneTimeUse</code>
     * 
     *  @return a list of <code>OneTimeUse</code>
     */
    public List<OneTimeUse> getOneTimeUses() {
        return oneTimeUses;
    }
 
    /** 
     *  Returns a list of <code>ProxyRestriction</code>  
     * 
     *  @return a list of <code>ProxyRestriction</code>
     */
    public List<ProxyRestriction> getProxyRestrictions() {
        return proxyRestrictions;
    }
 
    /** 
     *  Sets a list of <code>Condition</code>
     * 
     *  @param conditions a list of <code>Condition</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setConditions(List<Condition> conditions) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.conditions = conditions;
   }
 
    /** 
     *  Sets a list of <code>AudienceRestriction</code>
     * 
     *  @param ars a list of <code>AudienceRestriction</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAudienceRestrictions(List<AudienceRestriction> ars) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        audienceRestrictions = ars;
   }
 
    /** 
     *  Sets a list of <code>OneTimeUse</code>
     * 
     *  @param oneTimeUses a list of <code>OneTimeUse</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setOneTimeUses(List<OneTimeUse> oneTimeUses) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.oneTimeUses = oneTimeUses;
   }
 
    /** 
     *  Sets a list of <code>ProxyRestriction</code>  
     * 
     *  @param prs a list of <code>ProxyRestriction</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setProxyRestrictions(List<ProxyRestriction> prs) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        proxyRestrictions = prs;
   }
 
    /**
     * Returns the time instant before which the subject cannot be confirmed.
     *
     * @return the time instant before which the subject cannot be confirmed.
     */
    public Date getNotBefore() {
        return notBefore;
    }

    /**
     * Sets the time instant before which the subject cannot 
     *     be confirmed.
     *
     * @param value the time instant before which the subject cannot 
     *     be confirmed.
     * @exception SAML2Exception if the object is immutable
     */
    public void setNotBefore(Date value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        notBefore = value;
   }

    /**
     * Return true if a specific Date falls within the validity 
     * interval of this set of conditions.
     *
     * @param someTime Any time in milliseconds.
     * @return true if <code>someTime</code> is within the valid
     * interval of the <code>Conditions</code>.
     */
    public boolean checkDateValidity(long someTime) {
        return checkDateValidityWithSkew(someTime, 0);
    }

    /**
     * Return true if a specific Date falls within the validity 
     * interval of this set of conditions and skew.
     *
     * @param someTime Any time in milliseconds. 
     * @param skewTime Skew time in seconds. 
     * @return true if <code>someTime</code> is within the valid 
     * interval of the <code>Conditions</code>.     
     */
    public boolean checkDateValidityWithSkew(long someTime, int skewTime) {

        long skewTimeMillis = TimeUnit.SECONDS.toMillis(skewTime);

        if (notBefore == null ) {
            if (notOnOrAfter == null) {
                return true;
            } else {
                if (someTime < (notOnOrAfter.getTime()) + skewTimeMillis) {
                    return true;
                }
            }
        } else if (notOnOrAfter == null ) {
            if (someTime >= (notBefore.getTime() - skewTimeMillis)) {
                return true;
            }
        } else if ((someTime >= (notBefore.getTime() - skewTimeMillis)) && 
            (someTime < (notOnOrAfter.getTime()) + skewTimeMillis))
        {
            return true; 
        }
        return false;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {
        DocumentFragment fragment = document.createDocumentFragment();
        Element conditionsElement = XMLUtils.createRootElement(document, ASSERTION_PREFIX, ASSERTION_NAMESPACE_URI,
                CONDITIONS_ELEMENT, includeNSPrefix, declareNS);
        fragment.appendChild(conditionsElement);

        if (notBefore != null) {
            conditionsElement.setAttribute(NOT_BEFORE_ATTR, DateUtils.toUTCDateFormat(notBefore));
        }
        if (notOnOrAfter != null) {
            conditionsElement.setAttribute(NOT_ON_OR_AFTER_ATTR, DateUtils.toUTCDateFormat(notOnOrAfter));
        }
        if (conditions != null) {
            for (Condition condition : conditions) {
                conditionsElement.appendChild(condition.toDocumentFragment(document, includeNSPrefix, false));
            }
        }
        if (audienceRestrictions != null) {
            for (AudienceRestriction audienceRestriction : audienceRestrictions) {
                conditionsElement.appendChild(audienceRestriction.toDocumentFragment(document, includeNSPrefix, false));
            }
        }
        if (oneTimeUses != null) {
            for (OneTimeUse oneTimeUse : oneTimeUses) {
                conditionsElement.appendChild(oneTimeUse.toDocumentFragment(document, includeNSPrefix, false));
            }
        }
        if (proxyRestrictions != null) {
            for (ProxyRestriction proxyRestriction : proxyRestrictions) {
                conditionsElement.appendChild(proxyRestriction.toDocumentFragment(document, includeNSPrefix, false));
            }
        }

        return fragment;
    }

   /**
    * Makes the object immutable
    */
    public void makeImmutable() {
        if (isMutable) {
            if (conditions != null) {
                int length = conditions.size();
                for (int i = 0; i < length; i++) {
                    Condition condition = (Condition)conditions.get(i);
                    condition.makeImmutable();
                }
                conditions = Collections.unmodifiableList(conditions);
            }
            if (audienceRestrictions != null) {
                int length = audienceRestrictions.size();
                for (int i = 0; i < length; i++) {
                    AudienceRestriction ar =
                        (AudienceRestriction)audienceRestrictions.get(i);
                    ar.makeImmutable();
                }
                audienceRestrictions = Collections.unmodifiableList(
                                              audienceRestrictions);
            }
            if (oneTimeUses != null) {
                int length = oneTimeUses.size();
                for (int i = 0; i < length; i++) {
                    OneTimeUse oneTimeUse = (OneTimeUse)oneTimeUses.get(i);
                    oneTimeUse.makeImmutable();
                }
                oneTimeUses = Collections.unmodifiableList(oneTimeUses);
            }
            if (proxyRestrictions != null) {
                int length = proxyRestrictions.size();
                for (int i = 0; i < length; i++) {
                    ProxyRestriction pr =
                        (ProxyRestriction)proxyRestrictions.get(i);
                    pr.makeImmutable();
                }
                proxyRestrictions = Collections.unmodifiableList(
                                              proxyRestrictions);
            }
            isMutable = false;
        }
    }

   /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
    public boolean isMutable() {
        return isMutable;
    }
}
