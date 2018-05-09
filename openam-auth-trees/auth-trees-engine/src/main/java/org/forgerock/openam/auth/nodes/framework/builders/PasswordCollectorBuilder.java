/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.PasswordCollectorNode;

/**
 * A PasswordCollectorNode builder.
 */
public class PasswordCollectorBuilder extends AbstractNodeBuilder implements PasswordCollectorNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Password Collector";

    /**
     * A PasswordCollectorBuilder constructor.
     */
    public PasswordCollectorBuilder() {
        super(DEFAULT_DISPLAY_NAME, PasswordCollectorNode.class);
    }
}
