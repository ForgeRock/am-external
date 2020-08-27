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
 * $Id: SAML2MetaUtils.java,v 1.9 2009/09/21 17:28:12 exu Exp $
 *
 * Portions Copyrighted 2010-2020 ForgeRock AS.
 */
package com.sun.identity.saml2.meta;

import static java.util.stream.Collectors.toMap;
import static org.forgerock.openam.saml2.Saml2EntityRole.IDP;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;

import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeType;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AttributeAuthorityDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.AuthnAuthorityDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.EntitiesDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.KeyDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.XACMLPDPDescriptorType;
import com.sun.identity.saml2.jaxb.metadataextquery.AttributeQueryDescriptorType;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>SAML2MetaUtils</code> provides metadata related util methods.
 */
@Supported
public final class SAML2MetaUtils {

    private static final Logger logger = LoggerFactory.getLogger(SAML2MetaUtils.class);
    protected static final String RESOURCE_BUNDLE_NAME = "libSAML2Meta";
    protected static ResourceBundle resourceBundle =
                         Locale.getInstallResourceBundle(RESOURCE_BUNDLE_NAME);
    private static Logger debug = LoggerFactory.getLogger(SAML2MetaUtils.class);
    private static final String METADATA_JAXB_PACKAGES =
        "com.sun.identity.saml2.jaxb.xmlenc:" +
        "com.sun.identity.saml2.jaxb.xmlsig:" +
        "com.sun.identity.saml2.jaxb.assertion:" +
        "com.sun.identity.saml2.jaxb.metadata:" +
        "com.sun.identity.saml2.jaxb.metadata.algsupport:" +
	    "com.sun.identity.saml2.jaxb.metadataattr:" +
	    "com.sun.identity.saml2.jaxb.xmlenc11";
    private static final String ENTITY_CONFIG_PACKAGE = "com.sun.identity.saml2.jaxb.entityconfig";
    private static final String JAXB_PACKAGE_LIST_PROP =
        "com.sun.identity.liberty.ws.jaxb.packageList";
    private static JAXBContext publicContext = null;
    private static JAXBContext completeContext = null;
    private static final String PROP_JAXB_FORMATTED_OUTPUT =
                                        "jaxb.formatted.output";
    private static final String PROP_NAMESPACE_PREFIX_MAPPER =
                                    "com.sun.xml.bind.namespacePrefixMapper";

    private static NamespacePrefixMapperImpl nsPrefixMapper =
            new NamespacePrefixMapperImpl();

    static {
        try {
            Optional<String> extraPackages = Optional.ofNullable(SystemPropertiesManager.get(JAXB_PACKAGE_LIST_PROP))
                    .filter(StringUtils::isNotEmpty);
            publicContext = JAXBContext.newInstance(extraPackages
                    .map(p -> METADATA_JAXB_PACKAGES + ":" + p)
                    .orElse(METADATA_JAXB_PACKAGES));
            completeContext = JAXBContext.newInstance(ENTITY_CONFIG_PACKAGE + ":" + METADATA_JAXB_PACKAGES
                    + extraPackages.map(p -> ":" + p).orElse(""));
        } catch (JAXBException jaxbe) {
            debug.error("SAML2MetaUtils.static:", jaxbe);
        }
    }

    private SAML2MetaUtils() {
    }

    /**
     * Converts a <code>String</code> object to a JAXB object.
     * @param str a <code>String</code> object
     * @return a JAXB object converted from the <code>String</code> object.
     * @exception JAXBException if an error occurs while converting
     *                          <code>String</code> object
     */
    public static Object convertStringToJAXB(String str)
        throws JAXBException {

       Unmarshaller u = completeContext.createUnmarshaller();
       return u.unmarshal(XMLUtils.createSAXSource(new InputSource(new StringReader(str))));
    }

    /**
     * Converts a <code>Node</code> object to a JAXB object.
     * @param node a <code>Node</code> object
     * @return a JAXB object converted from the <code>Node</code> object.
     * @exception JAXBException if an error occurs while converting
     *                          <code>Node</code> object
     */
    public static Object convertNodeToJAXB(Node node)
        throws JAXBException {

       Unmarshaller u = completeContext.createUnmarshaller();
       //no need to get SAXSource, since the node is already created by using
       //a secure XML parser
       return u.unmarshal(node);
    }

