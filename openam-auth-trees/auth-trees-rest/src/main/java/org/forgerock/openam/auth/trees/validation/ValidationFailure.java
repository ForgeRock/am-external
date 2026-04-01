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
package org.forgerock.openam.auth.trees.validation;

/**
 * Represents a validation failure with a message and severity.
 * @param message the validation failure message
 * @param severity the severity of the validation failure
 */
public record ValidationFailure(String message, Severity severity) {

    /**
     * The severity of a validation failure.
     */
    public enum Severity {
        /**
         * Indicates a warning severity.
         */
        WARNING("warnings"),
        /**
         * Indicates an error severity.
         */
        ERROR("errors");

        private final String displayName;

        Severity(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Get the display name of the severity.
         * @return the display name
         */
        public String displayName() {
            return displayName;
        }
    }
}
