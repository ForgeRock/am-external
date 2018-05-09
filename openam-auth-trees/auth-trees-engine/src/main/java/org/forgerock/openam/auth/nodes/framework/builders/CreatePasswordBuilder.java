/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.CreatePasswordNode;

/**
 * A CreatePasswordNode builder.
 */
public class CreatePasswordBuilder extends AbstractNodeBuilder implements CreatePasswordNode.Config {

    private static final String DEFAULT_DISPLAY_NAME = "Create Password";
    private int minPasswordLength;

    /**
     * A CreatePasswordBuilder constructor.
     */
    public CreatePasswordBuilder() {
        super(DEFAULT_DISPLAY_NAME, CreatePasswordNode.class);
    }

    /**
     * A CreatePasswordBuilder constructor.
     * @param minPasswordLength Length of password to generate.
     * @return this builder.
     */
    public CreatePasswordBuilder minPasswordLength(int minPasswordLength) {
        this.minPasswordLength = minPasswordLength;
        return this;
    }

    @Override
    public int minPasswordLength() {
        return this.minPasswordLength;
    }

}