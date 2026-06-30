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
 * Copyright 2018-2026 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.framework.typeadapters;

import jakarta.inject.Inject;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.openam.auth.nodes.framework.valueproviders.InnerTreeChoiceValues;

/**
 * Type adapter for {@link Tree}. The inner tree is converted to/from a name.
 */
public final class InnerTreeTypeAdapter extends TreeTypeAdapter {

    /**
     * Default constructor for handling choices on the UI for the InnerTreeEvaluatorNode.
     * @param treeProvider Service for tree data management.
     */
    @Inject
    public InnerTreeTypeAdapter(@TreeProvider.Cached TreeProvider treeProvider) {
        super(treeProvider);
    }

    @Override
    protected Class<?> getChoiceValues() {
        return InnerTreeChoiceValues.class;
    }
}
