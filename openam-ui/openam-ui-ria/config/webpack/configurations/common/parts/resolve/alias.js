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

const { resolve } = require("path");

module.exports = {
    /**
     * External Dependencies
     * Packages aliased with "resolve(process.cwd(), ..." are done so to resolve duplicate packages
     * between dependencies. It ensures duplicate packages don't exist between bundles, resulting in
     * a smaller overall filename and ensuring that packages that are plugins (e.g. backgrid-selectall etc)
     * resolve and attach themselves to the same parent dependency.
     */
    "autosizeInput"     : "libs/jquery.autosize.input",
    "backbone"          : resolve(process.cwd(), "node_modules", "backbone"),
    "backgrid-selectall": "backgrid-select-all",
    "backgrid.paginator": "backgrid-paginator",
    "backgrid"          : resolve(process.cwd(), "node_modules", "backgrid"),
    "bootstrap-datetimepicker": "eonasdan-bootstrap-datetimepicker",
    "bootstrap-tabdrop" : "libs/bootstrap-tabdrop-1.0",
    "clockPicker"       : "clockpicker",
    "doTimeout"         : "libs/jquery.ba-dotimeout-1.0",
    "jquery"            : resolve(process.cwd(), "node_modules", "jquery"),
    "json-editor"       : "libs/jsoneditor-0.7.23-custom",
    "moment"            : resolve(process.cwd(), "node_modules", "moment"),
    "popoverclickaway"  : "libs/popover-clickaway",
    "prop-types"        : resolve(process.cwd(), "node_modules", "prop-types"),
    "sortable"          : "jquery-sortable",
    "URI"               : "urijs",

    // Internal Aliases
    "Footer"            : "org/forgerock/openam/ui/common/components/Footer",
    "ForgotUsernameView": "org/forgerock/openam/ui/user/anonymousProcess/ForgotUsernameView",
    "KBADelegate"       : "org/forgerock/openam/ui/user/services/KBADelegate",
    "LoginView"         : "org/forgerock/openam/ui/user/login/RESTLoginView",
    "NavigationFilter"  : "org/forgerock/openam/ui/common/components/navigation/filters/RouteNavGroupFilter",
    "PasswordResetView" : "org/forgerock/openam/ui/user/anonymousProcess/PasswordResetView",
    "RegisterView"      : "org/forgerock/openam/ui/user/anonymousProcess/SelfRegistrationView",
    "Router"            : "org/forgerock/commons/ui/common/main/Router",
    "ThemeManager"      : "org/forgerock/openam/ui/common/util/ThemeManager",
    "UserProfileView"   : "org/forgerock/commons/ui/user/profile/UserProfileView"
};
