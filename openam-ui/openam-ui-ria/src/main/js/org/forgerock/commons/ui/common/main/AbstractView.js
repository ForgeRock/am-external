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
 * Copyright 2011-2018 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "backbone",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "ThemeManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "templates/common/DefaultBaseTemplate",
    "handlebars-template-loader/runtime",
    "org/forgerock/openam/ui/common/util/es6/unwrapDefaultExport"
], ($, _, Backbone, Configuration, Constants, EventManager, Router, ThemeManager, UIUtils,
    ValidatorsManager, DefaultBaseTemplate, Handlebars, unwrapDefaultExport) => {
    /**
     * @exports org/forgerock/commons/ui/common/main/AbstractView
     */

     /**
       Internal helper method shared by the default implementations of
       validationSuccessful and validationFailed
     */
    function validationStarted (event) {
        if (!event || !event.target) {
            return $.Deferred().reject();
        }

        return $.Deferred().resolve($(event.target));
    }

    /**
      Sets the enabled state of the submit button based on the validation status of the provided form
    */
    function validationCompleted (formElement) {
        var button = formElement.find("input[type=submit]");

        if (!button.length) {
            button = formElement.find("#submit");
        }
        if (button.length) {
            button.prop('disabled', !ValidatorsManager.formValidated(formElement));
        }
    }

    return Backbone.View.extend({

        /**
         * This params should be passed when creating new object, for example:
         * new View({el: "#someId", template: "templates/main.html"});
         */
        element: "#content",

        baseTemplate: DefaultBaseTemplate,

        /**
         * View mode: replace or append
         */
        mode: "replace",
        defaultEvents: {
            "validationSuccessful :input": "validationSuccessful",
            "validationReset :input": "validationSuccessful",
            "validationFailed :input": "validationFailed"
        },
        initialize: function () {
            this.data = this.data || {};
            _.extend(this.events, this.defaultEvents);
            this.delegateEvents();
        },

        /**
         * Change content of 'el' element with 'viewTpl',
         * which is compiled using 'data' attributes.
         */
        parentRender: function(callback) {
            this.callback = callback;

            const needsNewBaseTemplate = () => {
                return (Configuration.baseTemplate !== this.baseTemplate && !this.noBaseTemplate);
            };
            EventManager.registerListener(Constants.EVENT_REQUEST_RESEND_REQUIRED, () => {
                this.unlock();
            });
    
            ThemeManager.getTheme().then((theme) => {
                this.data.theme = theme;
    
                if (needsNewBaseTemplate()) {
                    const renderBaseTemplate = (baseTemplate) => {
                        UIUtils.renderTemplate(
                            baseTemplate,
                            $("#wrapper"),
                            _.extend({}, Configuration.globalData, this.data),
                            _.bind(this.loadTemplate, this),
                            "replace",
                            needsNewBaseTemplate);
                    };

                    this.baseTemplate = unwrapDefaultExport.default(this.baseTemplate);

                    if (_.isFunction(this.baseTemplate)) {
                        renderBaseTemplate(this.baseTemplate);
                    } else {
                        this.loadThemedTemplate(this.baseTemplate).then(renderBaseTemplate);
                    }
                } else {
                    this.loadTemplate();
                }
            });
        },

        loadTemplate: function() {
            var self = this,
                validateCurrent = function () {
                    if (!_.has(self, "route")) {
                        return true;
                    } else if (!self.route.url.length && Router.getCurrentHash().replace(/^#/, '') === "") {
                        return true;
                    } else if (self.route === Router.configuration.routes.login) {
                        /**
                         * Determines if the current route is a login route, in which case allow the route  to execute.
                         * This is due to OpenAM's requirement for two views rendering being rendered at the same time
                         * (an arbitrary view and a session expiry login dialog view layered above) where the route and
                         * the hash don't match.
                         */
                        return true;
                    } else {
                        return Router.getCurrentHash().replace(/^#/, '').match(self.route.url);
                    }
                };

            this.setElement($(this.element));
            this.$el.unbind();
            this.delegateEvents();

            if (Configuration.baseTemplate !== this.baseTemplate && !this.noBaseTemplate) {
                Configuration.setProperty("baseTemplate", this.baseTemplate);
                EventManager.sendEvent(Constants.EVENT_CHANGE_BASE_VIEW);
            }

            _.each(this.partials, (partial, name) => {
                Handlebars.registerPartial(name, partial);
            });

            const renderTemplate = (template) => {
                UIUtils.renderTemplate(
                    template,
                    self.$el,
                    _.extend({}, Configuration.globalData, self.data),
                    self.callback ? _.bind(self.callback, self) : _.noop(),
                    self.mode,
                    validateCurrent);
            }

            /**
             * Some views do no define a template. UIUtils.renderTemplate handles this for us
             * so the `undefined` template is simply passed straight along to #renderTemplate.
             */
            if (_.isFunction(self.template) || !self.template) {
                renderTemplate(self.template);
            } else {
                this.loadThemedTemplate(self.template).then(renderTemplate);
            }
        },

        loadThemedTemplate (path) {
            return ThemeManager.getTheme().then((theme) => {
                const importDefaultTemplate = (path) =>
                    import(`templates/${path}`).then(unwrapDefaultExport.default);
                const importThemeTemplate = (themePath, templatePath) =>
                    import(`themes/${themePath}templates/${templatePath}`).then(unwrapDefaultExport.default);

                if (theme.path.length > 0) {
                    return importThemeTemplate(theme.path, path).catch(() => {
                        console.log(`Loading custom template "${path}" failed. Falling back to default.`);
                        return importDefaultTemplate(path);
                    });
                } else {
                    return importDefaultTemplate(path);
                }
            });
        },

        rebind: function() {
            this.setElement($(this.element));
            this.$el.unbind();
            this.delegateEvents();
        },

        render: function(args, callback) {
            this.parentRender(callback);
        },


        /**
         * This is the default implementation of the function used to reflect that
         * a given field has passed validation. It is invoked via a the event system,
         * and can be overridden per-view as needed.
         */
        validationSuccessful: function (event) {
            validationStarted(event)
            .then(function (input) {
                if (input.data()["bs.popover"]) {
                    input.popover('destroy');
                }
                input.parents(".form-group").removeClass('has-feedback has-error');
                return input.closest("form");
            })
            .then(validationCompleted);
        },

        /**
         * This is the default implementation of the function used to reflect that
         * a given field has failed validation. It is invoked via a the event system,
         * and can be overridden per-view as needed.
         *
         * @param {object} details - "failures" entry lists all messages (localized) associated with this validation
         *                           failure
         */
        validationFailed: function (event, details) {
            validationStarted(event)
            .then(function (input) {
                input.parents(".form-group").addClass('has-feedback has-error');
                if (input.data()["bs.popover"]) {
                    input.data('bs.popover').options.content = '<i class="fa fa-exclamation-circle"></i> '
                        + details.failures.join('<br><i class="fa fa-exclamation-circle"></i> ');
                } else {
                    input.popover({
                        validationMessage: details.failures,
                        animation: false,
                        content: '<i class="fa fa-exclamation-circle"></i> '
                        + details.failures.join('<br><i class="fa fa-exclamation-circle"></i> '),
                        trigger:'focus hover',
                        placement:'top',
                        html: 'true',
                        template: '<div class="popover popover-error help-block" role="tooltip">' +
                            '<div class="arrow"></div><h3 class="popover-title"></h3>' +
                            '<div class="popover-content"></div></div>'
                    });
                }
                if (input.is(":focus")) {
                    input.popover("show");
                }
                return input.closest("form");
            })
            .then(validationCompleted);
        },

        // legacy; needed here to prevent breakage of views which have an event registered for this function
        onValidate: function() {
            console.warn("Deprecated use of onValidate method; Change to validationSuccessful / validationFailed");
        }
    });
});
