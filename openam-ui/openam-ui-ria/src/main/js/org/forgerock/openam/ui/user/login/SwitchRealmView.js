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
 * Copyright 2011-2017 ForgeRock AS.
 */

define([
    "i18next",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/isRealmChanged",
    "org/forgerock/openam/ui/common/util/uri/getCurrentFragmentParamString",
    "org/forgerock/openam/ui/user/login/logout",
    "org/forgerock/openam/ui/user/services/SessionService"
], (i18next, AbstractView, EventManager, Configuration, Router, Constants, isRealmChanged,
    getCurrentFragmentParamString, logout, SessionService) => {
    isRealmChanged = isRealmChanged.default;
    logout = logout.default;
    getCurrentFragmentParamString = getCurrentFragmentParamString.default;

    function gotoLoginWithParams (args) {
        EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {
            args,
            route: Router.configuration.routes.login
        });
    }

    function removeUserAndGotoLogin (args) {
        Configuration.setProperty("loggedUser", null);
        gotoLoginWithParams(args);
    }

    const SwitchRealmView = AbstractView.extend({
        template: "templates/openam/SwitchRealmsTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",
        data: {},
        events: {
            "click [data-switch-realms]" : "onSwitchRealmsHandler"
        },
        partials: [
            "partials/alerts/_Alert.html"
        ],
        render () {
            this.data.args = [getCurrentFragmentParamString()];

            if (isRealmChanged()) {
                SessionService.isSessionValid().then(
                    () => this.parentRender(),
                    () => removeUserAndGotoLogin(this.data.args)
                );
            } else {
                removeUserAndGotoLogin(this.data.args); // Realm not changed, but params may have
            }
        },
        onSwitchRealmsHandler (event) {
            event.preventDefault();
            logout().then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedOut");
                gotoLoginWithParams(this.data.args);
            });
        }
    });

    return new SwitchRealmView();
});
