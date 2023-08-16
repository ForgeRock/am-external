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

/**
 * Historically upon AM's API, a schema "format" attribute of "password" would signal a password field is required.
 * "password" however is an invalid value for "format" and fails schema validation via ajv.
 * This attribute is currently converted to "_isPassword" until we upgrade our react-jsonschema-form to 1.6.0+ and make
 * use of custom-validation via customFormats. TODO AME-18142.
 * @param {object} property The property to inspect.
 * @returns {boolean} Whether the property is a password.
 * @see https://github.com/mozilla-services/react-jsonschema-form/issues/837
 * @see https://react-jsonschema-form.readthedocs.io/en/latest/validation/#custom-validation
 */
const isPassword = (property) => property._isPassword === true;

export default isPassword;