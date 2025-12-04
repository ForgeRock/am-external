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

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.auth.nodes.AttributeCollectorNode;

/**
 * An {@link AttributeCollectorNode} builder.
 */
public class AttributeCollectorBuilder extends AbstractNodeBuilder implements AttributeCollectorNode.Config {

    /**
     * Default display name.
     */
    public static final String DEFAULT_DISPLAY_NAME = "Attribute Collector";
    private List<String> attributesToCollect = new ArrayList<>();
    private Boolean required;
    private Boolean validateInputs;
    private String identityAttribute;

    /** {@link AttributeCollectorBuilder} constructor. */
    public AttributeCollectorBuilder() {
        super(DEFAULT_DISPLAY_NAME, AttributeCollectorNode.class);
    }

    /**
     * Set the attributes to collect.
     *
     * @param attributesToCollect the attributes to collect
     * @return this builder
     */
    public AttributeCollectorBuilder attributesToCollect(List<String> attributesToCollect) {
        this.attributesToCollect.clear();
        this.attributesToCollect.addAll(attributesToCollect);
        return this;
    }

    @Override
    public List<String> attributesToCollect() {
        return attributesToCollect;
    }

    /**
     * Set the required flag.
     *
     * @param required if all attributes should be required
     * @return this builder
     */
    public AttributeCollectorBuilder required(Boolean required) {
        this.required = required;
        return this;
    }

    @Override
    public Boolean required() {
        return required;
    }

    /**
     * Set whether to validate inputs against IDM policy.
     *
     * @param validateInputs true iff inputs should be validated
     * @return this builder
     */
    public AttributeCollectorBuilder validateInputs(Boolean validateInputs) {
        this.validateInputs = validateInputs;
        return this;
    }

    @Override
    public Boolean validateInputs() {
        return validateInputs;
    }

    /**
     * Set the identity attributed used in a query filter to retrieve the user object.
     *
     * @param identityAttribute the identity attribute
     * @return this builder
     */
    public AttributeCollectorBuilder identityAttribute(String identityAttribute) {
        this.identityAttribute = identityAttribute;
        return this;
    }

    @Override
    public String identityAttribute() {
        return identityAttribute;
    }
}
