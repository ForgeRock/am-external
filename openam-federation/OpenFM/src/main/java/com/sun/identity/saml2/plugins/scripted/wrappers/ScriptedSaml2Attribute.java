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
 * Copyright 2026 Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins.scripted.wrappers;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.plugins.AttributeMapperPluginHelper;

/**
 * A simplified version of {@link Attribute} that is more suitable for serialising into JSON for scripts.
 */
public class ScriptedSaml2Attribute {

    private static final AttributeMapperPluginHelper helper = new AttributeMapperPluginHelper();

    @JsonProperty
    private final String name;

    @JsonProperty
    private final String nameFormat;

    @JsonProperty
    private final List<String> values;

    /**
     * Constructor used when deserializing from JSON.
     */
    @JsonCreator
    public ScriptedSaml2Attribute(
            @JsonProperty("name") String name,
            @JsonProperty("nameFormat") String nameFormat,
            @JsonProperty("values") List<String> values
    ) {
        this.name = name;
        this.nameFormat = nameFormat;
        this.values = values;
    }

    /**
     * Constructor used when creating from an existing SAML {@link Attribute}.
     *
     * @param attribute Attribute to use.
     */
    public ScriptedSaml2Attribute(Attribute attribute) {
        this.name = attribute.getName();
        this.nameFormat = attribute.getNameFormat();
        this.values = attribute.getAttributeValueString();
    }

    /**
     * Return as a full SAML {@link Attribute}.
     *
     * @throws SAML2Exception
     */
    public Attribute toAttribute() throws SAML2Exception {
        return helper.createSAMLAttribute(name, nameFormat, Set.copyOf(values));
    }

    public String name() {
        return name;
    }

    public String nameFormat() {
        return nameFormat;
    }

    public List<String> values() {
        return values;
    }
}
