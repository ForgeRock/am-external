/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/commons/ui/common/util/Constants",
    "css/bootstrap-3.3.7-custom",
    "css/styles-admin"
], (Constants, BootstrapCustomCSS, StyleAdminCSS) => {
    var path = location.pathname.replace(new RegExp("^/|/$", "g"), "").split("/");
    path.splice(-1);
    path = path.join("/");
    // If we are using OpenAM at the root context then we won't need to prepend the context path with a /
    Constants.context = (path === "") ? path : `/${path}`;
    Constants.CONSOLE_PATH = `${Constants.context}/console`;
    Constants.OPENAM_HEADER_PARAM_CUR_PASSWORD = "currentpassword";

    // Patterns
    Constants.IPV4_PATTERN =
        "^(((25[0-5])|(2[0-4]\\d)|(1\\d\\d)|([1-9]?\\d)))((\\.((25[0-5])|(2[0-4]\\d)|(1\\d\\d)|([1-9]?\\d))){3})";
    Constants.IPV6_PATTERN =
        "^((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4][0-" +
        "9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3})|:))|(([0-9A-Fa-f]{1,4}:){5" +
        "}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][" +
        "0-9]|[1-9]?[0-9])){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5" +
        "]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa" +
        "-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[" +
        "0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}" +
        "){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0" +
        "-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}" +
        ":((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(" +
        ":(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25" +
        "[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:)))(%.+)?$";
    Constants.NUMBER_PATTERN = "[-+]?[0-9]*[.,]?[0-9]+";
    Constants.INTEGER_PATTERN = "\\d+";

    // Theme
    Constants.DEFAULT_STYLESHEETS = [BootstrapCustomCSS, StyleAdminCSS];
    Constants.EVENT_THEME_CHANGED = "main.EVENT_THEME_CHANGED";

    Constants.EVENT_REDIRECT_TO_JATO_FEDERATION = "main.navigation.EVENT_REDIRECT_TO_JATO_FEDERATION";

    Constants.SELF_SERVICE_FORGOTTEN_USERNAME = "selfservice/forgottenUsername";
    Constants.SELF_SERVICE_RESET_PASSWORD = "selfservice/forgottenPassword";
    Constants.SELF_SERVICE_REGISTER = "selfservice/userRegistration";

    return Constants;
});
