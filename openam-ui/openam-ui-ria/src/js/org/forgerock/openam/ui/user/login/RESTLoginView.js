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
 * Copyright 2011-2020 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";
import Form2js from "form2js/src/form2js";
import Handlebars from "handlebars-template-loader/runtime";
import i18next from "i18next";

import {
    isNotDefaultPath,
    remove as removeGotoUrl,
    setValidated as setValidatedGotoUrl,
    toHref as gotoUrlToHref
} from "org/forgerock/openam/ui/user/login/gotoUrl";
import { parseParameters, urlParamsFromObject } from "org/forgerock/openam/ui/common/util/uri/query";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AuthNService from "org/forgerock/openam/ui/user/services/AuthNService";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import CookieHelper from "org/forgerock/commons/ui/common/util/CookieHelper";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import getCurrentFragmentParamString from "org/forgerock/openam/ui/common/util/uri/getCurrentFragmentParamString";
import isRealmChanged from "org/forgerock/openam/ui/common/util/isRealmChanged";
import logout from "org/forgerock/openam/ui/user/login/logout";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import processLoginRequest from "config/process/processLoginRequest";
import RESTLoginHelper from "org/forgerock/openam/ui/user/login/RESTLoginHelper";
import Router from "org/forgerock/commons/ui/common/main/Router";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";

function hasSsoRedirectOrPost (goto) {
    let decodedGoto;
    if (goto) {
        decodedGoto = decodeURIComponent(goto);
    }
    return goto && (_.startsWith(decodedGoto, "/SSORedirect") || _.startsWith(decodedGoto, "/SSOPOST"));
}

function populateTemplate () {
    const self = this;
    const firstUserNamePassStage = Configuration.globalData.auth.currentStage === 1 && this.userNamePasswordStage;

    // self-service links should be shown only on the first stage of the username/password stages
    this.data.showForgotPassword = firstUserNamePassStage && Configuration.globalData.forgotPassword === "true";
    this.data.showForgotUserName = firstUserNamePassStage && Configuration.globalData.forgotUsername === "true";
    this.data.showForgotten = this.data.showForgotPassword || this.data.showForgotUserName;
    this.data.showSelfRegistration = firstUserNamePassStage && Configuration.globalData.selfRegistration === "true";
    this.data.showRememberLogin = firstUserNamePassStage;
    // socialImplementations links should be shown only on the first stage of the username/password stages
    // and should not show on the upgrade session page
    this.data.showSocialLogin = firstUserNamePassStage && !Configuration.loggedUser &&
                                    !_.isEmpty(Configuration.globalData.socialImplementations);

    if (Configuration.hasRESTLoginDialog) {
        this.prefillLoginData();

        BootstrapDialog.show({
            title: $.t("common.form.sessionExpired"),
            cssClass: "login-dialog",
            closable: false,
            message: $("<div></div>"),
            onshow () {
                const dialog = this;
                // change the target element of the view
                self.noBaseTemplate = true;
                self.element = dialog.message;
            },
            onshown () {
                // return back to the default state
                delete self.noBaseTemplate;
                self.element = "#content";
            }
        });
    }
}

function routeToLoginUnavailable (fragmentParams) {
    // We cannot use the Router.getLink() method here and simply apply the subrealm to the route because
    // Router.getLink() does more than its title suggests. It also applies the default properties to the route and
    // these are not always correct if there has been a previous successful login request.
    // FIXME: Remove any session specific properties from the UI upon session end.
    Router.routeTo(Router.configuration.routes.loginFailure, {
        args: [fragmentParams],
        trigger: true
    });
}

/**
 * Checks if the callback "type" is present in the array of "callbacks" objects.
 * @param {Array.<Object>} callbacks array of callback objects
 * @param {String} type The callback type that is being checked
 * @returns {Boolean} if the callback "type" is present in the callbacks.
 */
function hasCallback (callbacks, type) {
    return _.some(callbacks, (callback) => callback.type === type);
}

