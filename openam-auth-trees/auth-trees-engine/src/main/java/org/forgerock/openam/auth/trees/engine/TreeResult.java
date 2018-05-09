/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.trees.engine;

import java.util.List;

import javax.security.auth.callback.Callback;

import org.forgerock.util.Reject;

/**
 * Immutable container for the result of processing an authentication tree.
 *
 * @since AM 5.5.0
 */
public final class TreeResult {
    /** The state of the tree after processing. */
    public final TreeState treeState;
    /** The outcome of the tree processing. */
    public final Outcome outcome;
    /** Callbacks requested by a node when the Outcome is {@link Outcome#NEED_INPUT}. May be null. */
    public final List<Callback> callbacks;

    /**
     * Constructs a new TreeResult.
     *
     * @param treeState The state of the tree after processing.
     * @param outcome The outcome of the tree processing.
     * @param callbacks Callbacks requested by a node when the Outcome is {@link Outcome#NEED_INPUT}. May be null.
     */
    public TreeResult(TreeState treeState, Outcome outcome, List<Callback> callbacks) {
        Reject.ifNull(treeState, outcome);
        this.treeState = treeState;
        this.outcome = outcome;
        this.callbacks = callbacks;
    }
}
