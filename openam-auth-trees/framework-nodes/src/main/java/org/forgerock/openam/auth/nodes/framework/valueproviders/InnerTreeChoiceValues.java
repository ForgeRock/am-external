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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework.valueproviders;

import jakarta.inject.Inject;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.openam.core.realms.RealmLookup;

/**
 * This class is used to retrieve the tree names and IDs from the auth tree service for display
 * in a drop-down UI component. It filters out trees that are marked as transactional only.
 *
 * @since 13.0.0
 */
public class InnerTreeChoiceValues extends TreeChoiceValues {

    @Inject
    InnerTreeChoiceValues(TreeProvider treeProvider, RealmLookup realmLookup) {
        super(treeProvider, realmLookup);
    }

    @Override
    protected boolean filterTree(Tree tree) {
        return !tree.isTransactionalOnly();
    }
}
