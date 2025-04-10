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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.framework.typeadapters;

import static org.forgerock.openam.sm.annotations.adapters.AdapterUtils.validateSingleValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.openam.auth.nodes.framework.valueproviders.InnerTreeChoiceValues;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.annotations.adapters.AttributeSchemaBuilder;
import org.forgerock.openam.sm.annotations.adapters.TypeAdapter;
import org.forgerock.openam.sm.annotations.model.AttributeSyntax;
import org.forgerock.openam.sm.annotations.model.AttributeType;
import org.forgerock.openam.sm.annotations.model.UiType;


import io.vavr.control.Either;

/**
 * Type adapter for {@link Tree}. The inner tree is converted to/from a name.
 */
public final class InnerTreeTypeAdapter implements TypeAdapter<String> {

    private final TreeProvider treeProvider;

    /**
     * Default constructor for handling choices on the UI for the InnerTreeEvaluatorNode.
     * @param treeProvider Service for tree data management.
     */
    @Inject
    public InnerTreeTypeAdapter(TreeProvider treeProvider) {
        this.treeProvider = treeProvider;
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
                .addDynamicChoiceValues(InnerTreeChoiceValues.class);
    }

    @Override
    public Set<String> convertToStrings(Type type, String value, Optional<Annotation> annotation) {
        return Collections.singleton(value);
    }


    @Override
    public Either<IllegalStateException, String> convertFromStrings(Type type, Optional<Realm> realm, Set<String> value,
            Optional<Annotation> annotation) {
        if (realm.isEmpty()) {
            throw new IllegalStateException("Trees can only be used in realms");
        }
        return validateSingleValue(value).map(v -> treeProvider.getTree(realm.get(), v)
                .orElseThrow(() -> new IllegalStateException("Tree could not be found"))
                .getName());
    }
}
