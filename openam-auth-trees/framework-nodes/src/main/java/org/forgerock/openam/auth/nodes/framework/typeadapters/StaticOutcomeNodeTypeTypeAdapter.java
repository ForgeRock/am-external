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
 * Copyright 2021-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.typeadapters;

import static org.forgerock.openam.sm.annotations.adapters.AdapterUtils.validateSingleValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.forgerock.openam.auth.nodes.framework.valueproviders.StaticOutcomeNodeTypeChoiceValues;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.annotations.adapters.AttributeSchemaBuilder;
import org.forgerock.openam.sm.annotations.adapters.TypeAdapter;
import org.forgerock.openam.sm.annotations.model.AttributeSyntax;
import org.forgerock.openam.sm.annotations.model.AttributeType;
import org.forgerock.openam.sm.annotations.model.UiType;

import io.vavr.control.Either;

/**
 * {@link TypeAdapter} for nodeTypes that use the {@link org.forgerock.openam.auth.node.api.StaticOutcomeProvider}.
 */
public class StaticOutcomeNodeTypeTypeAdapter implements TypeAdapter<String> {
    @Override
    public boolean isApplicable(Type type) {
        // Does not matter that this is overly permissive as long as is not added to the TypeAdapterGuiceModule.
        // If needed to be added to the GuiceModule, worth making a specific NodeType class.
        return true;
    }

    @Override
    public AttributeSyntax getSyntax(Type type) {
        return AttributeSyntax.STRING;
    }

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
        attributeSchemaBuilder
                .addDynamicChoiceValues(StaticOutcomeNodeTypeChoiceValues.class);
    }

    @Override
    public Set<String> convertToStrings(Type type, String value, Optional<Annotation> annotation) {
        return Collections.singleton(value);
    }

    @Override
    public Either<IllegalStateException, String> convertFromStrings(Type type, Optional<Realm> realm, Set<String> value,
            Optional<Annotation> annotation) {
        return validateSingleValue(value);
    }
}
