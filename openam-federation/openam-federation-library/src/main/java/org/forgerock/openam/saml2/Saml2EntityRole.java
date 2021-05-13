/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2019-2020 ForgeRock AS.
 */
package org.forgerock.openam.saml2;

import static com.sun.identity.saml2.common.SAML2Constants.ATTR_AUTH_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.ATTR_QUERY_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.AUTHN_AUTH_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.PDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.PEP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static java.util.Collections.singletonList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.xml.bind.JAXBElement;

import com.sun.identity.saml2.jaxb.entityconfig.AttributeAuthorityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.AttributeQueryConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.AuthnAuthorityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.XACMLAuthzDecisionQueryConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.XACMLPDPConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AttributeAuthorityDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.AuthnAuthorityDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.ObjectFactory;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.XACMLPDPDescriptorType;
import com.sun.identity.saml2.jaxb.metadataextquery.AttributeQueryDescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaUtils;

/**
 * This enum represents the different roles a SAML2 entity provider can possibly have.
 *
 * @since AM 7.0.0
 */
public enum Saml2EntityRole {

    /**
     * Identity Provider.
     */
    IDP(IDPSSODescriptorType.class, IDPSSOConfigElement.class,
            Factories.META_DATA::createIDPSSODescriptorType,
            Factories.ENTITY_CONFIG::createIDPSSOConfigElement,
            IDP_ROLE),
    /**
     * Service Provider.
     */
    SP(SPSSODescriptorType.class, SPSSOConfigElement.class,
            Factories.META_DATA::createSPSSODescriptorType,
            Factories.ENTITY_CONFIG::createSPSSOConfigElement,
            SP_ROLE),
    /**
     * Attribute authority.
     */
    ATTR_AUTHORITY(AttributeAuthorityDescriptorType.class, AttributeAuthorityConfigElement.class,
            Factories.META_DATA::createAttributeAuthorityDescriptorType,
            Factories.ENTITY_CONFIG::createAttributeAuthorityConfigElement,
            ATTR_AUTH_ROLE),
    /**
     * Attribute query.
     */
    ATTR_QUERY(AttributeQueryDescriptorType.class, AttributeQueryConfigElement.class,
            Factories.EXT_QUERY::createAttributeQueryDescriptorType,
            Factories.ENTITY_CONFIG::createAttributeQueryConfigElement,
            ATTR_QUERY_ROLE),
    /**
     * Authentication authority.
     */
    AUTHN_AUTHORITY(AuthnAuthorityDescriptorType.class, AuthnAuthorityConfigElement.class,
            Factories.META_DATA::createAuthnAuthorityDescriptorType,
            Factories.ENTITY_CONFIG::createAuthnAuthorityConfigElement,
            AUTHN_AUTH_ROLE),
    /**
     * XACML Policy Decision Point.
     */
    XACML_PDP(XACMLPDPDescriptorType.class, XACMLPDPConfigElement.class,
            Factories.META_DATA::createPDPDescriptorType,
            Factories.ENTITY_CONFIG::createPDPConfig,
            PDP_ROLE),
    /**
     * XACML Policy Enforcement Point.
     */
    XACML_PEP(XACMLAuthzDecisionQueryDescriptorType.class, XACMLAuthzDecisionQueryConfigElement.class,
            Factories.META_DATA::createXACMLAuthzDecisionQueryDescriptorType,
            Factories.ENTITY_CONFIG::createXACMLAuthzDecisionQueryConfigElement,
            PEP_ROLE);

    /**
     * Retrieves the role's name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    private static class Factories {

        static final ObjectFactory META_DATA = new ObjectFactory();
        static final com.sun.identity.saml2.jaxb.entityconfig.ObjectFactory ENTITY_CONFIG =
                new com.sun.identity.saml2.jaxb.entityconfig.ObjectFactory();
        static final com.sun.identity.saml2.jaxb.metadataextquery.ObjectFactory EXT_QUERY =
                new com.sun.identity.saml2.jaxb.metadataextquery.ObjectFactory();

    }

    private final Class<? extends RoleDescriptorType> standardJaxbRoleClass;
    private final Class<? extends JAXBElement<BaseConfigType>> extendedJaxbRoleClass;
    private final Supplier<? extends RoleDescriptorType> standardMetadataCreator;
    private final Function<BaseConfigType, JAXBElement<BaseConfigType>> extendedMetadataCreator;
    private final String name;

    Saml2EntityRole(Class<? extends RoleDescriptorType> standardJaxbRoleClass,
            Class<? extends JAXBElement<BaseConfigType>> extendedJaxbRoleClass,
            Supplier<? extends RoleDescriptorType> standardMetadataCreator,
            Function<BaseConfigType, JAXBElement<BaseConfigType>> extendedMetadataCreator,
            String name) {
        this.standardJaxbRoleClass = standardJaxbRoleClass;
        this.extendedJaxbRoleClass = extendedJaxbRoleClass;
        this.standardMetadataCreator = standardMetadataCreator;
        this.extendedMetadataCreator = extendedMetadataCreator;
        this.name = name;
    }

    /**
     * Finds the enum representation of the role based on its (legacy) String representation.
     *
     * @param role The name of the role.
     * @return The enum representing the role.
     */
    public static Saml2EntityRole fromString(String role) {
        switch (role) {
        case IDP_ROLE:
            return IDP;
        case SP_ROLE:
            return SP;
        case ATTR_AUTH_ROLE:
            return ATTR_AUTHORITY;
        case ATTR_QUERY_ROLE:
            return ATTR_QUERY;
        case AUTHN_AUTH_ROLE:
            return AUTHN_AUTHORITY;
        case PDP_ROLE:
            return XACML_PDP;
        case PEP_ROLE:
            return XACML_PEP;
        default:
            throw new IllegalArgumentException("Unrecognised SAML2 entity role");
        }
    }

