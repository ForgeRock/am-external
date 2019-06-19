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
 * Copyright 2011-2019 ForgeRock AS.
 */

import BootstrapCustomCSS from "css/bootstrap-3.3.7-custom";
import StyleAdminCSS from "css/styles-admin";

const getContextPath = () => {
    let path = location.pathname.replace(new RegExp("^/|/$", "g"), "").split("/");
    path.splice(-1);
    path = path.join("/");

    // If we are using OpenAM at the root context then we won't need to prepend the context path with a /
    return path === "" ? path : `/${path}`;
};

const context = getContextPath();

export default {
    context,
    CONSOLE_PATH: `${context}/console`,
    OPENAM_HEADER_PARAM_CUR_PASSWORD: "currentpassword",

    // Theme
    DEFAULT_STYLESHEETS: [BootstrapCustomCSS, StyleAdminCSS],
    EVENT_THEME_CHANGED: "main.EVENT_THEME_CHANGED",

    SELF_SERVICE_FORGOTTEN_USERNAME: "selfservice/forgottenUsername",
    SELF_SERVICE_RESET_PASSWORD: "selfservice/forgottenPassword",
    SELF_SERVICE_REGISTER: "selfservice/userRegistration",

    /**
     * Commons Constants
     */
    host: "",

    EVENT_SERVICE_UNAVAILABLE: "error.SERICE_UNAVAILABLE",

    EVENT_PROFILE_INITIALIZATION: "user.profile.EVENT_PROFILE_INITIALIZATION",

    //service invoker
    EVENT_END_REST_CALL: "common.delegate.EVENT_END_REST_CALL",
    EVENT_START_REST_CALL: "common.delegate.EVENT_START_REST_CALL",
    EVENT_REST_CALL_ERROR: "common.delegate.EVENT_REST_CALL_ERROR",

    //views
    EVENT_CHANGE_VIEW: "view.EVENT_CHANGE_VIEW",
    EVENT_UNAUTHORIZED: "view.EVENT_UNAUTHORIZED",
    EVENT_UNAUTHENTICATED: "view.EVENT_UNAUTHENTICATED",
    EVENT_SHOW_LOGIN_DIALOG: "dialog.EVENT_SHOW_LOGIN_DIALOG",

    //login
    EVENT_SUCCESFULY_LOGGGED_IN: "user.login.EVENT_SUCCESFULY_LOGGGED_IN",
    EVENT_LOGIN_FAILED: "user.login.EVENT_LOGIN_FAILED",
    EVENT_SELF_REGISTRATION_REQUEST: "user.login.EVENT_SELF_REGISTRATION_REQUEST",
    EVENT_AUTHENTICATED: "user.login.EVENT_AUTHENTICATED",
    EVENT_ADMIN_LOGGED_IN: "user.login.EVENT_ADMIN_LOGGED_IN",

    //profile
    EVENT_SHOW_PROFILE_REQUEST: "user.profile.EVENT_SHOW_PROFILE_REQUEST",
    EVENT_USER_PROFILE_UPDATE_FAILED: "user.profile.EVENT_USER_PROFILE_UPDATE_FAILED",
    EVENT_USER_PROFILE_UPDATED_SUCCESSFULY: "user.profile.EVENT_USER_PROFILE_UPDATED_SUCCESSFULY",
    EVENT_USERNAME_UPDATED_SUCCESSFULY: "user.profile.EVENT_USERNAME_UPDATED_SUCCESSFULY",
    EVENT_PROFILE_DELETE_USER_REQUEST: "user.profile.EVENT_PROFILE_DELETE_USER_REQUEST",
    EVENT_GO_BACK_REQUEST: "user.profile.EVENT_GO_BACK_REQUEST",
    EVENT_SECURITY_DATA_CHANGE_REQUEST: "user.profile.EVENT_SECURITY_DATA_CHANGE_REQUEST",
    EVENT_SITE_IDENTIFICATION_CHANGE_REQUEST: "user.profile.EVENT_SITE_IDENTIFICATION_CHANGE_REQUEST",
    EVENT_ENTER_OLD_PASSWORD_REQUEST: "user.profile.EVENT_ENTER_OLD_PASSWORD_REQUEST",

    //admin
    EVENT_ADMIN_USERS: "admin.usermanagement.EVENT_ADMIN_USERS",
    EVENT_ADMIN_ADD_USER_REQUEST: "admin.usermanagement.EVENT_ADMIN_ADD_USER_REQUEST",
    EVENT_USER_LIST_DELETE_USER_REQUEST: "admin.usermanagement.EVENT_USER_LIST_DELETE_USER_REQUEST",
    EVENT_ADMIN_SHOW_PROFILE_REQUEST: "admin.usermanagement.EVENT_ADMIN_SHOW_PROFILE_REQUEST",
    EVENT_ADMIN_CHANGE_USER_PASSWORD: "admin.usermanagement.EVENT_ADMIN_CHANGE_USER_PASSWORD",

    EVENT_NAVIGATION_HOME_REQUEST: "common.navigation.EVENT_NAVIGATION_HOME_REQUEST",
    EVENT_SWITCH_VIEW_REQUEST: "common.navigation.EVENT_SWITCH_VIEW_REQUEST",
    EVENT_HANDLE_DEFAULT_ROUTE: "common.navigation.EVENT_HANDLE_DEFAULT_ROUTE",

    //serviceinvoker
    EVENT_AUTHENTICATION_DATA_CHANGED: "common.delegate.EVENT_AUTHENTICATION_DATA_CHANGED",

    EVENT_APP_INITIALIZED: "main.EVENT_APP_INITIALIZED",
    EVENT_READ_CONFIGURATION_REQUEST: "main.EVENT_READ_CONFIGURATION_REQUEST",

    ROUTE_REQUEST: "view.ROUTE_REQUEST",
    EVENT_CHANGE_BASE_VIEW: "view.EVENT_CHANGE_BASE_VIEW",

    //notifications
    EVENT_NOTIFICATION_DELETE_FAILED: "notification.EVENT_NOTIFICATION_DELETE_FAILED",
    EVENT_GET_NOTIFICATION_FOR_USER_ERROR: "notification.EVENT_GET_NOTIFICATION_FOR_USER_ERROR",

    EVENT_DISPLAY_MESSAGE_REQUEST: "messages.EVENT_DISPLAY_MESSAGE_REQUEST",

    EVENT_USER_APPLICATION_DEFAULT_LNK_CHANGED: "messages.EVENT_USER_APPLICATION_DEFAULT_LNK_CHANGED",
    EVENT_REQUEST_RESEND_REQUIRED: "messages.EVENT_REQUEST_RESEND_REQUIRED",

    //user application link states
    USER_APPLICATION_STATE_APPROVED: "B65FA6A2-D43D-49CB-BEA0-CE98E275A8CD",
    USER_APPLICATION_STATE_PENDING: "B65FA6A2-D43D-49CD-BEA0-CE98E275A8CD",

    DEFAULT_LANGUAGE: "en",

    ANONYMOUS_USERNAME: "anonymous",
    ANONYMOUS_PASSWORD: "anonymous",

    HEADER_PARAM_PASSWORD: "X-Password",
    HEADER_PARAM_USERNAME: "X-Username",
    HEADER_PARAM_NO_SESSION: "X-NoSession",

    SELF_SERVICE_CONTEXT: "selfservice/",

    EVENT_SELECT_KBA_QUESTION: "user.selfservice.kba.EVENT_SELECT_KBA_QUESTION",
    EVENT_DELETE_KBA_QUESTION: "user.selfservice.kba.EVENT_DELETE_KBA_QUESTION"
};
