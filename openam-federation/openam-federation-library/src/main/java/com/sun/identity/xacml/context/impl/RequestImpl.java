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
 * $Id: RequestImpl.java,v 1.4 2008/11/10 22:57:05 veiming Exp $
 *
 * Portions Copyrighted 2017-2021 ForgeRock AS.
 */
package com.sun.identity.xacml.context.impl;

import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_PREFIX;
import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_NS_URI;
import static com.sun.identity.xacml.common.XACMLConstants.CONTEXT_SCHEMA_LOCATION_VALUE;
import static com.sun.identity.xacml.common.XACMLConstants.REQUEST;
import static com.sun.identity.xacml.common.XACMLConstants.SCHEMA_LOCATION_ATTR;
import static com.sun.identity.xacml.common.XACMLConstants.XSI_NS_ATTR;
import static com.sun.identity.xacml.common.XACMLConstants.XSI_NS_URI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.openam.annotations.SupportedAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.Action;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Environment;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.context.Resource;
import com.sun.identity.xacml.context.Subject;

/**
 * The <code>Request</code> element is the top-level element in the XACML
 * context schema. Its an abstraction layer used by the policy language.
 * It contains <code>Subject</code>, <code>Resource</code>, <code>Action
 * </code> and <code>Environment</code> elements.
 * <p>
 * <pre>
 * &lt;xs:complexType name="RequestType"&gt;
 *   &lt;xs:sequence&gt;
 *     &lt;xs:element ref="xacml-context:Subject" maxOccurs="unbounded"/&gt;
 *     &lt;xs:element ref="xacml-context:Resource" maxOccurs="unbounded"/&gt;
 *     &lt;xs:element ref="xacml-context:Action"/&gt;
 *     &lt;xs:element ref="xacml-context:Environment"/&gt;
 *   &lt;xs:sequence&gt;
 * &lt;xs:complexType&gt;
 * </pre>
 */
@SupportedAll
public class RequestImpl implements Request {

    private static final Logger logger = LoggerFactory.getLogger(RequestImpl.class);
    private List<Subject> subjects = new ArrayList<>();
    private List<Resource> resources = new ArrayList<>();
    private Action action = null;
    private Environment env = null;
    private boolean isMutable = true;
    
    private static Set<String> supportedSubjectCategory = new HashSet<>();
    static {
        supportedSubjectCategory.add(XACMLConstants.ACCESS_SUBJECT);
        supportedSubjectCategory.add(XACMLConstants.
            INTERMEDIARY_SUBJECT);
    };
   /** 
    * Default constructor
    */
    public RequestImpl() {
    }

