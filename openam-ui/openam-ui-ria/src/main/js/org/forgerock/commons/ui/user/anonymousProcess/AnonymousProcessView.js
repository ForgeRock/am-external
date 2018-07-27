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
 * Copyright 2015-2018 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "form2js/src/form2js",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/user/delegates/AnonymousProcessDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "templates/user/AnonymousProcessWrapper"
], function($, _, form2js,
    AbstractView,
    AnonymousProcessDelegate,
    Constants,
    EventManager,
    Router,
    URIUtils,
    ValidatorsManager,
    AnonymousProcessWrapper) {

    function getCurrentFragmentParamString () {
        var params = URIUtils.getCurrentFragmentQueryString();
        return _.isEmpty(params) ? "" : "&" + params;
    }

    /**
     * Given a position in the DOM, look for children elements which comprise a
     * boolean expression. Using all of those found with content, return a filter
     * string which represents them.
     */
    function walkTreeForFilterStrings(basicNode) {
        var groupValues,
            node = $(basicNode);

        if (node.hasClass("filter-value") && node.val().length > 0) {
            return node.attr('name') + ' eq "' + node.val().replace('"', '\\"') + '"';
        } else if (node.hasClass("filter-group")) {
            groupValues = _.chain(node.find(">.form-group>.filter-value, >.filter-group"))
                           .map(walkTreeForFilterStrings)
                           .filter(function (value) {
                               return value.length > 0;
                           })
                           .value();

            if (groupValues.length === 0) {
                return "";
            } else if (groupValues.length === 1) {
                return groupValues[0];
            }

            if (node.hasClass("filter-or")) {
                return "(" + groupValues.join(" OR ") + ")";
            } else {
                return "(" + groupValues.join(" AND ") + ")";
            }
        } else {
            return "";
        }
    }

    /**
     * @exports org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView
     *
     * To use this generic object, it is required that two properties be defined prior to initialization:
     * "processType" - the Anonymous Process type registered on the backend under the selfservice context
     * "i18nBase" - the base of the translation file that contains the entries specific to this instance
     *
     * Example:
     *
     *    new AnonymousProcessView.extend({
     *         processType: "reset",
     *         i18nBase: "common.user.passwordReset"
     *     });
     */

    var AnonymousProcessView = AbstractView.extend({
        baseTemplate: "user/AnonymousProcessBaseTemplate",
        template: AnonymousProcessWrapper,
        events: {
            "submit form": "formSubmit",
            "click #restart": "restartProcess",
            "change #userQuery :input": "buildQueryFilter",
            "keyup #userQuery :input": "buildQueryFilter",
            "customValidate #userQuery": "validateForm"
        },
        data: {
            i18n: {}
        },
        // included as part of the AnonymousProcessView object for test purposes
        walkTreeForFilterStrings: walkTreeForFilterStrings,

        getFormContent: function () {
            if (this.$el.find("form").attr("id") === "userQuery") {
                return {
                    queryFilter: this.$el.find(":input[name=queryFilter]").val()
                };
            } else {
                return form2js($(this.element).find("form")[0]);
            }
        },

        formSubmit: function(event) {
            var formContent = this.getFormContent();

            event.preventDefault();

            this.delegate.submit(formContent).then(
                _.bind(this.renderProcessState, this),
                _.bind(this.renderProcessState, this)
            );

        },

        render: function(args) {
            var params = Router.convertCurrentUrlToJSON().params;
            this.stateData = {};

            if (!this.delegate || args[0] !== "/continue") {
                this.setDelegate(Constants.SELF_SERVICE_CONTEXT + this.processType, params.token);
            }

            if (!_.isEmpty(params)) {
                this.submitDelegate(params, _.bind(function (response) {
                    // in the case when the token presented on the URL results in an unsuccessful
                    // response, do not simply redirect to /continue; doing so would reset the process
                    // and hide the reason for the failure from the user.
                    if (_.get(response, "status.success") === false) {
                        return AbstractView.prototype.parentRender.call(this, _.bind(function () {
                            this.renderProcessState(response);
                        }, this));
                    } else {
                        EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                            route: Router.currentRoute,
                            args: ["/continue", ""]
                        });
                    }
                }, this));
                return;
            }

            // TODO: The first undefined argument is the deprecated realm which is defined in the
            // CommonRoutesConfig login route. This needs to be removed as part of AME-11109.
            this.data.args = [undefined, getCurrentFragmentParamString()];
            this.setTranslationBase();
            this.parentRender();
        },

        parentRender: function () {
            AbstractView.prototype.parentRender.call(this, _.bind(function () {
                this.delegate.start().then(_.bind(this.renderProcessState, this));
            }, this));
        },

        renderResponse: function (response) {
            AbstractView.prototype.parentRender.call(this, _.bind(function () {
                this.renderProcessState(response);
            }, this))
        },

        setDelegate: function (endpoint, token, additional) {
            this.delegate = new AnonymousProcessDelegate(endpoint, token, additional);
        },

        submitDelegate: function (params, onSubmit) {
            this.delegate.submit(_.omit(params, "token")).always(onSubmit);
        },

        setTranslationBase: function () {
            _.each(["title", "completed", "failed", "tryAgain", "return"], function (key) {
                this.data.i18n[key] = this.i18nBase + "." + key;
            }, this);
        },

        restartProcess: function (e) {
            e.preventDefault();
            delete this.delegate;
            delete this.stateData;
            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                route: _.extend({}, Router.currentRoute, { forceUpdate: true })
            });
        },

        attemptCustomTemplate: function (stateData, response, processStatePromise) {
            this.loadThemedTemplate(`user/process/${this.processType}/${response.type}-${response.tag}.html`)
            .then((template) => {
                const renderedTemplate = template(stateData);
                processStatePromise.resolve(renderedTemplate);
            }, () => {
                this.loadGenericTemplate(stateData, response, processStatePromise);
            });
        },

        loadGenericTemplate: function (stateData, response, processStatePromise) {
            const templatePage = _.has(response, "requirements") ? "GenericInputForm" : "GenericEndPage";
            
            this.loadThemedTemplate(`user/process/${templatePage}.html`)
            .then((template) => {
                const renderedTemplate = template(stateData);
                processStatePromise.resolve(renderedTemplate);
            });
        },

        renderProcessState: function (response) {
            var processStatePromise = $.Deferred();

            if (_.has(response, "requirements")) {
                this.stateData = _.extend({
                    requirements: response.requirements
                }, this.data);
            } else {
                this.stateData = _.extend({
                    status: response.status,
                    additions: response.additions
                }, this.data);
            }

            if (_.has(response, "type") && _.has(response, "tag")) {
                this.attemptCustomTemplate(this.stateData, response, processStatePromise);
            } else {
                this.loadGenericTemplate(this.stateData, response, processStatePromise);
            }

            return processStatePromise.then(_.bind(function (content) {
                this.$el.find("#processContent").html(content);
                ValidatorsManager.bindValidators(this.$el, this.baseEntity, _.bind(function () {
                    ValidatorsManager.validateAllFields(this.$el);
                }, this));
                this.$el.find(":input:visible:first").focus();
            }, this));
        },

        buildQueryFilter: function () {
            this.$el.find(":input[name=queryFilter]")
                .val(walkTreeForFilterStrings(this.$el.find("#filterContainer")));
            this.validateForm();
        },
        validateForm: function () {
            var button = this.$el.find("input[type=submit]"),
                incompleteAndGroup = false;

            // there has to be some value in the queryFilter or the whole thing is invalid
            if (this.$el.find(":input[name=queryFilter]").val().length === 0) {
                button.prop("disabled", true);
                return;
            }

            // filter-and groups must have each of their children filled-out
            this.$el.find(".filter-and").each(function () {
                // if there are any values at all specified for this "and" group...
                if (walkTreeForFilterStrings(this).length > 0) {
                    // then we need to make sure that they are all populated
                    incompleteAndGroup = !(
                        // check all direct filter-value fields for content
                        _.reduce($(">.form-group>.filter-value", this), function (state, field) {
                            var hasValue = field.value.length > 0;
                            $(field).attr("data-validation-status", hasValue ? "ok" : "error");
                            return state && hasValue;
                        }, true) &&
                        // check all direct sub-groups for content
                        _.reduce($(">.filter-group", this), function (state, subGroup) {
                            return walkTreeForFilterStrings(subGroup).length > 0;
                        }, true)
                    );
                } else {
                    $(">.form-group>.filter-value", this).attr("data-validation-status", "ok");
                }
            });

            button.prop("disabled", incompleteAndGroup);
        }

    });

    return AnonymousProcessView;
});
