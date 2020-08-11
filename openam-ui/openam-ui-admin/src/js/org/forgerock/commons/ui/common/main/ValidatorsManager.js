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
 * Copyright 2012-2019 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";

import validators from "config/validators";

/**
 * @exports org/forgerock/commons/ui/common/main/ValidatorsManager
 */

const ValidatorsManager = {
    configuration: {
        validators
    }
};

// custom event "validate" is always bound, along with these common events
ValidatorsManager.defaultEvents = "keyup change blur paste validate";

/**
    Binds events to elements of the DOM within the containerElement, using
    the attributes of those elements to indicate which events should be bound.

    Uses the validator functions defined as part of this module configuration
    to perform the validation for each element.
    @param {Object} containerElement - portion of the DOM containing input elements to validate
*/
ValidatorsManager.bindValidators = function (containerElement) {
    const _arguments = _.toArray(arguments);

    $(containerElement).find(":input").each(function () {
        ValidatorsManager.bindValidatorsForField(containerElement, $(this));
    });

    _.each(ValidatorsManager.afterBindValidators, _.bind(function (fn) {
        fn.apply(this, _arguments);
    }, this));
};

// array of functions to invoke (in order) after bindValidators is executed
// each function is called with the same arguments that are provided to bindValidators
ValidatorsManager.afterBindValidators = [
    // generic method for executing a callback function
    function () {
        _.each(_.toArray(arguments), (arg) => {
            if (_.isFunction(arg)) {
                arg();
            }
        });
    }
];

ValidatorsManager.bindValidatorsForField = function (containerElement, field) {
    const eventsToBind = `${ValidatorsManager.defaultEvents} ${field.attr("data-validator-event") || ""}`;

    // remove previously-bound event handlers on this element
    field.off(eventsToBind);
    // restrict the handler method so that multiple simultaneous events don't trigger repeat evaluation
    field.on(eventsToBind, _.debounce(() => {
        ValidatorsManager.evaluateAllValidatorsForField(field, containerElement);
    }, 100));
};

/**
    Trigger the custom "validate" event on every input field within a given container element
*/
ValidatorsManager.validateAllFields = function (containerElement) {
    containerElement.find(":input").trigger("validate");
};

/**
    Function that is executed in the (this) context of a given DOM element
    which is expected to have various attributes included to describe which
    validators should be evaluated on it.
*/
ValidatorsManager.evaluateAllValidatorsForField = function (element, container) {
    const validatorsRegistered = element.attr("data-validator");

    if (validatorsRegistered) {
        // wait for all promises to be resolved from the various valiators named on the element
        return $.when.apply($, _.map(validatorsRegistered.split(" "), (validatorName) => {
            return ValidatorsManager.evaluateValidator(validatorName, element, container);
        })).then(function () {
            const allFailures = _(arguments)
                .toArray()
                .flatten()
                .filter((value) => {
                    return value !== undefined;
                })
                .uniq()
                .value();

            if (allFailures.length) {
                ValidatorsManager.validationFailed(element, allFailures);
            } else {
                ValidatorsManager.validationSucceeded(element);
            }
            return ValidatorsManager.evaluateDependentFields(element, container);
        });
    } else {
        // if there are no validators, then this element is valid
        ValidatorsManager.validationSucceeded(element);
        return ValidatorsManager.evaluateDependentFields(element, container);
    }
};

/**
 * When an element as a data-validation-dependents attribute, then each
 * field id (separated by commas) listed in that attribute will be validated
 * Returns a promise which is resolved when all dependents are validated
*/
ValidatorsManager.evaluateDependentFields = function (element, container) {
    const dependentFields = element.attr("data-validation-dependents");

    if (dependentFields) {
        // _.toArray added here due to bug in phantomjs. Not necessary otherwise
        return $.when.apply($, _.toArray(container
            .find(":input")
            .filter(function () {
                return $.inArray($(this).attr("id"), dependentFields.split(",")) !== -1;
            })
            .map(function () {
                return ValidatorsManager.evaluateAllValidatorsForField($(this), container);
            })));
    } else {
        return $.Deferred().resolve();
    }
};

/**
 * Executes a particular validator registered in the module configuration.
 * Returns a promise which will be resolved with the array of validation failures
 * which may have resulted from the evaluation.
 */
ValidatorsManager.evaluateValidator = function (validatorName, element, container) {
    const deferred = $.Deferred();
    const validatorConfig = this.configuration.validators[validatorName];
    const parameters = [
        container, // the element containing the element as well as any related elements
        element, // the specific input within the form being validated
        _.bind(deferred.resolve, deferred) // resolve the deferred object with the validator callback response
    ];

    if (typeof validatorConfig === "undefined") {
        return;
    } else {
        validatorConfig.validator.apply(this, parameters.concat(_.toArray(arguments)));
        return deferred;
    }
};

ValidatorsManager.validationSucceeded = function (element) {
    element.attr("data-validation-status", "ok");
    element.data("validation-failures", []);
    element.trigger("validationSuccessful");
    element.trigger("customValidate");
};

ValidatorsManager.validationFailed = function (element, allFailures) {
    element.attr("data-validation-status", "error");
    element.data("validation-failures", allFailures);
    element.trigger("validationFailed", { failures: allFailures });
    element.trigger("customValidate");
};

ValidatorsManager.formValidated = function (formElement) {
    return formElement.find("[data-validation-status=error]:visible").length === 0;
};

/**
 * Returns a given set of fields back to their pre-bindValidators / evaluateValidator state
 */
ValidatorsManager.clearValidators = function (containerElement) {
    $(containerElement).find("[data-validation-status]").each(function () {
        $(this)
            .removeAttr("data-validation-status")
            .trigger("validationReset");
    });
};

export default ValidatorsManager;