    /**
     * Converts a JAXB object to a <code>String</code> object.
     * @param jaxbObj a JAXB object
     * @return a <code>String</code> representing the JAXB object.
     * @exception JAXBException if an error occurs while converting JAXB object
     */
    public static String convertJAXBToString(Object jaxbObj) throws JAXBException {
        StringWriter sw = new StringWriter();
        Marshaller marshaller = getMarshaller(jaxbObj);
        marshaller.setProperty(PROP_JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(PROP_NAMESPACE_PREFIX_MAPPER, nsPrefixMapper);
        marshaller.marshal(jaxbObj, sw);
        return sw.toString();
    }

    /**
     * Converts a JAXB object and writes to an <code>OutputStream</code> object.
     * @param jaxbObj a JAXB object
     * @param os an <code>OutputStream</code> object
     * @exception JAXBException if an error occurs while converting JAXB object
     */
    public static void convertJAXBToOutputStream(Object jaxbObj, OutputStream os) throws JAXBException {
        Marshaller marshaller = getMarshaller(jaxbObj);
        marshaller.setProperty(PROP_JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(PROP_NAMESPACE_PREFIX_MAPPER, nsPrefixMapper);
        marshaller.marshal(jaxbObj, os);
    }

    private static Marshaller getMarshaller(Object jaxbObj) throws JAXBException {
        return jaxbObj instanceof EntityConfigElement
                ? completeContext.createMarshaller()
                : publicContext.createMarshaller();
    }

    /**
     * Converts a JAXB object to a <code>String</code> object and creates a
     * <code>Map</code>. The key is 'attrName' and the value is a
     * <code>Set</code> contains the <code>String</code> object.
     * @param attrName attribute name
     * @param jaxbObj a JAXB object
     * @return a <code>Map</code>. The key is 'attrName' and the value is a
     *         <code>Set</code> contains the <code>String</code> object
     *         converted from the JAXB object.
     * @exception JAXBException if an error occurs while converting JAXB object
     */
    protected static Map convertJAXBToAttrMap(String attrName, Object jaxbObj)
        throws JAXBException {

        String xmlString = convertJAXBToString(jaxbObj);
        Map attrs = new HashMap();
        Set values = new HashSet();
        values.add(xmlString);
        attrs.put(attrName, values);

        return attrs;
    }

    /**
     * Gets attribute value pairs from <code>BaseConfigType</code> and
     * put in a <code>Map</code>. The key is attribute name and the value is
     * a <code>List</code> of attribute values;
     * @param config the <code>BaseConfigType</code> object
     * @return a attribute value <code>Map</code>
     */
    public static Map<String, List<String>> getAttributes(JAXBElement<BaseConfigType> config) {
        return config.getValue()
                .getAttribute()
                .stream()
                .collect(toMap(AttributeType::getName, type -> new ArrayList<>(type.getValue())));
    }

    /**
     * Returns the realm by parsing the metaAlias. MetaAlias format is
     * <pre>
     * &lt;realm&gt;/&lt;any string without '/'&gt; for non-root realm or
     * /&lt;any string without '/'&gt; for root realm.
     * </pre>
     * @param metaAlias The metaAlias.
     * @return the realm associated with the metaAlias.
     */
    @Supported
    public static String getRealmByMetaAlias(String metaAlias) {
        if (metaAlias == null) {
            return null;
        }

        int index = metaAlias.lastIndexOf("/");
        if (index == -1 || index == 0) {
            return "/";
        }

        return metaAlias.substring(0, index);
    }

    /**
     * Returns metaAlias embedded in uri.
     * @param uri The uri string.
     * @return the metaAlias embedded in uri or null if not found.
     */
    @Supported
    public static String getMetaAliasByUri(String uri) {
        if (uri == null) {
            return null;
        }

        final int index = uri.indexOf(SAML2MetaManager.NAME_META_ALIAS_IN_URI);
        final int marker = index + SAML2MetaManager.NAME_META_ALIAS_IN_URI.length();
        if (index == -1 || marker == uri.length()) {
            return null;
        }

        return uri.substring(marker);
    }

    /**
     * Returns first policy decision point descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return policy decision point descriptor or null if it is not found.
     */
    public static XACMLPDPDescriptorType getPolicyDecisionPointDescriptor(EntityDescriptorElement eDescriptor) {
        if (eDescriptor != null) {
            List<RoleDescriptorType> list = eDescriptor.getValue()
                    .getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

            for (RoleDescriptorType descriptorType : list) {
                if (descriptorType instanceof XACMLPDPDescriptorType) {
                    return (XACMLPDPDescriptorType) descriptorType;
                }
            }
        }

        return null;
    }


    /**
     * Returns first policy enforcement point descriptor in an entity
     * descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return policy enforcement point descriptor or null if it is not found.
     */
    public static XACMLAuthzDecisionQueryDescriptorType getPolicyEnforcementPointDescriptor(
            EntityDescriptorElement eDescriptor) {
        if (eDescriptor != null) {
            List<RoleDescriptorType> list = eDescriptor.getValue()
                    .getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

            for (RoleDescriptorType obj : list) {
                if (obj instanceof XACMLAuthzDecisionQueryDescriptorType) {
                    return (XACMLAuthzDecisionQueryDescriptorType) obj;
                }
            }
        }

        return null;
    }

    /**
     * Returns first service provider's SSO descriptor in an entity
     * descriptor.
     * @param eDescriptor The entity descriptor.
     * @return <code>SPSSODescriptorElement</code> for the entity or null if
     *         not found.
     */
    public static SPSSODescriptorType getSPSSODescriptor(EntityDescriptorElement eDescriptor) {
        if (eDescriptor == null) {
            return null;
        }

        List<RoleDescriptorType> list = eDescriptor.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        for (RoleDescriptorType obj : list) {
            if (obj instanceof SPSSODescriptorType) {
                return (SPSSODescriptorType) obj;
            }
        }

        return null;
    }

    /**
     * Returns first identity provider's SSO descriptor in an entity
     * descriptor.
     * @param eDescriptor The entity descriptor.
     * @return <code>IDPSSODescriptorElement</code> for the entity or null if
     *         not found.
     */
    public static IDPSSODescriptorType getIDPSSODescriptor(EntityDescriptorElement eDescriptor) {
        if (eDescriptor == null) {
            return null;
        }

        List<RoleDescriptorType> list = eDescriptor.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        for (RoleDescriptorType obj : list) {
            if (obj instanceof IDPSSODescriptorType) {
                return (IDPSSODescriptorType) obj;
            }
        }

        return null;
    }

    /**
     * Returns attribute authority descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return an <code>AttributeAuthorityDescriptorElement</code> object for
     *     the entity or null if not found.
     */
    public static AttributeAuthorityDescriptorType getAttributeAuthorityDescriptor(
            EntityDescriptorElement eDescriptor) {
        if (eDescriptor == null) {
            return null;
        }

        List<RoleDescriptorType> list = eDescriptor.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

        for (RoleDescriptorType obj : list) {
            if (obj instanceof AttributeAuthorityDescriptorType) {
                return (AttributeAuthorityDescriptorType) obj;
            }
        }

        return null;
    }

    /**
     * Returns attribute query descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return an <code>AttributeQueryDescriptorElement</code> object for
     *     the entity or null if not found.
     */
    public static AttributeQueryDescriptorType getAttributeQueryDescriptor(EntityDescriptorElement eDescriptor) {
        if (eDescriptor == null) {
            return null;
        }

        List<RoleDescriptorType> list =
            eDescriptor.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

        for (RoleDescriptorType obj : list) {
            if (obj instanceof AttributeQueryDescriptorType) {
                return (AttributeQueryDescriptorType) obj;
            }
        }

        return null;
    }

    /**
     * Returns authentication authority descriptor in an entity descriptor.
     *
     * @param eDescriptor The entity descriptor.
     * @return an <code>AuthnAuthorityDescriptorElement</code> object for
     *     the entity or null if not found.
     */
    public static AuthnAuthorityDescriptorType getAuthnAuthorityDescriptor(EntityDescriptorElement eDescriptor) {
        if (eDescriptor == null) {
            return null;
        }

        List<RoleDescriptorType> list =
            eDescriptor.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();

        for (RoleDescriptorType obj : list) {
            if (obj instanceof AuthnAuthorityDescriptorType) {
                return (AuthnAuthorityDescriptorType) obj;
            }
        }

        return null;
    }

    /**
     * Returns first service provider's SSO configuration in an entity.
     * @param eConfig <code>EntityConfigElement</code> of the entity to
     * be retrieved.
     * @return <code>SPSSOConfigElement</code> for the entity or null if not
     *         found.
     */
    public static SPSSOConfigElement getSPSSOConfig(EntityConfigElement eConfig) {
        if (eConfig == null) {
            return null;
        }

        List<JAXBElement<BaseConfigType>> list =
                eConfig.getValue().getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        for (JAXBElement<BaseConfigType> obj : list) {
            if (obj instanceof SPSSOConfigElement) {
                return (SPSSOConfigElement) obj;
            }
        }

        return null;
    }

    /**
     * Returns first identity provider's SSO configuration in an entity
     * @param eConfig <code>EntityConfigElement</code> of the entity to
     * be retrieved.
     * @return <code>IDPSSOConfigElement</code> for the entity or null if not
     *         found.
     */
    public static IDPSSOConfigElement getIDPSSOConfig(
        EntityConfigElement eConfig) {
        if (eConfig == null) {
            return null;
        }

        List<JAXBElement<BaseConfigType>> list =
            eConfig.getValue().getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig();
        for (JAXBElement<BaseConfigType> obj : list) {
            if (obj instanceof IDPSSOConfigElement) {
                return (IDPSSOConfigElement) obj;
            }
        }

        return null;
    }

    /**
     * Returns the SAML2 entity meta data.
     *
     * @param realm Realm where the entity belongs to.
     * @param entityId The SAML entity id.
     * @param sign Denotes if the export data needs signing.
     * @return The meta data string.
     * @throws SAML2MetaException Thrown when the export data fails.
     */
    public static String exportStandardMeta(String realm, String entityId, boolean sign) throws SAML2MetaException {
        try {
            SAML2MetaManager metaManager = new SAML2MetaManager();
            EntityDescriptorElement standardMetadata = metaManager.getEntityDescriptor(realm, entityId);
            if (standardMetadata == null) {
                return null;
            }

            EntityConfigElement extendedMetadata = metaManager.getEntityConfig(realm, entityId);
            if (extendedMetadata != null && extendedMetadata.getValue().isHosted()) {
                KeyDescriptorExporter keyDescriptorExporter = InjectorHolder.getInstance(KeyDescriptorExporter.class);
                populateKeyDescriptors(standardMetadata, extendedMetadata, IDP, (roleExtended) ->
                        keyDescriptorExporter.createKeyDescriptors(realm, entityId, IDP, roleExtended));
                populateKeyDescriptors(standardMetadata, extendedMetadata, SP, (roleExtended) ->
                        keyDescriptorExporter.createKeyDescriptors(realm, entityId, SP, roleExtended));
            }

            String xmlstr = null;
            if (sign) {
                Document doc = SAML2MetaSecurityUtils.sign(realm, standardMetadata);
                if (doc != null) {
                    xmlstr = XMLUtils.print(doc);
                }
            }
            if (xmlstr == null) {
                xmlstr = convertJAXBToString(standardMetadata);
                xmlstr = SAML2MetaSecurityUtils.formatBase64BinaryElement(xmlstr);
            }
            return workaroundAbstractRoleDescriptor(xmlstr);
        } catch (JAXBException | SAML2Exception e) {
            throw new SAML2MetaException(e.getMessage());
        }
    }

    /**
     *
     * @param metadata A string representing an EntityDescriptorElement XML document
     * @return EntityDescriptorElement an EntityDescriptorElement from the passed metadata
     * @throws SAML2MetaException If there was a problem with the parsed metadata
     * @throws JAXBException If there was a problem parsing the metadata
     */
    public static EntityDescriptorElement getEntityDescriptorElement(String metadata)
        throws SAML2MetaException, JAXBException {

        Document doc = XMLUtils.toDOMDocument(metadata);

        if (doc == null) {
            throw new SAML2MetaException("Null document");
        }

        Element docElem = doc.getDocumentElement();

        if ((!SAML2MetaConstants.ENTITY_DESCRIPTOR.equals(docElem.getLocalName())) ||
            (!SAML2MetaConstants.NS_METADATA.equals(docElem.getNamespaceURI()))) {
            throw new SAML2MetaException("Invalid  descriptor");
        }

        Object element = preProcessSAML2Document(doc);

        return (element instanceof EntityDescriptorElement) ?
            (EntityDescriptorElement)element : null;
    }

    /**
     * For the given XML metadata document representing either a SAML2 EntityDescriptorElement or EntitiesDescriptorElement,
     * return a list of entityId's for all the Entities created. Carries out a signature validation of the document as
     * part of the import process.
     * @param metaManager An instance of the SAML2MetaManager, used to do the actual create.
     * @param realm The realm to create the Entities in
     * @param doc The XML document that represents either an EntityDescriptorElement or EntitiesDescriptorElement
     * @return A list of all entityId's imported or an empty list if no Entities were imported.
     * @throws SAML2MetaException for any issues as a result of trying to create the Entities.
     * @throws JAXBException for any issues converting the document into a JAXB document.
     */
    public static List<String> importSAML2Document(SAML2MetaManager metaManager,
            String realm, Document doc) throws SAML2MetaException, JAXBException {

        List<String> result = new ArrayList<String>(1);

        Object element = preProcessSAML2Document(doc);

        if (element instanceof EntityDescriptorElement) {
            String entityId = importSAML2Entity(metaManager, realm,
                    (EntityDescriptorElement)element);
            if (entityId != null) {
                result.add(entityId);
            }
        } else if (element instanceof EntitiesDescriptorElement) {
            result = importSAML2Entites(metaManager, realm,
                    (EntitiesDescriptorElement)element);
        }

        if (debug.isDebugEnabled()) {
            debug.debug("SAML2MetaUtils.importSAML2Document: " +
                "Created " + result + " entities");
        }

        return result;
    }

    private static Object preProcessSAML2Document(Document doc) throws SAML2MetaException, JAXBException {

        SAML2MetaSecurityUtils.verifySignature(doc);
        workaroundAbstractRoleDescriptor(doc);

        Object obj = convertNodeToJAXB(doc);

        // Remove any Extensions elements as these are currently not supported.
        obj = workaroundJAXBBug(obj);

        return obj;
    }

    private static List<String> importSAML2Entites(SAML2MetaManager metaManager, String realm,
            EntitiesDescriptorElement descriptor) throws SAML2MetaException {

        List<String> result = new ArrayList<String>();

        List<JAXBElement<?>> descriptors = descriptor.getValue().getEntityDescriptorOrEntitiesDescriptor();
        if (descriptors != null && !descriptors.isEmpty()) {
            Iterator<JAXBElement<?>> entities = descriptors.iterator();
            while (entities.hasNext()) {
                JAXBElement<?> o = entities.next();
                if (o instanceof EntityDescriptorElement) {
                    String entityId = importSAML2Entity(metaManager, realm,
                            (EntityDescriptorElement) o);
                    if (entityId != null) {
                        result.add(entityId);
                    }
                }
            }
        }

        return result;
    }

    private static String importSAML2Entity(SAML2MetaManager metaManager, String realm,
            EntityDescriptorElement descriptor) throws SAML2MetaException {

        String result = null;

        List roles = descriptor.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        Iterator it = roles.iterator();
        while (it.hasNext()) {
            RoleDescriptorType role = (RoleDescriptorType)it.next();
            List protocols = role.getProtocolSupportEnumeration();
            if (!protocols.contains(SAML2Constants.PROTOCOL_NAMESPACE)) {
                if (debug.isDebugEnabled()) {
                    debug.debug("SAML2MetaUtils.importSAML2Entity: "
                        + "Removing non-SAML2 role from entity "
                        + descriptor.getValue().getEntityID());
                }
                it.remove();
            }
        }

        if (roles.size() > 0) {
            metaManager.createEntityDescriptor(realm, descriptor);
            result = descriptor.getValue().getEntityID();
        }

        return result;
    }
    
    private static Object workaroundJAXBBug(Object obj) throws JAXBException {

        String metadata = convertJAXBToString(obj);
        String replaced = metadata.replaceAll("<(.*:)?Extensions/>", "");
        if (metadata.equalsIgnoreCase(replaced)) {
            return obj;
        } else {
            return convertStringToJAXB(replaced);
        }
    }

    private static void workaroundAbstractRoleDescriptor(Document doc) {

        NodeList nl = doc.getDocumentElement().getElementsByTagNameNS(
            SAML2MetaConstants.NS_METADATA,SAML2MetaConstants.ROLE_DESCRIPTOR);
        int length = nl.getLength();
        if (length == 0) {
            return;
        }

        for (int i = 0; i < length; i++) {
            Element child = (Element)nl.item(i);
            String type = child.getAttributeNS(SAML2Constants.NS_XSI, "type");
            if (type != null) {
                if ((type.equals(
                    SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE)) ||
                    (type.endsWith(":" +
                    SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE))) {

                    String newTag = type.substring(0, type.length() - 4);

                    String extraNS = "";
                    if (newTag.contains(":")) {
                        String prefix = newTag.substring(0, newTag.indexOf(":"));
                        String nsvalue = child.getAttributeNS(SAML2Constants.NS_XML, prefix);
                        if (!SAML2MetaSecurityUtils.NS_MD_QUERY.equals(nsvalue)) {
                            extraNS = " " + SAML2Constants.NAMESPACE_PREFIX + ":" + prefix +
                               SAML2Constants.EQUAL + SAML2Constants.QUOTE +
                               SAML2MetaSecurityUtils.NS_MD_QUERY + SAML2Constants.QUOTE;
                       }
                    }

                    String xmlstr = XMLUtils.print(child);
                    int index = xmlstr.indexOf(
                        SAML2MetaConstants.ROLE_DESCRIPTOR);
                    xmlstr = "<" + newTag + extraNS + xmlstr.substring(index +
                        SAML2MetaConstants.ROLE_DESCRIPTOR.length());
                    if (!xmlstr.endsWith("/>")) {
                        index = xmlstr.lastIndexOf("</");
                        xmlstr = xmlstr.substring(0, index) + "</" + newTag +
                            ">";
                    }

                    Document tmpDoc = XMLUtils.toDOMDocument(xmlstr);
                    Node newChild =
                        doc.importNode(tmpDoc.getDocumentElement(), true);
                    child.getParentNode().replaceChild(newChild, child);
                }
            }
        }
    }

    private static String workaroundAbstractRoleDescriptor(String xmlstr) {
        int index =
                xmlstr.indexOf(":" + SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR);
        if (index == -1) {
            return xmlstr;
        }

        int index2 = xmlstr.lastIndexOf("<", index);
        if (index2 == -1) {
            return xmlstr;
        }

        String prefix = xmlstr.substring(index2 + 1, index);
        String type = prefix + ":" +
                SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR_TYPE;

        xmlstr = xmlstr.replaceAll("<" + prefix + ":" +
                        SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR,
                "<" + SAML2MetaConstants.ROLE_DESCRIPTOR + " " +
                        SAML2Constants.XSI_DECLARE_STR + " xsi:type=\"" + type + "\"");
        xmlstr = xmlstr.replaceAll("</" + prefix + ":" +
                        SAML2MetaConstants.ATTRIBUTE_QUERY_DESCRIPTOR,
                "</" + SAML2MetaConstants.ROLE_DESCRIPTOR);
        return xmlstr;
    }

    private static void populateKeyDescriptors(EntityDescriptorElement standardMetadata,
            EntityConfigElement extendedMetadata, Saml2EntityRole saml2EntityRole,
            Function<Map<String, List<String>>, List<KeyDescriptorElement>, SAML2Exception> keyDescriptorMapper)
            throws SAML2Exception {

        if (!saml2EntityRole.isPresent(standardMetadata)) {
            return;
        }
        RoleDescriptorType roleStandardMetadata =
                saml2EntityRole.getStandardMetadata(standardMetadata);

        Map<String, List<String>> roleExtendedMetadata = extendedMetadata == null ? Collections.emptyMap() :
                saml2EntityRole.getExtendedMetadata(extendedMetadata);

        roleStandardMetadata.getKeyDescriptor().clear();
        roleStandardMetadata.getKeyDescriptor().addAll(keyDescriptorMapper.apply(roleExtendedMetadata));
    }
}
