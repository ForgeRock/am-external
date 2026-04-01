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
package org.forgerock.openam.auth.node.api.upgrade.exception;

import org.forgerock.openam.annotations.Evolving;

/**
 * Exception thrown when a node upgrade fails.
 * This exception is used to indicate that the upgrade process for a node has failed,
 * either due to the absence of an upgrader for the specified version or due to a failure
 * during the upgrade process.
 */
@Evolving
public class NodeUpgradeException extends Exception {

    private final UpgradeFailureReason reason;

    /**
     * Constructs a new NodeUpgradeException with the specified reason.
     *
     * @param reason the reason for the upgrade failure.
     */
    public NodeUpgradeException(UpgradeFailureReason reason) {
        super();
        this.reason = reason;
    }

    /**
     * Constructs a new NodeUpgradeException with the specified reason and cause.
     *
     * @param reason the reason for the upgrade failure.
     * @param e the cause of the upgrade failure.
     */
    public NodeUpgradeException(UpgradeFailureReason reason, Exception e) {
        super(e);
        this.reason = reason;
    }

    /**
     * Returns the reason for the upgrade failure.
     *
     * @return the reason for the upgrade failure.
     */
    public UpgradeFailureReason reason() {
        return reason;
    }

    /**
     * Enum representing the possible reasons for a node upgrade failure.
     */
    public enum UpgradeFailureReason {
        /**
         * No upgrader is available for the specified version.
         */
        UNKNOWN_VERSION,
        /**
         * The upgrade process failed for some reason.
         */
        UPGRADE_FAILED,
        /**
         * The upgrader is missing metadata.
         */
        NO_UPGRADE_METADATA,
        /**
         * The upgrader is not compatible with the provided configuration.
         */
        INVALID_CONFIGURATION,
        /**
         * The upgrade is not supported for the current node version.
         */
        UNSUPPORTED_UPGRADE
    }
}
