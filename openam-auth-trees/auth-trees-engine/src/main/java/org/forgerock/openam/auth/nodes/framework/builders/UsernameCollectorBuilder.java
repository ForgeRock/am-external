/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.UsernameCollectorNode;

/**
 * A UsernameCollectorNode builder.
 */
public class UsernameCollectorBuilder extends AbstractNodeBuilder implements UsernameCollectorNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "User Name Collector";

    /**
     * A UsernameCollectorBuilder constructor.
     */
    public UsernameCollectorBuilder() {
        super(DEFAULT_DISPLAY_NAME, UsernameCollectorNode.class);
    }
}
