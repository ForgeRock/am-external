/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
    "org/forgerock/openam/ui/user/services/SessionService",
    "templates/common/LoginBaseTemplate",
    "partials/alerts/_Alert"
], (i18next, AbstractView, EventManager, Configuration, Router, Constants, isRealmChanged,
    getCurrentFragmentParamString, logout, SessionService, LoginBaseTemplate, AlertPartial) => {
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
        template: "openam/SwitchRealmsTemplate",
        baseTemplate: LoginBaseTemplate,
        data: {},
        events: {
            "click [data-switch-realms]" : "onSwitchRealmsHandler"
        },
        partials: {
            "alerts/_Alert": AlertPartial
        },
        render () {
            this.data.fragmentParamString = getCurrentFragmentParamString();
            this.data.args = [this.data.fragmentParamString];

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
