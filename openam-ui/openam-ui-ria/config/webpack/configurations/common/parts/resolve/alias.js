/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
    // TODO: Remove this when there are no longer any references to the "underscore" dependency
    "underscore"        : "lodash",
    "URI"               : "urijs",

    // Internal Aliases
    "Footer"            : "org/forgerock/openam/ui/common/components/Footer",
    "ForgotUsernameView": "org/forgerock/openam/ui/user/anonymousProcess/ForgotUsernameView",
    "KBADelegate"       : "org/forgerock/openam/ui/user/services/KBADelegate",
    "LoginDialog"       : "org/forgerock/openam/ui/user/login/RESTLoginDialog",
    "LoginView"         : "org/forgerock/openam/ui/user/login/RESTLoginView",
    "NavigationFilter"  : "org/forgerock/openam/ui/common/components/navigation/filters/RouteNavGroupFilter",
    "PasswordResetView" : "org/forgerock/openam/ui/user/anonymousProcess/PasswordResetView",
    "RegisterView"      : "org/forgerock/openam/ui/user/anonymousProcess/SelfRegistrationView",
    "Router"            : "org/forgerock/commons/ui/common/main/Router",
    "ThemeManager"      : "org/forgerock/openam/ui/common/util/ThemeManager",
    "UserProfileView"   : "org/forgerock/commons/ui/user/profile/UserProfileView"
};
