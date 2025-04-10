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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import BootstrapCustomCSS from "css/bootstrap-3.4.1-custom";
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

    // Theme
    DEFAULT_STYLESHEETS: [BootstrapCustomCSS, StyleAdminCSS],
    EVENT_THEME_CHANGED: "main.EVENT_THEME_CHANGED",

    /**
     * Commons Constants
     */
    host: "",

    EVENT_SERVICE_UNAVAILABLE: "error.SERICE_UNAVAILABLE",

    //views
    EVENT_CHANGE_VIEW: "view.EVENT_CHANGE_VIEW",
    EVENT_UNAUTHORIZED: "view.EVENT_UNAUTHORIZED",
    EVENT_UNAUTHENTICATED: "view.EVENT_UNAUTHENTICATED",

    //login
    EVENT_HANDLE_DEFAULT_ROUTE: "common.navigation.EVENT_HANDLE_DEFAULT_ROUTE",

    EVENT_APP_INITIALIZED: "main.EVENT_APP_INITIALIZED",
    EVENT_READ_CONFIGURATION_REQUEST: "main.EVENT_READ_CONFIGURATION_REQUEST",

    EVENT_CHANGE_BASE_VIEW: "view.EVENT_CHANGE_BASE_VIEW",

    //notifications
    EVENT_DISPLAY_MESSAGE_REQUEST: "messages.EVENT_DISPLAY_MESSAGE_REQUEST",

    DEFAULT_LANGUAGE: "en"
};
