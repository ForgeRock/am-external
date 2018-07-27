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
 * Unwraps a ES6 module, returning the default export, if it existed.
 * @param {Object} module Module to unwrap
 * @returns {Object} Module, possibly unwrapped
 */
export default function unwrapDefaultExport (module) {
    if (module && module.default) {
        module = module.default;
    }

    return module;
}
