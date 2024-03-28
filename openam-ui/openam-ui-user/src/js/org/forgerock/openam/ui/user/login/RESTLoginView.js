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
 * Copyright 2011-2023 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";
import Form2js from "form2js/src/form2js";
import Handlebars from "handlebars-template-loader/runtime";
import i18next from "i18next";
import { FRDevice } from "@forgerock/javascript-sdk";

import {
    isNotDefaultPath,
    remove as removeGotoUrl,
    setValidated as setValidatedGotoUrl,
    toHref as gotoUrlToHref
} from "org/forgerock/openam/ui/user/login/gotoUrl";
import { parseParameters, urlParamsFromObject } from "org/forgerock/openam/ui/common/util/uri/query";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AuthNService from "org/forgerock/openam/ui/user/services/AuthNService";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import CookieHelper from "org/forgerock/commons/ui/common/util/CookieHelper";
import getCurrentFragmentParamString from "org/forgerock/openam/ui/common/util/uri/getCurrentFragmentParamString";
import isRealmChanged from "org/forgerock/openam/ui/common/util/isRealmChanged";
import logout from "org/forgerock/openam/ui/user/login/logout";
import MaxIdleTimeLeftStrategy from "org/forgerock/openam/ui/common/sessions/strategies/MaxIdleTimeLeftStrategy";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import processLoginRequest from "config/process/processLoginRequest";
import RESTLoginHelper from "org/forgerock/openam/ui/user/login/RESTLoginHelper";
import Router from "org/forgerock/commons/ui/common/main/Router";
import ServiceInvoker from "org/forgerock/commons/ui/common/main/ServiceInvoker";
import SessionValidator from "org/forgerock/openam/ui/common/sessions/SessionValidator";
import URIUtils from "org/forgerock/commons/ui/common/util/URIUtils";
import defaultAppIcon from "../../../../../../../resources/images/default-app.svg";
import { sanitize } from "org/forgerock/openam/ui/common/util/Sanitizer";

function hasSsoRedirectOrPost (goto) {
    let decodedGoto;
    if (goto) {
        decodedGoto = decodeURIComponent(goto);
    }
    return goto && (_.startsWith(decodedGoto, "/SSORedirect") || _.startsWith(decodedGoto, "/SSOPOST"));
}

