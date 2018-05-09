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
    "org/forgerock/commons/ui/user/anonymousProcess/PasswordResetView"
], (_, Constants, AnonymousProcessView, PasswordResetView) => {
    function AMPasswordResetView () { }

    AMPasswordResetView.prototype = PasswordResetView;
    AMPasswordResetView.prototype.endpoint = Constants.SELF_SERVICE_RESET_PASSWORD;

    _.extend(AMPasswordResetView.prototype, AnonymousProcessView.prototype);

    return new AMPasswordResetView();
});
