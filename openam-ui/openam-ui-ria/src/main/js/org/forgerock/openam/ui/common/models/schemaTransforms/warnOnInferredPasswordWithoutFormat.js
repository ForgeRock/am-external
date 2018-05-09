/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([], () => {
    /**
     * Warns if a property is inferred to be a password and does not have a format of password
     * @param {Object} property Property to transform
     * @param {String} name Raw property name
     */
    return function warnOnInferredPasswordWithoutFormat (property, name) {
        const possiblePassword = name.toLowerCase().indexOf("password", name.length - 8) !== -1;
        const hasFormat = property.format === "password";
        if (property.type === "string" && possiblePassword && !hasFormat) {
            console.error(`[cleanJSONSchema] Detected (inferred) a password property "${name}" ` +
                "without format attribute of \"password\"");
        }
    };
});
