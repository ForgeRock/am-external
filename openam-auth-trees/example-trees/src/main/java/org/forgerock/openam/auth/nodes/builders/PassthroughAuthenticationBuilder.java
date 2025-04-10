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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import org.forgerock.openam.auth.nodes.PassthroughAuthenticationNode;

/**
 * The {@link PassthroughAuthenticationNode} builder.
 */
public class PassthroughAuthenticationBuilder extends AbstractNodeBuilder
        implements PassthroughAuthenticationNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Passthrough Authentication";

    private String systemEndpoint;
    private String objectType;
    private String identityAttribute;
    private String passwordAttribute;

    /**
     * Construct a {@link PassthroughAuthenticationNode}.
     */
    public PassthroughAuthenticationBuilder() {
        super(DEFAULT_DISPLAY_NAME, PassthroughAuthenticationNode.class);
    }

    /**
     * Sets the system endpoint for passthrough authN in IDM.
     *
     * @param systemEndpoint The name of the system endpoint (connector) that should be used for passthrough authN
     * @return this builder
     */
    public PassthroughAuthenticationBuilder systemEndpoint(String systemEndpoint) {
        this.systemEndpoint = systemEndpoint;
        return this;
    }

    /**
     * Sets the object type for the object to be authenticated.
     *
     * @param objectType The name of the ICF object type for the object to be authenticated
     * @return this builder
     */
    public PassthroughAuthenticationBuilder objectType(String objectType) {
        this.objectType = objectType;
        return this;
    }

    /**
     * Sets the username attribute for passthrough authN in IDM.
     *
     * @param identityAttribute The attribute used as the username for passthrough authN
     * @return this builder
     */
    public PassthroughAuthenticationBuilder identityAttribute(String identityAttribute) {
        this.identityAttribute = identityAttribute;
        return this;
    }

    /**
     * Sets the password attribute for passthrough authN in IDM.
     *
     * @param passwordAttribute The attribute used as the password for passthrough authN
     * @return this builder
     */
    public PassthroughAuthenticationBuilder passwordAttribute(String passwordAttribute) {
        this.passwordAttribute = passwordAttribute;
        return this;
    }

    @Override
    public String systemEndpoint() {
        return this.systemEndpoint;
    }

    @Override
    public String objectType() {
        return this.objectType;
    }

    @Override
    public String identityAttribute() {
        return identityAttribute;
    }

    @Override
    public String passwordAttribute() {
        return passwordAttribute;
    }
}
