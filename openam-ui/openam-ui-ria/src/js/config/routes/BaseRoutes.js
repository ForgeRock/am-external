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
 * Copyright 2018 ForgeRock AS.
 */

import Constants from "org/forgerock/openam/ui/common/util/Constants";

// These routes will only be used if no other matching pattern or url is found.
export default {
    "unauthorized": {
        view: () => import("org/forgerock/commons/ui/common/UnauthorizedView.js"),
        url: ""
    },
    "forbidden": {
        view: () => import("org/forgerock/openam/ui/common/views/error/ForbiddenView.js"),
        url: /.*/
    },
    "404": {
        view: () => import("org/forgerock/commons/ui/common/NotFoundView.js"),
        url: /^([\w\W]*)$/,
        pattern: "?"
    },
    "default": {
        event: Constants.EVENT_HANDLE_DEFAULT_ROUTE,
        url: /^$/,
        pattern: ""
    },
    // from https://developers.facebook.com/blog/post/552/
    // We started adding a fragment #_=_ to the redirect_uri when this field is left blank.
    // Please ensure that your app can handle this behavior.
    "facebook_redirect": {
        event: Constants.EVENT_HANDLE_DEFAULT_ROUTE,
        url: /^_=_$/,
        pattern: ""
    }
};
