/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "underscore",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/ModuleLoader",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper"
], function(_, cookieHelper, ModuleLoader, RESTLoginHelper) {
    var obj = {};

    obj.login = function(params, successCallback, errorCallback) {
        // resets the session cookie to discard old session that may still exist
        cookieHelper.deleteCookie("session-jwt", "/", "");
        return ModuleLoader.promiseWrapper(_.bind(_.curry(RESTLoginHelper.login)(params), RESTLoginHelper), {
            success: successCallback,
            error: errorCallback
        });
    };

    obj.logout = function(successCallback, errorCallback) {
        return ModuleLoader.promiseWrapper(_.bind(RESTLoginHelper.logout, RESTLoginHelper), {
            success: successCallback,
            error: errorCallback
        });
    };

    obj.getLoggedUser = function(successCallback, errorCallback) {
        return ModuleLoader.promiseWrapper(_.bind(RESTLoginHelper.getLoggedUser, RESTLoginHelper), {
            success: successCallback,
            error: errorCallback
        });
    };

    return obj;
});
