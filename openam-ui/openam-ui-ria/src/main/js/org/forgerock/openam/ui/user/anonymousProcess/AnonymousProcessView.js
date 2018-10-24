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
    "lodash",
    "i18next",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/URIUtils",
    "org/forgerock/commons/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/user/delegates/AnonymousProcessDelegate",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/uri/getCurrentFragmentParamString",
    "org/forgerock/openam/ui/common/util/uri/query",
    "store/index"
], (_, i18next, Messages, EventManager, Router, URIUtils, AnonymousProcessView, AnonymousProcessDelegate, fetchUrl,
    Constants, getCurrentFragmentParamString, query, store) => { // eslint-disable-line padded-blocks

    function getNextRoute (endpoint) {
        if (endpoint === Constants.SELF_SERVICE_REGISTER) {
            return Router.configuration.routes.continueSelfRegister;
        } else if (endpoint === Constants.SELF_SERVICE_RESET_PASSWORD) {
            return Router.configuration.routes.continuePasswordReset;
        }
        return "";
    }

    function isFromEmailLink (params) {
        return params.token;
    }

    function isSelfRegistrationFlow (endpoint) {
        return endpoint.indexOf(Constants.SELF_SERVICE_REGISTER) !== -1;
    }

    function isPasswordResetFlow (endpoint) {
        return endpoint.indexOf(Constants.SELF_SERVICE_RESET_PASSWORD) !== -1;
    }

    function getRegistrationError (code, reason) {
        let error;
        // Provide a self registration specific error message related to the following scenarios.
        // If no match, no message returned - ErrorHandler will display generic message instead based on code
        if (code === 409 || _.startsWith(reason, "ldap exception") || _.startsWith(reason, "Resource already exists")) {
            error = i18next.t("config.messages.UserMessages.registerDataInvalid");
        } else if (code === 400 && _.startsWith(reason, "CONSTRAINT_VIOLATION")) {
            error = i18next.t("config.messages.UserMessages.registerDataInvalid");
        } else if (_.startsWith(reason, "Identity names may not have a space character")) {
            error = i18next.t("config.messages.UserMessages.registerDataInvalid");
        }
        return error;
    }

    return AnonymousProcessView.extend({

        render () {
            const fragmentParams = query.parseParameters(URIUtils.getCurrentFragmentQueryString());
            const nextRoute = getNextRoute(this.endpoint);
            const endpoint = fetchUrl.default(`/${this.endpoint}`, {
                realm: store.default.getState().remote.info.realm
            });
            const setTemplateData = () => {
                this.data.fragmentParamString = getCurrentFragmentParamString.default();
                // TODO: The first undefined argument is the deprecated realm which is defined in the
                // CommonRoutesConfig login route. This needs to be removed as part of AME-11109.
                this.data.args = [undefined, this.data.fragmentParamString];
                this.setTranslationBase();
            };

            if (!this.delegate || Router.currentRoute !== nextRoute) {
                this.setDelegate(`json${endpoint}`, fragmentParams.token);
            }
            if (isFromEmailLink(fragmentParams)) {
                this.submitDelegate(fragmentParams, (response) => {
                    const status = _.get(response, "status");
                    const isFailure = status && status.success === false;

                    if (isPasswordResetFlow(endpoint) && isFailure) {
                        delete fragmentParams["token"];
                        delete fragmentParams["code"];
                        this.data.fragmentParamString =
                            _.isEmpty(fragmentParams) ? "" : `&${query.urlParamsFromObject(fragmentParams)}`;
                        this.data.args = [undefined, this.data.fragmentParamString];
                        this.setTranslationBase();
                        this.renderResponse(response);
                    } else if (isSelfRegistrationFlow(endpoint) && isFailure) {
                        const errorMessage = getRegistrationError(status.code, status.reason);
                        // If no specific user registration msg, generic message (based on response code) will be used
                        if (errorMessage) {
                            Messages.addMessage({ message: errorMessage, type: Messages.TYPE_DANGER });
                        }
                        Router.routeTo(Router.currentRoute, { trigger: true });
                    } else {
                        Router.routeTo(nextRoute, { trigger: true });
                    }
                });
            } else {
                setTemplateData();
                this.parentRender();
            }
        },

        restartProcess (e) {
            e.preventDefault();
            delete this.delegate;
            delete this.stateData;

            EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
                args: this.data.args,
                route: _.extend({}, Router.currentRoute, { forceUpdate: true })
            });
        }
    });
});
