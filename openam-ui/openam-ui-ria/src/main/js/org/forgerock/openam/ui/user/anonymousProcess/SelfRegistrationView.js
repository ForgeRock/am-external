/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/openam/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/user/anonymousProcess/SelfRegistrationView",
    "org/forgerock/commons/ui/user/anonymousProcess/KBAView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/user/login/RESTLoginView"
], (_, Constants, AnonymousProcessView, SelfRegistrationView, KBAView, Configuration, RESTLoginView) => {
    function shouldRouteToLoginView (response, destination) {
        return response.tag === "end" && destination === "login";
    }

    function shouldAutoLogin (response, destination) {
        return response.tag === "end" && destination === "auto-login";
    }

    function AMSelfRegistrationView () { }

    AMSelfRegistrationView.prototype = SelfRegistrationView;
    AMSelfRegistrationView.prototype.endpoint = Constants.SELF_SERVICE_REGISTER;

    _.extend(AMSelfRegistrationView.prototype, AnonymousProcessView.prototype);

    AMSelfRegistrationView.prototype.renderProcessState = function (response) {
        const destination = _.get(Configuration, "globalData.successfulUserRegistrationDestination");
        const realm = _.get(Configuration, "globalData.realm", "");

        if (shouldAutoLogin(response, destination)) {
            RESTLoginView.handleExistingSession(response.additions);
        } else if (shouldRouteToLoginView(response, destination)) {
            window.location.href = `#login${realm}`;
        } else {
            AnonymousProcessView.prototype.renderProcessState.call(this, response).then(() => {
                if (response.type === "kbaSecurityAnswerDefinitionStage" && response.tag === "initial") {
                    KBAView.render(response.requirements.properties.kba);
                }
            });
        }
    };

    return new AMSelfRegistrationView();
});
