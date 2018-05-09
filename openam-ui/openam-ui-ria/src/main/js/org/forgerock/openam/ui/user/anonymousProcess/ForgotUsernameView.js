/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/openam/ui/user/anonymousProcess/AnonymousProcessView",
    "org/forgerock/commons/ui/user/anonymousProcess/ForgotUsernameView"
], (_, Constants, AnonymousProcessView, ForgotUsernameView) => {
    function AMForgotUsernameView () { }

    AMForgotUsernameView.prototype = ForgotUsernameView;
    AMForgotUsernameView.prototype.endpoint = Constants.SELF_SERVICE_FORGOTTEN_USERNAME;

    _.extend(AMForgotUsernameView.prototype, AnonymousProcessView.prototype);

    return new AMForgotUsernameView();
});