    /**
     * This constructor is used to build <code>Request</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Request</code> object
     * @exception XACMLException if it could not process the XML string
     */
    public RequestImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            logger.error(
                "RequestImpl.processElement(): invalid XML input");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>Request</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Request</code> object
     * @exception XACMLException if it could not process the Element
     */
    public  RequestImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            logger.error(
                "RequestImpl.processElement(): invalid root element");
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
             logger.error(
                "RequestImpl.processElement(): local name missing");
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.REQUEST)) {
            logger.error(
                "RequestImpl.processElement(): invalid local name " +
                 elemName);
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_local_name"));
        }

        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes < 1) {
            logger.error(
                "RequestImpl.processElement(): request has no subelements");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_subelements"));
        }
   
        ContextFactory factory = ContextFactory.getInstance();
        List children = new ArrayList();
        int i = 0;
        Node child;
        while ( i < numOfNodes) {
            child = (Node)nodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                children.add(child);
            }
            i++;
        }
        if (children.isEmpty()) {
            logger.error("RequestImpl.processElement():"
                + " request has no subelements");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_subelements"));
        }
        child = (Node)children.get(0);
        // The first subelement should be <Subject>
        String childName = child.getLocalName();
        if ((childName == null) || (!childName.
            equals(XACMLConstants.SUBJECT))) {
            logger.error("RequestImpl.processElement():"+
                " the first element is not <Subject>");
        throw new XACMLException(
            XACMLSDKUtils.xacmlResourceBundle.getString(
            "missing_subelement_subject"));
        }
        Subject subject = factory.getInstance().createSubject((Element)child);
        if (!supportedSubjectCategory.contains(
            subject.getSubjectCategory().toString())) 
        {
            logger.error("RequestImpl.processElement():subject "
                +"category in subject not supported");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "unsupported_subject_category")); 
        }
        subjects.add(subject);
        boolean resourceFound = false;
        boolean actionFound = false;
        boolean envFound = false;
        for ( int j = 1; j < children.size(); j++) {
            child = (Node)children.get(j);
            // so far <Resource> is not encountered
            // Go through next sub elements for <Subject> and <Resource>
            // The next subelement may be <Resource> or <Subject>
            childName = child.getLocalName();
            if ((childName != null) &&
                (childName.equals(XACMLConstants.RESOURCE) || childName.
                equals(XACMLConstants.SUBJECT))) {
                    if (resourceFound) {
                        if (childName.equals(XACMLConstants.SUBJECT)) {
                            // all <Subject> should be before <Resource>
                            logger.error("RequestImpl."
                                +"processElement(): <Subject> should be "
                                + "before <Resource>");
                            throw new XACMLException(
                                XACMLSDKUtils.xacmlResourceBundle.getString(
                                    "element_out_of_place"));
                        } else { // found another resource
                            Resource resource = factory.getInstance()
                                    .createResource((
                                    Element)child);
                            resources.add(resource);
                        }
                    } else if (childName.equals(XACMLConstants.SUBJECT)) {
                            subject = factory.getInstance().createSubject(
                                (Element)child);
                            subjects.add(subject);
                    } else { // childname is resource
                            resourceFound = true;
                            Resource resource = factory.getInstance()
                                    .createResource((
                                    Element)child);
                            resources.add(resource);
                    }
            } else if ((childName != null) && (childName.
                equals(XACMLConstants.ACTION))) {
                if (!resourceFound) {
                    logger.error("RequestImpl."
                        +"processElement(): <Resource> should be "
                        + "before <Action>");
                    throw new XACMLException(
                        XACMLSDKUtils.xacmlResourceBundle.getString(
                            "element_out_of_place"));
                } else {
                    actionFound = true;
                    action = factory.createAction((Element)child);                                     
                }
            } else if ((childName != null) && (childName.
                equals(XACMLConstants.ENVIRONMENT))) {
                if (!resourceFound || !actionFound){
                    logger.error("RequestImpl."
                        +"processElement(): <Resource> and "
                        +"Action should be before <Environment>");
                        throw new XACMLException(
                            XACMLSDKUtils.xacmlResourceBundle.getString(
                                "element_out_of_place"));
                } else {
                    envFound = true;
                    env = factory.createEnvironment((Element) child);
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("resourceFound:"+resourceFound);
            logger.debug("actionFound:"+actionFound);
            logger.debug("envFound:"+envFound);
        }
        if (!resourceFound || !actionFound || !envFound) {
            logger.error("RequestImpl.processElement(): Some"
                +"of required elements are missing");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                    "missing_subelements"));
        }
    }
        

    /**
     * Returns the one to many <code>Subject</code> elements of this object
     *
     * @return the <code>Subject</code> elements of this object
     */
    public List getSubjects() {
        return subjects;
    }

    /**
     * Sets the one to many <code>Subject</code> elements of this object
     *
     * @param subjects the one to many <code>Subject</code> elements of this 
     * object
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setSubjects(List subjects) throws XACMLException {
        if (!isMutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        if (subjects == null || subjects.isEmpty()) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                    "null_not_valid"));
        }
        this.subjects.addAll(subjects);
    }
    

    /**
     * Returns the one to many <code>Resource</code> elements of this object
     *
     * @return the <code>Resource</code> elements of this object
     */
    public List getResources() {
        return resources;
    }

    /**
     * Sets the one to many <code>Resource</code> elements of this object
     *
     * @param resources the one to many <code>Resource</code> elements of this 
     * object
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setResources(List resources) throws XACMLException {
        if (!isMutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        } 
        if (resources == null || resources.isEmpty()) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid"));
        }
        this.resources.addAll(resources);
    }

    /**
     * Returns the instance of <code>Action</code> element
     *
     * @return the instance of <code>Action</code>.
     */
    public Action getAction() {
        return action;
    }

    /**
     * Sets the instance of <code>Action</code>
     *
     * @param argAction instance of  <code>Action</code>.
     * 
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setAction(Action argAction) throws XACMLException {
        if (!isMutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        
        if (argAction == null) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid")); 
        }
        action = argAction;
        
    }

    /**
     * Returns the instance of <code>Environment</code> element.
     *
     * @return the instance of <code>Environment</code>.
     */
    public Environment getEnvironment() {
        return env;
    }

    /**
     * Sets the instance of the <code>Environment</code>
     *
     * @param argEnv instance of <code>Environment</code>.
     * @throws XACMLException if the object is immutable
     *         An object is considered <code>immutable</code> if <code>
     *         makeImmutable()</code> has been invoked on it. It can
     *         be determined by calling <code>isMutable</code> on the object.
     */
    public void setEnvironment(Environment argEnv) throws XACMLException {
        if (!isMutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        } 
        if (argEnv == null ) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid")); 
        }
        env = argEnv;
    }

    @Override
    public DocumentFragment toDocumentFragment(Document document, boolean includeNSPrefix, boolean declareNS)
            throws SAML2Exception {

        DocumentFragment fragment = document.createDocumentFragment();
        Element requestElement = XMLUtils.createRootElement(document, CONTEXT_NS_PREFIX, CONTEXT_NS_URI, REQUEST,
                includeNSPrefix, declareNS);
        fragment.appendChild(requestElement);
        if (declareNS) {
            requestElement.setAttribute(XSI_NS_ATTR, XSI_NS_URI);
            requestElement.setAttribute(SCHEMA_LOCATION_ATTR, CONTEXT_SCHEMA_LOCATION_VALUE);
        }

        if (subjects != null) {
            for (Subject subject : subjects) {
                requestElement.appendChild(subject.toDocumentFragment(document, includeNSPrefix, false));
            }
        }
        if (resources != null) {
            for (Resource resource : resources) {
                requestElement.appendChild(resource.toDocumentFragment(document, includeNSPrefix, false));
            }
        }
        if (action != null) {
            requestElement.appendChild(action.toDocumentFragment(document, includeNSPrefix, false));
        }
        if (env != null) {
            requestElement.appendChild(env.toDocumentFragment(document, includeNSPrefix, false));
        }

        return fragment;
    }

    /**
    * Makes the object immutable
    */
    public void makeImmutable() {}

   /**
    * Checks if the object is mutable
    *
    * @return <code>true</code> if the object is mutable,
    *         <code>false</code> otherwise
    */
    public boolean isMutable() {
        return isMutable;
    }
}
