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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.script;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.openam.annotations.Supported;

/**
 * Container for Tree information to be accessed in scripts.
 */
@Supported(scriptingApi = true, javaApi = false)
public class ScriptedTreeWrapper {

    private final Tree tree;

    /**
     * Constructs a new {@link ScriptedTreeWrapper}.
     * @param tree the tree to be wrapped.
     */
    public ScriptedTreeWrapper(Tree tree) {
        this.tree = tree;
    }

    /**
     * Returns the tree name.
     * @return the name of the tree.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String name() {
        return tree.getName();
    }

    /**
     * Returns the tree identity resource.
     * @return the identity resource to be managed by the tree.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public String identityResource() {
        return tree.getIdentityResource();
    }

    /**
     * Whether the tree can only be executed as an inner tree.
     * @return true if the tree can only be executed as an inner tree.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public boolean innerJourney() {
        return tree.innerTreeOnly();
    }

    /**
     * Whether the tree is a must run tree.
     * @return true if the tree is a must run tree.
     */
    @Supported(scriptingApi = true, javaApi = false)
    public boolean mustRun() {
        return tree.mustRun();
    }
}
