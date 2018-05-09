/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.framework.typeadapters;

import static org.forgerock.openam.sm.annotations.adapters.AdapterUtils.validateSingleValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.forgerock.openam.auth.nodes.framework.valueproviders.InnerTreeChoiceValues;
import org.forgerock.openam.auth.trees.engine.AuthTree;
import org.forgerock.openam.auth.trees.engine.AuthTreeService;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.annotations.adapters.AttributeSchemaBuilder;
import org.forgerock.openam.sm.annotations.adapters.TypeAdapter;
import org.forgerock.openam.sm.annotations.model.AttributeSyntax;
import org.forgerock.openam.sm.annotations.model.AttributeType;
import org.forgerock.openam.sm.annotations.model.UiType;

import com.google.inject.Inject;

/**
 * Type adapter for {@link AuthTree}. The inner tree is converted to/from a name.
 */
public final class InnerTreeTypeAdapter implements TypeAdapter<String> {

    private final AuthTreeService authTreeService;

    /**
     * Default constructor for handling choices on the UI for the InnerTreeEvaluatorNode.
     * @param authTreeService Service for tree data management.
     */
    @Inject
    public InnerTreeTypeAdapter(AuthTreeService authTreeService) {
        this.authTreeService = authTreeService;
    }

    @Override
    public boolean isApplicable(Type type) {
        return true;
    }

    @Override
    public AttributeSyntax getSyntax(Type type) {
        return AttributeSyntax.STRING;
    }

    @Override
    public AttributeType getType() {
        return AttributeType.SINGLE_CHOICE;
    }

    @Override
    public void augmentAttributeSchema(Type type, AttributeSchemaBuilder attributeSchemaBuilder,
            Optional<Annotation> annotation) {
        attributeSchemaBuilder
                .setUiType(UiType.SCRIPT_SELECT)
                .addDynamicChoiceValues(InnerTreeChoiceValues.class);
    }

    @Override
    public Set<String> convertToStrings(Type type, String value, Optional<Annotation> annotation) {
        return Collections.singleton(value);
    }


    @Override
    public String convertFromStrings(Type type, Optional<Realm> realm, Set<String> value,
            Optional<Annotation> annotation) {
        if (!realm.isPresent()) {
            throw new IllegalStateException("Trees can only be used in realms");
        }
        return authTreeService.getTree(realm.get(), validateSingleValue(value))
                .orElseThrow(() -> new IllegalStateException("Tree could not be found"))
                .getName();
    }
}
