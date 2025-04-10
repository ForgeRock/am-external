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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import java.util.Set;

import org.forgerock.openam.auth.nodes.PatchObjectNode;

/**
 * The {@link PatchObjectNode} builder.
 */
public class PatchObjectBuilder extends AbstractNodeBuilder implements PatchObjectNode.Config {
    private static final String DEFAULT_DISPLAY_NAME = "Patch Object";

    private String identityResource;
    private String identityAttribute;
    private boolean patchAsObject;
    private Set<String> ignoredFields;

    /**
     * Construct a {@link PatchObjectNode}.
     */
    public PatchObjectBuilder() {
        super(DEFAULT_DISPLAY_NAME, PatchObjectNode.class);
    }

    /**
     * Sets the identity resource for object patching in IDM.
     *
     * @param identityResource the IDM identity resource, e.g. "managed/user"
     * @return this builder
     */
    public PatchObjectBuilder identityResource(String identityResource) {
        this.identityResource = identityResource;
        return this;
    }

    /**
     * Sets the attribute to query the IDM object by.
     *
     * @param identityAttribute the identity attribute of the IDM object
     * @return this builder
     */
    public PatchObjectBuilder identityAttribute(String identityAttribute) {
        this.identityAttribute = identityAttribute;
        return this;
    }

    /**
     * Sets whether the patch should be done as object.
     *
     * @param patchAsObject the patch value
     * @return true if patch should be done as object
     */
    public PatchObjectBuilder patchAsObject(boolean patchAsObject) {
        this.patchAsObject = patchAsObject;
        return this;
    }

    /**
     * Sets the set of fields to ignore.
     *
     * @param ignoredFields set of fields to ignore
     * @return the ignored fields
     */
    public PatchObjectBuilder ignoredFields(Set<String> ignoredFields) {
        this.ignoredFields = ignoredFields;
        return this;
    }

    @Override
    public String identityResource() {
        return this.identityResource;
    }

    @Override
    public String identityAttribute() {
        return identityAttribute;
    }

    @Override
    public boolean patchAsObject() {
        return patchAsObject;
    }

    @Override
    public Set<String> ignoredFields() {
        return ignoredFields;
    }
}
