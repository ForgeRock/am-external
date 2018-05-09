/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/admin/utils/form/bindSavePromiseToElement
  */
define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages"
], ($, _, Messages) =>

/**
      * Binds a promise representing a save to a button element, visualising it's state.
      * <p>
      * Intented to be used in conjunction with the <code>_JSONSchemaFooter.html</code> partial.
      * @param  {Promise} promise Save promise. Usually a promise from an AJAX request
      * @param  {HTMLElement} element The button element visualising the promise's state
      * @example
      * clickHandler: function (event) {
      *   const promise = Service.update(this.data);
      *   bindSavePromiseToElement(promise, event.currentTarget);
      * }
      */
    (promise, element) => {
        element = $(element);
        element.prop("disabled", true);
        element.width(element.width());

        const span = element.find("span");
        const text = span.text();
        const elementClass = element.attr("class");

        span.fadeOut(300, () => {
            span.empty();
            span.removeClass().addClass("fa fa-refresh fa-spin");
            span.fadeIn(300);

            promise.done(() => {
                span.removeClass().addClass("fa fa-check fa-fw");
            }).fail((response) => {
                Messages.addMessage({ type: Messages.TYPE_DANGER, response });
                span.removeClass().addClass("fa fa-times fa-fw");
                element.removeClass().addClass("btn btn-danger");
            }).always(() => {
                _.delay(() => {
                    span.fadeOut(300, () => {
                        span.removeClass().text(text);
                        element.removeClass().addClass(elementClass).prop("disabled", false);
                        span.fadeIn(300);
                    });
                }, 1000);
            });
        });
    }
);
