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
package org.forgerock.openam.auth.nodes.adapters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;

import org.forgerock.openam.auth.nodes.validators.PolicySetValidator;
import org.forgerock.openam.sm.annotations.adapters.AttributeSchemaBuilder;
import org.forgerock.openam.sm.annotations.adapters.StringTypeAdapter;
import org.forgerock.openam.sm.annotations.model.AttributeType;
import org.forgerock.openam.sm.annotations.model.UiType;

import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * A type adapter for policy set selector attributes.
 */
public class PolicySetTypeAdapter extends StringTypeAdapter {

    @Override
    public AttributeType getType(Type type) {
        return AttributeType.SINGLE_CHOICE;
    }

    @Override
    public Optional<UiType> getUiType(Type type) {
        return Optional.of(UiType.SCRIPT_SELECT);
    }

    @Override
    public void augmentAttributeSchema(Type type, AttributeSchemaBuilder attributeSchemaBuilder,
            Optional<Annotation> annotation) {
        attributeSchemaBuilder.addDynamicChoiceValues(PolicySetChoiceValues.class);
    }

    @Override
    public Set<Class<? extends ServiceAttributeValidator>> getValidators(Type type, Optional<Annotation> annotation) {
        return Set.of(PolicySetValidator.class);
    }
}
