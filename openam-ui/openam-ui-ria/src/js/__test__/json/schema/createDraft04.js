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
 * Copyright 2018 ForgeRock AS.
 */

/**
 * Creates a Draft 04 JSON Schema.
 * @module support/json/schema/createDraft04
 * @returns {Object} A Draft 04 JSON Schema.
 */
const createDraft04 = () => ({
    properties: {
        one: {
            title: "First line",
            type: "string"
        },
        two: {
            title: "Second line",
            type: "string"
        },
        three: {
            title: "Third line",
            type: "string"
        }
    },
    required: ["one", "two"],
    type: "object"
});

export default createDraft04;
