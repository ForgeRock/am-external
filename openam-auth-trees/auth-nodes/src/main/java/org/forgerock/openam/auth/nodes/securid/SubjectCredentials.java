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
package org.forgerock.openam.auth.nodes.securid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents the subject credentials required for SecurID authentication.
 *
 * @param methodId        The identifier for the authentication method.
 * @param referenceId     The reference identifier for the authentication session.
 * @param collectedInputs A list of collected inputs required for authentication.
 */
@JsonInclude(NON_NULL)
record SubjectCredentials(String methodId, String referenceId, List<CollectedInput> collectedInputs) {
    /**
     * Represents a collected input required for SecurID authentication.
     *
     * @param name     The name of the input.
     * @param value    The value of the input.
     * @param dataType The data type of the input.
     */
    record CollectedInput(String name, String value, String dataType) {
    }
}