    /**
     * Finds the enum that matches the standard role instance.
     *
     * @param roleInstance the standard role instance
     * @return the corresponding role enum
     * @throws IllegalArgumentException should there not be a matching role enum
     */
    public static Saml2EntityRole fromStandardRole(RoleDescriptorType roleInstance) {
        return Arrays.stream(values())
                .filter(role -> role.matchesStandardJaxbRole().test(roleInstance))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role instance"));
    }

    /**
     * Finds the enum that matches the extended role instance.
     *
     * @param roleInstance the extended role instance
     * @return the corresponding role enum
     * @throws IllegalArgumentException should there not be a matching role enum
     */
    public static Saml2EntityRole fromExtendedRole(JAXBElement<BaseConfigType> roleInstance) {
        return Arrays.stream(values())
                .filter(role -> role.matchesExtendedJaxbRole().test(roleInstance))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown role instance"));
    }

    /**
     * Retrieves the current role's descriptor from the entity descriptor.
     *
     * @param standardJaxbEntity The entity descriptor.
     * @return The specific role's descriptor.
     */
    public RoleDescriptorType getStandardMetadata(EntityDescriptorElement standardJaxbEntity) {
        return standardJaxbEntity.getValue()
                .getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()
                .stream()
                .filter(matchesStandardJaxbRole())
                .findAny()
                .orElseThrow(() ->
                        new IllegalStateException("Unable to find " + name() + " role in the entity provider"));
    }

    /**
     * Retrieves the current role's metadata from the extended entity config.
     *
     * @param extendedJaxbEntity The extended entity metadata.
     * @return The map representation of the extended config for the current role.
     */
    public Map<String, List<String>> getExtendedMetadata(EntityConfigElement extendedJaxbEntity) {
        return extendedJaxbEntity.getValue()
                .getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig()
                .stream()
                .filter(matchesExtendedJaxbRole())
                .findAny()
                .map(config -> {
                    Map<String, List<String>> attributes = SAML2MetaUtils.getAttributes(config);
                    attributes.put("metaAlias", singletonList(config.getValue().getMetaAlias()));
                    return attributes;
                })
                .orElseGet(HashMap::new);
    }

    /**
     * Returns the predicate that can be used to find the current role's standard metadata.
     *
     * @return The predicate for the role's standard metadata.
     */
    public Predicate<RoleDescriptorType> matchesStandardJaxbRole() {
        return jaxbRole -> jaxbRole.getClass().isAssignableFrom(standardJaxbRoleClass);
    }

    /**
     * Returns the predicate that can be used to find the current role's extended metadata.
     *
     * @return The predicate for the role's extended metadata.
     */
    public Predicate<JAXBElement<BaseConfigType>> matchesExtendedJaxbRole() {
        return jaxbRole -> jaxbRole.getClass().isAssignableFrom(extendedJaxbRoleClass);
    }

    /**
     * Returns new instance of standard metadata role.
     *
     * @return new standard metadata role
     */
    public RoleDescriptorType createNewStandardMetadataRole() {
        return standardMetadataCreator.get();
    }

    /**
     * Returns new instance of extended metadata role.
     *
     * @return new extended metadata role
     */
    public JAXBElement<BaseConfigType> createNewExtendedMetadataRole() {
        BaseConfigType baseConfig = Factories.ENTITY_CONFIG.createBaseConfigType();
        return extendedMetadataCreator.apply(baseConfig);
    }

    /**
     * Checks if this role is present on an {@link EntityDescriptorElement}.
     *
     * @param entityDescriptor the entity descriptor.
     * @return true if role is present, else false.
     */
    public boolean isPresent(EntityDescriptorElement entityDescriptor) {
        return entityDescriptor.getValue()
                .getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()
                .stream()
                .anyMatch(matchesStandardJaxbRole());
    }
}