const LoginView = AbstractView.extend({
    template: "openam/RESTLoginTemplate",
    baseTemplate: "common/LoginBaseTemplate",
    data: {},
    events: {
        "click input[type=submit]": "formSubmit"
    },

    handleExistingSession (requirements) {
        const element = this.$el;
        // If we have a token, let's see who we are logged in as....
        RESTLoginHelper.getLoggedUser((user) => {
            if (isRealmChanged()) {
                window.location.replace(`#switchRealm/${getCurrentFragmentParamString()}`);
            } else {
                Configuration.setProperty("loggedUser", user);
                if (isNotDefaultPath(requirements.successUrl)) {
                    setValidatedGotoUrl(requirements.successUrl);
                    window.location.href = gotoUrlToHref();
                    // This happens after we have already changed the href for situations where the goto url is
                    // taking a while to load, and so removes the login page from view.
                    element.empty();
                    return false;
                } else {
                    removeGotoUrl();
                }

                EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, {
                    anonymousMode: false
                });

                // Copied from loginRequest (previously EVENT_LOGIN_REQUEST handler)
                if (Configuration.gotoFragment &&
                        _.indexOf(["#", "", "#/", "/#"], Configuration.gotoFragment) === -1) {
                    console.log(`Auto redirect to ${Configuration.gotoFragment}`);
                    Router.navigate(Configuration.gotoFragment, { trigger: true });
                    delete Configuration.gotoFragment;
                } else {
                    Router.navigate("", { trigger: true });
                }
            }
        }, () => {
            logout();
        });
    },

    autoLogin () {
        const submitContent = {};
        const auth = Configuration.globalData.auth;
        let index;

        _.each(_.keys(auth.urlParams), (key) => {
            if (key.indexOf("IDToken") > -1) {
                index = parseInt(key.substring(7), 10) - 1;
                submitContent[`callback_${index}`] = auth.urlParams[`IDToken${key.substring(7)}`];
            }
        });
        auth.autoLoginAttempts = 1;
        processLoginRequest(submitContent);
    },

    isZeroPageLoginAllowed () {
        const referer = document.referrer;
        const whitelist = Configuration.globalData.zeroPageLogin.refererWhitelist;

        if (!Configuration.globalData.zeroPageLogin.enabled) {
            return false;
        }

        if (!referer) {
            return Configuration.globalData.zeroPageLogin.allowedWithoutReferer;
        }

        return !whitelist || !whitelist.length || whitelist.indexOf(referer) > -1;
    },

    formSubmit (e) {
        let expire;

        e.preventDefault();
        // disabled button before login
        $(e.currentTarget).prop("disabled", true);

        const submitContent = new Form2js(this.$el[0]);
        submitContent[$(e.target).attr("name")] = $(e.target).attr("index");

        // START CUSTOM STAGE-SPECIFIC LOGIC HERE

        // Known to be used by username/password based authn stages
        if (this.$el.find("[name=loginRemember]:checked").length !== 0) {
            expire = new Date();
            expire.setDate(expire.getDate() + 20);
            // An assumption that the login name is the first text input box
            CookieHelper.setCookie("login", this.$el.find("input[type=text]:first").val(), expire);
        } else if (this.$el.find("[name=loginRemember]").length !== 0) {
            CookieHelper.deleteCookie("login");
        }

        // END CUSTOM STAGE-SPECIFIC LOGIC HERE

        this.loginRequestFunction({
            submitContent,
            failureCallback () {
                // enabled the login button if login failure
                $(e.currentTarget).prop("disabled", false);
                $("input[type='password']").val("");
                // If its not the first stage then render the Login Unavailable view with link back to login screen.
                if (Configuration.globalData.auth.currentStage > 1) {
                    let fragmentParams = URIUtils.getCurrentFragmentQueryString();
                    if (fragmentParams) {
                        fragmentParams = `&${fragmentParams}`;
                    }
                    // Go to the Login Unavailable view with all the original fragment parameters.
                    routeToLoginUnavailable(fragmentParams);
                }
            }
        });
    },

    // Specifying realm as part of the fragment is not supported since 14.0.
    // This function removes the realm parameter from the fragment and puts it into the query string.
    // TODO: Should be removed once AME-11109 is resolved.
    moveLegacyRealmFragmentToQuery (fragmentParams) {
        const fragmentRealm = fragmentParams.realm;
        delete fragmentParams.realm;

        const fragmentWithoutRealm = `#login${
            _.isEmpty(fragmentParams) ? "" : `&${urlParamsFromObject(fragmentParams)}`
        }`;

        const queryStringWithUpdatedRealm = `?${urlParamsFromObject({
            realm: fragmentRealm,
            ...parseParameters(URIUtils.getCurrentQueryString())
        })}`;

        return `${URIUtils.getCurrentPathName()}${queryStringWithUpdatedRealm}${fragmentWithoutRealm}`;
    },

    getUrlWithoutNewSessionParameters () {
        const paramsWithoutNewSession = (paramString, separator) => {
            const params = parseParameters(paramString);
            if (params.arg === "newsession") {
                delete params.arg;
            }
            return _.isEmpty(params) ? "" : `${separator}${urlParamsFromObject(params)}`;
        };
        const query = paramsWithoutNewSession(URIUtils.getCurrentQueryString(), "?");
        const fragment = paramsWithoutNewSession(URIUtils.getCurrentFragmentQueryString(), "&");
        return `${URIUtils.getCurrentPathName()}${query}#login${fragment}`;
    },

    render (args) {
        const fragmentParams = parseParameters(URIUtils.getCurrentFragmentQueryString());

        if (fragmentParams.realm) {
            location.href = this.moveLegacyRealmFragmentToQuery(fragmentParams);
            return;
        }

        const addtionalArguments = args ? args[1] : undefined;
        let params = {};
        const auth = Configuration.globalData.auth;

        this.data.fragmentParamString = getCurrentFragmentParamString();

        // TODO: The first undefined argument is the deprecated realm which is defined in the
        // CommonRoutesConfig login route. This needs to be removed as part of AME-11109.
        this.data.args = [undefined, this.data.fragmentParamString];

        const queryObjectExclServiceParam = {
            ...fragmentParams,
            ...parseParameters(URIUtils.getCurrentQueryString())
        };
        delete queryObjectExclServiceParam["service"];
        this.data.queryStringExclServiceParam = "";
        this.data.queryStringExclServiceParam = _.isEmpty(queryObjectExclServiceParam)
            ? "" : `&${urlParamsFromObject(queryObjectExclServiceParam)}`;

        if (args) {
            auth.additional = addtionalArguments;
            auth.urlParams = {};
            params = this.handleParams();

            // If there are IDTokens try to login with the provided credentials
            if (params.IDToken1 && this.isZeroPageLoginAllowed() && !auth.autoLoginAttempts) {
                this.autoLogin();
            }
        }

        AuthNService.getRequirements().then(_.bind(function (reqs) {
            const hasNewSessionParameter = reqs.hasOwnProperty("tokenId") && params.arg === "newsession";
            if (hasNewSessionParameter) {
                const urlWithoutNewSessionParam = this.getUrlWithoutNewSessionParameters();
                logout().then(() => {
                    window.location.href = urlWithoutNewSessionParam;
                }, () => {
                    window.location.href = urlWithoutNewSessionParam;
                });
                return;
            }

            // If simply by asking for the requirements, we end up with a token,
            // then we must have already had a session
            if (reqs.hasOwnProperty("tokenId")) {
                this.handleExistingSession(reqs);
            } else if (reqs.hasOwnProperty("type") && reqs.type === "error" && reqs.hasOwnProperty("message")) {
                Messages.addMessage({ type: Messages.TYPE_DANGER, message: reqs.message });
                const paramString = URIUtils.getCurrentFragmentQueryString();
                routeToLoginUnavailable(RESTLoginHelper.filterUrlParams(parseParameters(paramString)));
            } else { // We aren't logged in yet, so render a form...
                this.loginRequestFunction = processLoginRequest;
                this.renderForm(reqs, params);
            }
        }, this), _.bind((error) => {
            if (_.has(error, "message")) {
                Messages.addMessage({ type: Messages.TYPE_DANGER, message: error.message });
            }

            if (_.has(error, "detail.failureUrl") && !_.isEmpty(error.detail.failureUrl)) {
                /**
                 * If there is a login failure which has occurred without a login form submission, e.g. Zero Page Login
                 * and a failureUrl is set (e.g. Failure URL node), route the user to that.
                 */
                window.location.href = error.detail.failureUrl;
            } else {
                /**
                 * We haven't managed to get a successful response from the server
                 * This could be due to many reasons, including that the params are incorrect
                 * For example requesting service=thewrongname. So here we use the RESTLoginHelper.filterUrlParams
                 * function to only return the params we wish to save. The authIndexType and authIndexValue
                 * would normally only be applied when the user has logged in, so they should not contain invalid values
                 */

                const paramString = URIUtils.getCurrentFragmentQueryString();
                routeToLoginUnavailable(RESTLoginHelper.filterUrlParams(parseParameters(paramString)));
            }
        }, this));
    },
    isUsernamePasswordStage (reqs) {
        const usernamePasswordStages = ["DataStore1", "AD1", "JDBC1", "LDAP1", "Membership1", "RADIUS1"];
        if (_.includes(usernamePasswordStages, reqs.stage)) {
            return true;
        }
        return (reqs.callbacks && hasCallback(reqs.callbacks, "NameCallback"));
    },
    renderForm (reqs, urlParams) {
        const requirements = _.clone(reqs);
        const promise = $.Deferred();
        const self = this;

        this.userNamePasswordStage = this.isUsernamePasswordStage(reqs);

        requirements.callbacks = [];

        _.each(reqs.callbacks, (element) => {
            let redirectForm;
            let redirectCallback;

            if (element.type === "RedirectCallback") {
                redirectCallback = _.fromPairs(_.map(element.output, (o) => {
                    return [o.name, o.value];
                }));

                redirectForm = $(`<form action='${redirectCallback.redirectUrl}' method='POST'></form>`);

                if (redirectCallback.redirectMethod === "POST") {
                    _.each(redirectCallback.redirectData, (v, k) => {
                        redirectForm.append(
                            `<input type='hidden' name='${k}' value='${v}' aria-hidden='true' />`);
                    });
                    redirectForm.appendTo("body").submit();
                } else {
                    window.location.replace(redirectCallback.redirectUrl);
                }
            } else if (element.type === "PollingWaitCallback") {
                const pollingWaitTimeoutMs = _.find(element.output, { name: "waitTime" }).value;

                _.delay(() => {
                    // we are already on the "wait" screen, set the boolean to indicate we don't need to rerender the page
                    // until the user authenticates/registers using push auth
                    this.pollingInProgress = true;

                    if (hasCallback(this.reqs.callbacks, "PollingWaitCallback")) {
                        processLoginRequest();
                    }
                }, pollingWaitTimeoutMs);
            }

            requirements.callbacks.push({
                input: {
                    index: requirements.callbacks.length,
                    name: element.input ? element.input[0].name : null,
                    value: element.input ? element.input[0].value : null
                },
                output: element.output,
                type: element.type
            });
        });

        if (!hasCallback(reqs.callbacks, "ConfirmationCallback") &&
            !hasCallback(reqs.callbacks, "PollingWaitCallback") &&
            !hasCallback(reqs.callbacks, "RedirectCallback")) {
            const confirmationCallback = {
                "input": {
                    index: requirements.callbacks.length,
                    name: "loginButton",
                    value: 0
                },
                output: [{
                    name: "options",
                    value: [i18next.t("common.user.login")]
                }],
                type: "ConfirmationCallback"
            };
            requirements.callbacks.push(confirmationCallback);
        }

        // This code is run each time a new callback is returned, so we need to check if the callbacks have changed
        const haveCallbacksChanged = (reqs.callbacks && !this.reqs) || !_.isEqual(reqs.callbacks, this.reqs.callbacks);
        const isPolling = this.pollingInProgress || hasCallback(reqs.callbacks, "PollingWaitCallback");

        this.reqs = reqs;
        this.data.reqs = requirements;

        // Is there an attempt at autologin happening?
        // if yes then don't render the form until it fails one time
        if (urlParams.IDToken1 && Configuration.globalData.auth.autoLoginAttempts === 1) {
            Configuration.globalData.auth.autoLoginAttempts++;
        } else if (haveCallbacksChanged || !isPolling) {
            // OPENAM-9480: set the flag to false to indicate that the user moved to another stage
            // (e.g. back to first stage in case of failed push auth)
            this.pollingInProgress = false;

            const callback = () => {
                populateTemplate.call(self);
                self.parentRender(() => {
                    self.prefillLoginData();
                    // Resolve a promise when all templates will be loaded
                    promise.resolve();
                });
            };

            this.loadThemedTemplate(`openam/authn/${reqs.stage}`).then((renderTemplate) => {
                self.template = renderTemplate;
                callback();
            }, () => {
                self.template = "openam/RESTLoginTemplate";
                callback();
            });
        }
        return promise;
    },
    prefillLoginData () {
        const login = CookieHelper.getCookie("login");

        if (this.$el.find("[name=loginRemember]").length !== 0 && login) {
            this.$el.find("input[type=text]:first").val(login);
            this.$el.find("[name=loginRemember]").attr("checked", "true");
            // setTimeout here is a workaround for iOS safari which tries to
            // block autofocussing on elements.
            setTimeout(() => {
                this.$el.find("[type=password]").focus();
            });
        } else {
            this.$el.find(":input:not([type='radio']):not([type='checkbox'])" +
                ":not([type='submit']):not([type='button']):first").focus();
        }
    },

    handleParams () {
        // TODO: Remove support for fragment params and change to URIUtils.getCurrentQueryString()
        // as currently we are checking both the framgent and query with framgent over-riding.
        const paramString = URIUtils.getCurrentCompositeQueryString();
        const params = parseParameters(paramString);
        // Rest does not accept the params listed in the array below as is
        // they must be transformed into the "authIndexType" and "authIndexValue" params
        // but if composite_advice set that must be adhered to
        if (!params.authIndexType || params.authIndexType !== "composite_advice") {
            const resourceUrlParam = _.find(params, (value, key) => {
                return key.toLowerCase() === "resourceurl";
            });
            _.each(["authlevel", "module", "service", "user", "resource"], (param) => {
                if (params[param]) {
                    const resourceDefinedWithoutUrl = param === "resource" && !resourceUrlParam;
                    if (resourceDefinedWithoutUrl) {
                        return;
                    }
                    const indexValue = param === "resource" ? resourceUrlParam : params[param];
                    params.authIndexType = param === "authlevel" ? "level" : param;
                    params.authIndexValue = indexValue;
                    //*** Note special case for authLevel
                    Configuration.globalData.auth.additional += `&authIndexType=${
                        (param === "authlevel" ? "level" : param)}&authIndexValue=${indexValue}`;
                }
            });
        }
        // Special case for SSORedirect and SSOPOST
        if (hasSsoRedirectOrPost(params.goto)) {
            params.goto = `${Constants.context}${params.goto}`;
            Configuration.globalData.auth.additional.replace("&goto=", `&goto=${Constants.context}`);
        }

        Configuration.globalData.auth.urlParams = params;
        return params;
    }
});

