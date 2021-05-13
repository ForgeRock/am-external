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

package org.forgerock.openam.federation.rest;

import java.util.Optional;

import org.forgerock.openam.saml2.Saml2EntityRole;

import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;

/**
 * A simple model object encompassing the extended metadata {@link EntityConfigElement}
 * and the standard metadata {@link EntityDescriptorElement}.
 *
 * @since 7.0.0
 */
public final class JaxbEntity {

    private final EntityDescriptorElement standardMetadata;
    private final EntityConfigElement extendedMetadata;

    /**
     * Creates an instance of the JaxbEntity.
     *
     * @param standardMetadata The saml2 entity standard metadata element.
     * @param extendedMetadata The saml2 entity extended metadata element.
     */
    public JaxbEntity(EntityDescriptorElement standardMetadata, EntityConfigElement extendedMetadata) {
        this.extendedMetadata = extendedMetadata;
        this.standardMetadata = standardMetadata;
    }

    /**
     * Get the saml2 entity standard metadata element.
     *
     * @return The saml2 entity standard metadata element.
     */
    public EntityDescriptorElement getStandardMetadata() {
        return standardMetadata;
    }

    /**
     * Get the saml2 entity extended metadata element.
     *
     * @return The saml2 entity extended metadata element.
     */
    public EntityConfigElement getExtendedMetadata() {
        return extendedMetadata;
    }

    /**
     * Check if a saml2 entity is hosted.
     *
     * @return true if hosted, else false.
     */
    public boolean isHosted() {
        return extendedMetadata == null ? false : extendedMetadata.getValue().isHosted();
    }

    /**
     * Checks if the jaxb entity has the role.
     *
     * @param role the saml2 entity role.
     * @return true if saml2 entity has the role, else false.
     */
    public boolean hasRole(Saml2EntityRole role) {
        return getStandardMetadata().getValue()
                .getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()
                .stream()
                .anyMatch(role.matchesStandardJaxbRole());
    }

    /**
     * Finds the matching jaxb role in the jaxb entity for a given saml entity role.
     *
     * @param role the saml2 entity role.
     * @return the jaxb role wrapped in an {@link Optional}
     */
    public Optional<RoleDescriptorType> findRole(Saml2EntityRole role) {
        return getStandardMetadata()
                .getValue()
                .getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()
                .stream()
                .filter(role.matchesStandardJaxbRole())
                .findFirst();
    }

    /**
     * Adds a saml2 role to jaxb entity.
     * If the entity provider already has the role, the role will not be added.
     *
     * @param role the saml2 entity role
     */
    public void addRole(Saml2EntityRole role) {
        if (!hasRole(role)) {
            getStandardMetadata().getValue()
                    .getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()
                    .add(role.createNewStandardMetadataRole());
            getExtendedMetadata().getValue().getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig()
                    .add(role.createNewExtendedMetadataRole());
        }
    }

    /**
     * Removes a saml2 role from jaxb entity.
     *
     * @param role the saml2 entity role.
     */
    public void removeRole(Saml2EntityRole role) {
        if (hasRole(role)) {
            getStandardMetadata().getValue()
                    .getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()
                    .removeIf(role.matchesStandardJaxbRole());
            getExtendedMetadata().getValue()
                    .getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig()
                    .removeIf(role.matchesExtendedJaxbRole());
        }
    }
}
