/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
