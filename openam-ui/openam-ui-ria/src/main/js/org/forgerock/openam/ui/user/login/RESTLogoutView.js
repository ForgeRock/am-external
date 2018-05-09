/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper",
    "org/forgerock/openam/ui/user/login/removeOAuth2Goto",
    "org/forgerock/openam/ui/user/login/navigateThenRefresh",
    "templates/common/LoginBaseTemplate"
], ($, AbstractView, Constants, EventManager, RESTLoginHelper, removeOAuth2Goto,
    navigateThenRefresh, LoginBaseTemplate) => {
    removeOAuth2Goto = removeOAuth2Goto.default;

    var LogoutView = AbstractView.extend({
        template: "openam/ReturnToLoginTemplate",
        baseTemplate: LoginBaseTemplate,
        data: {},
        events: {
            "click [data-return-to-login-page]" : navigateThenRefresh
        },
        render () {
            const successfulLoginUrlParams = removeOAuth2Goto(RESTLoginHelper.getSuccessfulLoginUrlParams());
            RESTLoginHelper.removeSuccessfulLoginUrlParams();

            /*
            The RESTLoginHelper.filterUrlParams returns a filtered list of the parameters from the value set within the
            Configuration.globalData.auth.fullLoginURL which is populated by the server upon successful login.
            Once the session has ended we need to manually remove the fullLoginURL as it is no longer valid and can
            cause problems to subsequent failed login requests - i.e ones which do not override the current value.
            FIXME: Remove all session specific properties from the globalData object.
            */
            this.data.params = RESTLoginHelper.filterUrlParams(successfulLoginUrlParams);

            EventManager.sendEvent(Constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: true });

            this.data.title = $.t("templates.user.RestLogoutTemplate.loggedOut");
            this.parentRender();
        }
    });

    return new LogoutView();
});
