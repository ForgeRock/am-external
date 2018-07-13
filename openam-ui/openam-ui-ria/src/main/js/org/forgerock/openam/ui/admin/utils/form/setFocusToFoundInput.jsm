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
 * Copyright 2017 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/utils/form/setFocusToFoundInput
 */

/**
 * Forces the viewport to scroll to the form-group with focus set to the input (if enabled).
 * @param {element} element containing input to set focus on
 */
const setFocusToFoundInput = (element) => {
    const input = element.find("input");
    const inheritedInput = input.prop("disabled") === true;
    if (inheritedInput) {
        input.closest(".form-group").find("button").focus().blur();
    } else {
        input.focus();
    }
};

export default setFocusToFoundInput;
