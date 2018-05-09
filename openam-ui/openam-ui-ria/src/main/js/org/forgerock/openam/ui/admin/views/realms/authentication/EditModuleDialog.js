/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/main/Router"
], ($, BootstrapDialog, Router) => {
    function closeDialog (dialog) {
        dialog.close();
    }
    function redirectAndClose (dialog) {
        Router.setUrl(dialog.options.data.link);
        dialog.close();
    }

    return function (name, chains, href) {
        BootstrapDialog.show({
            title: $.t("console.authentication.modules.inUse.title"),
            message: $.t("console.authentication.modules.inUse.message", {
                moduleName: name,
                usedChains: chains
            }),
            data: {
                link : href
            },
            buttons: [{
                label: $.t("common.form.cancel"),
                cssClass: "btn-default",
                action: closeDialog
            }, {
                label: $.t("common.form.yes"),
                cssClass: "btn-primary",
                action: redirectAndClose
            }]
        });
    };
});
