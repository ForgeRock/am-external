/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * Historically upon AM's API, a schema "format" attribute of "password" would signal a password field is required.
 * "password" however is an invalid value for "format" and fails schema validation via ajv.
 * This attribute is currently converted to "_isPassword" while support for custom "format" values is in progress.
 * @param {Object} property The property to inspect.
 * @returns {boolean} Whether the property is a password.
 * @see https://github.com/mozilla-services/react-jsonschema-form/issues/837
 * @see https://github.com/mozilla-services/react-jsonschema-form/pull/794
 */
const isPassword = (property) => property._isPassword === true;

export default isPassword;