Handlebars.registerHelper("callbackRender", function () {
    const self = this;
    let result = "";
    let prompt = "";
    let options;
    let defaultOption;
    let btnClass = "";

    _.find(this.output, (obj) => {
        if (obj.name === "prompt" && obj.value !== undefined && obj.value.length) {
            prompt = obj.value.replace(/:$/, "");
        }
    });

    function generateId (name) {
        return _.isEmpty(name) ? "" : _.camelCase(name);
    }

    const renderContext = {
        id: generateId(this.input.name),
        index: this.input.index,
        value: this.input.value,
        prompt
    };

    function renderPartial (name, context) {
        const partial = _.find(Handlebars.partials, (code, templateName) => {
            return templateName.indexOf(`login/_${name}`) !== -1;
        });
        const data = _.merge(renderContext, context);

        return partial(data);
    }

    switch (this.type) {
        case "PasswordCallback": result += renderPartial("Password"); break;
        case "TextInputCallback": result += renderPartial("TextInput"); break;
        case "TextOutputCallback":
            options = {
                message: _.find(this.output, { name: "message" }),
                type: _.find(this.output, { name: "messageType" })
            };

            // Magic number 4 is for a <script>, taken from ScriptTextOutputCallback.java
            if (options.type.value === "4") {
                result += renderPartial("ScriptTextOutput", {
                    messageValue: options.message.value
                });
            } else {
                result += renderPartial("TextOutput", {
                    typeValue: options.type.value,
                    messageValue: options.message.value
                });
            }
            break;
        case "ConfirmationCallback":
            options = _.find(this.output, { name: "options" });

            if (options && options.value !== undefined) {
                // if there is only one option then mark it as default.
                defaultOption = options.value.length > 1
                    ? _.find(this.output, { name: "defaultOption" }) : { "value": 0 };

                _.each(options.value, (option, key) => {
                    btnClass = defaultOption && defaultOption.value === key ? "btn-primary" : "btn-default";
                    result += renderPartial("Confirmation", {
                        btnClass,
                        key,
                        option
                    });
                });
            }
            break;
        case "ChoiceCallback":
            options = _.find(this.output, { name: "choices" });

            if (options && options.value !== undefined) {
                result += renderPartial("Choice", {
                    values: _.map(options.value, (option, key) => {
                        return {
                            active: self.input.value === key,
                            key,
                            value: option
                        };
                    })
                });
            }
            break;
        case "HiddenValueCallback": result += renderPartial("HiddenValue"); break;
        case "RedirectCallback": result += renderPartial("Redirect"); break;
        case "PollingWaitCallback": result += renderPartial("PollingWait", {
            message: _.find(this.output, { name: "message" }).value
        }); break;
        default: result += renderPartial("Default"); break;
    }

    return new Handlebars.SafeString(result);
});

export default new LoginView();
