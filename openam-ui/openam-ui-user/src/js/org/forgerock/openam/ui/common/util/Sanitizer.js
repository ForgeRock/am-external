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
 * Copyright 2023 ForgeRock AS.
 */

import sanitizeHtml from "sanitize-html";

const baseConfig = {
    ...sanitizeHtml.defaults,
    allowedAttributes: {
        ...sanitizeHtml.defaults.allowedAttributes,
        "*": ["class", "style", "id"],
        a: ["href", "name", "target"],
        img: [...sanitizeHtml.defaults.allowedAttributes.img, "height", "alt"]
    },
    allowedTags: [...sanitizeHtml.defaults.allowedTags, "img"],
    allowedSchemesByTag: {
        ...sanitizeHtml.defaults.allowedSchemesByTag,
        img: [...sanitizeHtml.defaults.allowedSchemes, "data"]
    }
};

export function sanitize (content) {
    return sanitizeHtml(content, baseConfig);
}