function populateTemplate () {
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
 * @param {Array.<object>} callbacks array of callback objects
 * @param {string} type The callback type that is being checked
 * @returns {boolean} if the callback "type" is present in the callbacks.
 */
function hasCallback (callbacks, type) {
    return _.some(callbacks, (callback) => callback.type === type);
}

const LoginView = AbstractView.extend({
    template: "openam/RESTLoginTemplate",
    baseTemplate: "common/LoginBaseTemplate",
    data: {},
    events: {
        "click input[type=submit]": "formSubmit",
        "click button.js-select-idp-button": "selectIdpClick"
    },

    handleExistingSession (requirements) {
        const element = this.$el;
        // If we have a token, let's see who we are logged in as....
        RESTLoginHelper.getLoggedUser((user, remainingSessionTime) => {
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

                ServiceInvoker.removeAnonymousDefaultHeaders();
                if (Configuration.globalData.xuiUserSessionValidationEnabled) {
                    SessionValidator.start(MaxIdleTimeLeftStrategy, remainingSessionTime);
                }
                Router.navigate("", { trigger: true });
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

    selectIdpClick (event) {
        const submitContent = { [event.target.name]: event.target.value };

        $("button.js-select-idp-button").prop("disabled", true);
        this.loginRequestFunction({
            submitContent,
            failureCallback: () => {
                // enabled the login button if login failure
                $("button.js-select-idp-button").prop("disabled", false);
                this.renderLoginFailure();
            }
        });
    },

    formSubmit (e) {
        let expire;

        e.preventDefault();
        // disabled button before login
        $(e.currentTarget).prop("disabled", true);

        const submitContent = new Form2js(this.$el[0]);
        submitContent[$(e.target).attr("name")] = $(e.target).attr("index");

        // Set consent boolean for all consent callbacks
        const consentCollectors = this.$el.find("input[data-consentCollector]");
        if (consentCollectors.length) {
            const consentCollector = consentCollectors[consentCollectors.length - 1];
            consentCollectors.each((i, element) => {
                element.checked = consentCollector.checked;
            });
        }

        // Make values of checkboxes true/false
        this.$el.find("input[type=checkbox]").each((i, element) => {
            submitContent[element.name] = element.checked;
        });

        // Make values of number inputs the correct type
        this.$el.find("input[type=number]").each((i, element) => {
            submitContent[element.name] = parseInt(element.value, 10);
        });

        // START CUSTOM STAGE-SPECIFIC LOGIC HERE

        // Known to be used by username/password based authn stages
        const secure = (location.protocol === "https:");
        if (this.$el.find("[name=loginRemember]:checked").length !== 0) {
            expire = new Date();
            expire.setDate(expire.getDate() + 20);
            // An assumption that the login name is the first text input box
            CookieHelper.setCookie("login", this.$el.find("input[type=text]:first").val(), expire, undefined,
                undefined, secure, "None");
        } else if (this.$el.find("[name=loginRemember]").length !== 0) {
            CookieHelper.deleteCookie("login", undefined, undefined, secure, "None");
        }

        // Check for KbaCreateCallback, merge question and answer into array
        // instead of two separate properties, where the first item is the
        // question and second is the answer
        _.each(submitContent, (val, key) => {
            if (key.includes("_kba_")) {
                const split = key.split("_kba_");
                const newKey = split[0];
                const index = parseInt(split[1], 10);
                if (submitContent[newKey]) {
                    submitContent[newKey][index] = val;
                } else {
                    submitContent[newKey] = [val];
                }
                delete submitContent[key];
            }
        });

        // END CUSTOM STAGE-SPECIFIC LOGIC HERE
        this.loginRequestFunction({
            submitContent,
            failureCallback: () => {
                // enabled the login button if login failure
                $(e.currentTarget).prop("disabled", false);
                $("input[type='password']").val("");
                this.renderLoginFailure();
            }
        });
    },

    renderLoginFailure () {
        // If its not the first stage or if we are not using the default login service
        // then render the Login Unavailable view with link back to login screen
        if (Configuration.globalData.auth.currentStage > 1 || !Configuration.globalData.auth.isDefaultService) {
            let fragmentParams = URIUtils.getCurrentFragmentQueryString();
            if (fragmentParams) {
                fragmentParams = `&${fragmentParams}`;
            }
            // Go to the Login Unavailable view with all the original fragment parameters.
            routeToLoginUnavailable(fragmentParams);
        }
    },

    /**
    * Specifying realm as part of the fragment is not supported since 14.0.
    * This function removes the realm parameter from the fragment and puts it into the query string.
    * TODO: Should be removed once AME-11109 is resolved.
    */
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

        AuthNService.getRequirements().then((reqs) => {
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

            // At this point the AM backend has processed the suspendedId param
            // which means we can now remove it from the url so it is not processed
            // again on subsequent steps
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get("suspendedId")) {
                // Remove the parameter
                urlParams.delete("suspendedId");
                // Replace the current url with url no longer containing the suspendedId
                window.history.replaceState({}, "", `${location.pathname}?${urlParams}`);
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
        }, (error) => {
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
        });
    },
    isUsernamePasswordStage (reqs) {
        const usernamePasswordStages = ["DataStore1", "AD1", "JDBC1", "LDAP1", "Membership1", "RADIUS1"];
        if (_.includes(usernamePasswordStages, reqs.stage)) {
            return true;
        }
        return (reqs.callbacks && hasCallback(reqs.callbacks, "NameCallback"));
    },
    lastIndexOfCallbacksOfType (callbacks, type) {
        return _.findLastIndex(callbacks, (callback) => callback.type === type);
    },
    renderForm (reqs, urlParams) {
        const requirements = _.clone(reqs);
        const promise = $.Deferred();
        const self = this;

        this.userNamePasswordStage = this.isUsernamePasswordStage(reqs);
        const consentMappingOutputs = [];

        requirements.callbacks = [];

        const onlySelectIdpCallbacks = reqs.callbacks.every((callback) => callback.type === "SelectIdPCallback");
        const onlyDeviceProfileCallback = reqs.callbacks.every((callback) => callback.type === "DeviceProfileCallback");

        _.each(reqs.callbacks, (element, index) => {
            if (element.type === "RedirectCallback") {
                const redirectCallback = _.fromPairs(_.map(element.output, (o) => {
                    return [o.name, o.value];
                }));

                const redirectForm = $(`<form action='${redirectCallback.redirectUrl}' method='POST'></form>`);

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
            } else if (element.type === "DeviceProfileCallback") {
                element.output.push({ name: "onlyCallback", value: onlyDeviceProfileCallback });
                if (onlyDeviceProfileCallback) {
                    const device = new FRDevice();
                    device.getProfile({
                        location: _.find(element.output, { name: "location" }).value,
                        metadata: _.find(element.output, { name: "metadata" }).value
                    }).then((profile) => {
                        element.input[0].value = JSON.stringify(profile);
                        processLoginRequest();
                    }).catch(() => {
                        element.input[0].value = "";
                        processLoginRequest();
                    });
                }
            }

            // KbaCreateCallback needs two input fields, one for question, one
            // for the answer
            if (element.type === "KbaCreateCallback") {
                requirements.callbacks.push({
                    input: [
                        {
                            index: requirements.callbacks.length,
                            name: element.input ? element.input[0].name : null,
                            value: element.input ? element.input[0].value : null
                        },
                        {
                            index: requirements.callbacks.length,
                            name: element.input ? element.input[1].name : null,
                            value: element.input ? element.input[1].value : null
                        }
                    ],
                    output: element.output,
                    type: element.type
                });
            } else {
                requirements.callbacks.push({
                    input: {
                        index: requirements.callbacks.length,
                        name: element.input ? element.input[0].name : null,
                        value: element.input ? element.input[0].value : null
                    },
                    output: element.output,
                    type: element.type
                });
            }

            if (element.type === "ConsentMappingCallback") {
                requirements.callbacks[index].output.allConsentMappingOutputs = consentMappingOutputs;
                requirements.callbacks[index].output.isLast =
                    this.lastIndexOfCallbacksOfType(reqs.callbacks, "ConsentMappingCallback") === index;
                requirements.callbacks[index].output.index = index;
                consentMappingOutputs.push(requirements.callbacks[index].output);
            } else if (element.type === "SelectIdPCallback") {
                requirements.callbacks[index].onlyCallback = onlySelectIdpCallbacks;
            }
        });

        if (!hasCallback(reqs.callbacks, "ConfirmationCallback") &&
            !hasCallback(reqs.callbacks, "PollingWaitCallback") &&
            !hasCallback(reqs.callbacks, "RedirectCallback") &&
            !hasCallback(reqs.callbacks, "SuspendedTextOutputCallback") &&
            !onlyDeviceProfileCallback &&
            !onlySelectIdpCallbacks) {
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

        // Sanitize the content of the header and description if they exist to
        // remove potentially malicious tags and attributes
        if (this.data.reqs.header) {
            this.data.reqs.header = sanitize(this.data.reqs.header);
        }

        if (this.data.reqs.description) {
            this.data.reqs.description = sanitize(this.data.reqs.description);
        }

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

function getFailedPolicies (callback) {
    const output = callback.output.find((output) => output.name === "failedPolicies");
    if (!output) {
        return [];
    }

    return output.value.map(JSON.parse);
}

Handlebars.registerHelper("isPassword", (value) => {
    return value === "password";
});

Handlebars.registerHelper("callbackRender", function () {
    const self = this;
    let result = "";
    let prompt = "";
    let options;
    let defaultOption;
    let btnClass = "";
    const failedPolicies = getFailedPolicies(this);
    const errorMessages = failedPolicies.map((failedPolicy) => i18next.t(
        `common.policyValidationMessages.${failedPolicy.policyRequirement}`,
        { ...failedPolicy.params, interpolation: { escapeValue: false } }
    ));

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
        case "PasswordCallback":
        case "ValidatedCreatePasswordCallback":
            result += renderPartial("Default", { type: "password", errorMessages }); break;
        case "TextInputCallback": result += renderPartial("TextInput"); break;
        case "SuspendedTextOutputCallback":
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
        case "ReCaptchaCallback": result += renderPartial("ReCaptcha", {
            siteKey: _.find(this.output, { name: "recaptchaSiteKey" }).value,
            apiUri: _.find(this.output, { name: "captchaApiUri" }).value,
            divClass: _.find(this.output, { name: "captchaDivClass" }).value,
            reCaptchaV3: _.find(this.output, { name: "reCaptchaV3" }).value,
            disableSubmission: _.find(this.output, { name: "disableSubmission" }).value
        }); break;
        case "BooleanAttributeInputCallback": result += renderPartial("Checkbox", { errorMessages }); break;
        case "NumberAttributeInputCallback":
            result += renderPartial("Default", { errorMessages, type: "number" }); break;
        case "KbaCreateCallback":
            options = _.find(this.output, { name: "predefinedQuestions" });
            if (options && options.value !== undefined) {
                result += renderPartial("Kba", {
                    values: _.map(options.value, (option, key) => {
                        return {
                            active: self.input.value === key,
                            key,
                            value: option
                        };
                    }),
                    questionId: generateId(this.input[0].name),
                    question: this.input[0],
                    answerId: generateId(this.input[1].name),
                    answer: this.input[1]
                });
            }
            break;
        case "ConsentMappingCallback":
            result += this.output.isLast ? renderPartial("ConsentMapping", {
                allConsentMappingOutputs: _.map(this.output.allConsentMappingOutputs, (callbackOutput) => ({
                    displayName: _.find(callbackOutput, { name: "displayName" }).value,
                    icon: _.find(callbackOutput, { name: "icon" }).value || defaultAppIcon,
                    consentMessage: _.find(callbackOutput, { name: "message" }).value,
                    isLast: callbackOutput.isLast,
                    id: callbackOutput.index
                }))
            }) : ""; break;
        case "TermsAndConditionsCallback":
            result += renderPartial("TermsAndConditions", {
                termsAndConditionsText: _.find(this.output, { name: "terms" }).value
            });
            break;
        case "SelectIdPCallback":
            result += renderPartial("SelectIdp", {
                providers: _.find(this.output, { name: "providers" }).value,
                onlyCallback: this.onlyCallback
            });
            break;
        case "DeviceProfileCallback": {
            const id = `callback_${this.input.index}`;
            const onlyCallback = _.find(this.output, { name: "onlyCallback" }).value;
            const message = _.find(this.output, { name: "message" }).value;
            result += renderPartial("DeviceProfile", {
                message,
                onlyCallback,
                id
            });
            if (!onlyCallback) {
                const device = new FRDevice();
                device.getProfile({
                    location: _.find(this.output, { name: "location" }).value,
                    metadata: _.find(this.output, { name: "metadata" }).value
                }).then((profile) => {
                    document.querySelector(`#${id}_input`).value = JSON.stringify(profile);
                    if (message) {
                        document.querySelector(`#${id}_spinner`).classList.add("display-none");
                        document.querySelector(`#${id}_check`).classList.remove("display-none");
                    }
                }).catch(() => {
                    document.querySelector(`#${id}_input`).value = "";
                    if (message) {
                        document.querySelector(`#${id}_spinner`).classList.add("display-none");
                        document.querySelector(`#${id}_check`).classList.remove("display-none");
                    }
                });
            }
            break;
        }
        default: result += renderPartial("Default", { errorMessages, type: "text" }); break;
    }

    return new Handlebars.SafeString(result);
});

export default new LoginView();
