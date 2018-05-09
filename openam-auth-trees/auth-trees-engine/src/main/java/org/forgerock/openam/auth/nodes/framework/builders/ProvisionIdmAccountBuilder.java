/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.ProvisionIdmAccountNode;

/**
 * A Provision IDM Account builder.
 */
public class ProvisionIdmAccountBuilder extends AbstractNodeBuilder implements ProvisionIdmAccountNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Provision IDM Account";

    /**
     * A ProvisionDynamic Account constructor.
     */
    public ProvisionIdmAccountBuilder() {
        super(DEFAULT_DISPLAY_NAME, ProvisionIdmAccountNode.class);
    }
}