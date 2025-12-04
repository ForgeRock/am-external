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
package org.forgerock.openam.auth.nodes.builders;

import org.forgerock.openam.auth.nodes.CreateObjectNode;

/**
 * The CreateObjectNode Builder.
 */
public class CreateObjectBuilder extends AbstractNodeBuilder implements CreateObjectNode.Config  {

    private static final String DEFAULT_DISPLAY_NAME = "Create Object";
    private String identityResource;

    /**
     * The CreateObjectBuilder constructor.
     */
    public CreateObjectBuilder() {
        super(DEFAULT_DISPLAY_NAME, CreateObjectNode.class);
    }

    /**
     * Set the identity resource for object creation in IDM.
     * @param identityResource the identity resource, e.g. "managed/user"
     * @return this builder.
     */
    public CreateObjectBuilder identityResource(String identityResource) {
        this.identityResource = identityResource;
        return this;
    }

    @Override
    public String identityResource() {
        return this.identityResource;
    }
}
