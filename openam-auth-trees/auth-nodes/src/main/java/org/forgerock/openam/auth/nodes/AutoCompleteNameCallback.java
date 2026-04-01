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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import java.util.List;

import javax.security.auth.callback.NameCallback;

/**
 * This is an AM specific NameCallback that supports autocomplete options.
 * <p>
 * When serialized to JSON, it will appear as a {@link javax.security.auth.callback.NameCallback} with an additional
 * attribute. This attribute will be ignored by the UI and SDKs that do not understand it. If the list is empty, the
 * attribute will not be present in the serialized JSON.
 * <p>
 * When that object is deserialized by AM after the user has responded, it will be deserialized back into a
 * {@link javax.security.auth.callback.NameCallback}, ignoring the autocomplete options.
 */
public class AutoCompleteNameCallback extends NameCallback {

    private final List<String> autocompleteValues;

    /**
     * Construct a {@code AutoCompleteNameCallback} with a prompt and a list of autocomplete fields.
     *
     * @param prompt the prompt used to request the name.
     * @param autocompleteValues the list of autocomplete values.
     *
     * @exception IllegalArgumentException if {@code prompt} is null empty.
     */
    public AutoCompleteNameCallback(String prompt, List<String> autocompleteValues) {
        super(prompt);
        this.autocompleteValues = autocompleteValues == null ? List.of() : List.copyOf(autocompleteValues);
    }

    /**
     * Get the autocomplete values.
     * @return the autocomplete values.
     */
    public List<String> getAutocompleteValues() {
        return autocompleteValues;
    }
}
