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
 * Copyright 2018-2019 ForgeRock AS.
 */

import Ajv from "ajv";

import schema from "./schema";

/**
 * Validates that the `themes` section of a theme configuration is valid.
 * @module org/forgerock/openam/ui/common/util/theme/validate
 * @param {object} configuration A theme configuration.
 * @example
 * import themeConfiguration from "config/ThemeConfiguration";
 * import validate from "org/forgerock/openam/ui/common/util/theme/validate";
 *
 * validate(themeConfiguration.themes);
 */
const validate = (configuration) => {
    const ajv = new Ajv();
    const validate = ajv.compile(schema);
    const isValid = validate(configuration);

    if (!isValid) {
        validate.errors.forEach(({ dataPath, message }) => {
            const errorMessage = dataPath ? `"${dataPath}" ${message}.` : `Configuration ${message}.`;
            throw new Error(`Theme configuration error. ${errorMessage}`);
        });
    }
};

export default validate;
