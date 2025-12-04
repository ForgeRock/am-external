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

import java.util.Locale;
import java.util.Map;

import org.forgerock.openam.auth.nodes.KbaCreateNode;

/**
 * A builder for the {@link KbaCreateNode}.
 */
public class KbaCreateBuilder extends AbstractNodeBuilder implements KbaCreateNode.Config {

    /**
     * Default display name.
     */
    public static final String DEFAULT_DISPLAY_NAME = "KBA Definition";
    private Map<Locale, String> message;

    /**
     * Construct a {@link KbaCreateBuilder}.
     */
    public KbaCreateBuilder() {
        super(DEFAULT_DISPLAY_NAME, KbaCreateNode.class);
    }

    @Override
    public Map<Locale, String> message() {
        return message;
    }

    /**
     * Set the message translations for this node.
     *
     * @param message the message translations for this node
     * @return this builder
     */
    public KbaCreateBuilder message(Map<Locale, String> message) {
        this.message = message;
        return this;
    }
}
