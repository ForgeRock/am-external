/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

/**
 * To be used when an exception has occurred in a session hook.
 */
public class TreeHookException extends Exception {
    /**
     * Default constructor that constructs the TreeHookException.
     */
    public TreeHookException() {
        super();
    }

    /**
     * Constructs the TreeHookException taking the information from a given
     * throwable object.
     *
     * @param e Throwable we want to wrap into a TreeHookException.
     */
    public TreeHookException(Throwable e) {
        super(e);
    }
}
