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
 * Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.federation.rest.schema;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.SAML2_ENTITY_ROLES_REQUEST_HANDLER;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;
import org.forgerock.openam.federation.rest.Saml2RequestHandler.HostedEntityContext;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.annotations.AttributePath;
import org.forgerock.openam.objectenricher.service.ReadOnlyValueMapper;
import org.forgerock.util.encode.Base64url;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.identity.saml2.jaxb.metadata.AttributeAuthorityDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.AuthnAuthorityDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.XACMLPDPDescriptorType;
import com.sun.identity.saml2.jaxb.metadataextquery.AttributeQueryDescriptorType;

/**
 * SAML2 entity schema.
 */
@Title(SAML2_ENTITY_ROLES_REQUEST_HANDLER + "entity.title")
@Description(SAML2_ENTITY_ROLES_REQUEST_HANDLER + "entity.description")
public class Saml2EntityProvider {

    /**
     * The Id.
     */
    @Title(SAML2_ENTITY_ROLES_REQUEST_HANDLER + "entity.id.title")
    @Description(SAML2_ENTITY_ROLES_REQUEST_HANDLER + "entity.id.description")
    @AttributePath(value = "/entityID", mapper = IdMapper.class)
    @JsonProperty("_id")
    private String id;

    /**
     * The Entity Id.
     */
    @Title(SAML2_ENTITY_ROLES_REQUEST_HANDLER + "entity.entityId.title")
    @Description(SAML2_ENTITY_ROLES_REQUEST_HANDLER + "entity.entityId.description")
    @AttributePath("/entityID")
    private String entityId;

    /**
     * The Location.
     */
    @Title(SAML2_ENTITY_ROLES_REQUEST_HANDLER + "entity.location.title")
    @Description(SAML2_ENTITY_ROLES_REQUEST_HANDLER + "entity.location.description")
    @AttributePath(value = "/entityID", mapper = LocationMapper.class)
    private Location location;

    /**
     * The Roles.
     */
    @Title(SAML2_ENTITY_ROLES_REQUEST_HANDLER + "entity.roles.title")
    @Description(SAML2_ENTITY_ROLES_REQUEST_HANDLER + "entity.roles.description")
    @AttributePath(value = "/roleDescriptorOrIDPSSODescriptorOrSPSSODescriptor", mapper = RoleMapper.class)
    private List<Role> roles;


    /**
     * Gets id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets entity id.
     *
     * @return the entity id
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * Sets entity id.
     *
     * @param entityId the entity id
     */
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    /**
     * Gets location.
     *
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets location.
     *
     * @param location the location
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * Gets roles.
     *
     * @return the roles
     */
    public List<Role> getRoles() {
        return roles;
    }

    /**
     * Sets roles.
     *
     * @param roles the roles
     */
    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    /**
     * Describes whether an entity is remote or hosted.
     */
    public enum Location {

        /**
         * Hosted entity provider.
         */
        hosted,
        /**
         * Remote entity provider.
         */
        remote
    }

    /**
     * Enum denoting the the entity provider role type.
     */
    public enum Role {

        /**
         * Identity Provider.
         */
        @JsonProperty("identityProvider")
        IDENTITY_PROVIDER(IDPSSODescriptorType.class),

        /**
         * Service Provider.
         */
        @JsonProperty("serviceProvider")
        SERVICE_PROVIDER(SPSSODescriptorType.class),

        /**
         * Attribute Query Provider.
         */
        @JsonProperty("attributeQueryProvider")
        ATTRIBUTE_QUERY_PROVIDER(AttributeQueryDescriptorType.class),

        /**
         * Attribute Authority.
         */
        @JsonProperty("attributeAuthority")
        ATTRIBUTE_AUTHORITY(AttributeAuthorityDescriptorType.class),

        /**
         * Authentication Authority.
         */
        @JsonProperty("authenticationAuthority")
        AUTHENTICATION_AUTHORITY(AuthnAuthorityDescriptorType.class),

        /**
         * XACML Policy Enforcement Point.
         */
        @JsonProperty("xacmlPolicyEnforcementPoint")
        XACML_POLICY_ENFORCEMENT_POINT(XACMLAuthzDecisionQueryDescriptorType.class),

        /**
         * XACML Policy Decision Point.
         */
        @JsonProperty("xacmlPolicyDecisionPoint")
        XACML_POLICY_DECISION_POINT(XACMLPDPDescriptorType.class);

        private final Class<? extends RoleDescriptorType> clazz;

        Role(Class<? extends RoleDescriptorType> clazz) {
            this.clazz = clazz;
        }

        /**
         * Infer the Role from the given element type.
         *
         * @param roleDescriptor The element to infer the Role from.
         * @return The Role.
         */
        public static Role of(RoleDescriptorType roleDescriptor) {
            return Arrays.stream(values())
                    .filter(type -> type.clazz.isAssignableFrom(roleDescriptor.getClass()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Can not find matching type"));
        }
    }

    /**
     * Mapper to base 64 encode the entity id.
     */
    public static final class IdMapper extends ReadOnlyValueMapper<String, String> {

        @Override
        public String map(String value, EnricherContext context) {
            return Base64url.encode(value.getBytes(UTF_8));
        }
    }

    /**
     * Mapper for mapping the Location of the entity provider.
     */
    public static final class LocationMapper extends ReadOnlyValueMapper<String, Location> {

        @Override
        public Location map(String entityId, EnricherContext context) {
            return context.as(HostedEntityContext.class).isHostedEntity(entityId)
                    ? Location.hosted : Location.remote;
        }
    }

    /**
     * Mapper for mapping RoleDescriptorType.
     */
    public static final class RoleMapper extends ReadOnlyValueMapper<List<RoleDescriptorType>, List<Role>> {

        @Override
        public List<Role> map(List<RoleDescriptorType> value, EnricherContext context) {
            return value.stream()
                    .map(Role::of)
                    .collect(Collectors.toList());
        }
    }

}
