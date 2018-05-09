/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/utils/FormHelper
 * @deprecated
 */
define([
    "org/forgerock/openam/ui/admin/utils/form/bindSavePromiseToElement",
    "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction",
    "org/forgerock/openam/ui/admin/utils/form/setActiveTab",
    "org/forgerock/openam/ui/admin/utils/deprecatedWarning"
], (bindSavePromiseToElement, showConfirmationBeforeAction, setActiveTab, deprecatedWarning) => {
    const obj = {};

    showConfirmationBeforeAction = showConfirmationBeforeAction.default;

    obj.bindSavePromiseToElement = function (promise, element) {
        deprecatedWarning(
            "FormHelper.bindSavePromiseToElement",
            "org/forgerock/openam/ui/admin/utils/form/bindSavePromiseToElement"
        );
        return bindSavePromiseToElement(promise, element);
    };

    obj.showConfirmationBeforeDeleting = function (msg, deleteCallback) {
        deprecatedWarning(
            "FormHelper.showConfirmationBeforeDeleting",
            "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction"
        );
        return showConfirmationBeforeAction(msg, deleteCallback);
    };

    obj.setActiveTab = function (msg, deleteCallback) {
        deprecatedWarning(
            "FormHelper.setActiveTab",
            "org/forgerock/openam/ui/admin/utils/form/setActiveTab"
        );
        return setActiveTab(msg, deleteCallback);
    };

    return obj;
});
