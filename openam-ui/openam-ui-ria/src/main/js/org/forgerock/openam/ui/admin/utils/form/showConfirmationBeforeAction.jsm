/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { t } from "i18next";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";

/**
 * Shows a confirmation dialog before performing a dangerous action and calls action callback if needed.
 * @module org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction
 * @param  {object} msg Message object
 * @param  {string} [msg.type] Type of object on which action is performed
 * @param  {string} [msg.message] Confirmation message to show
 * @param  {function} actionCallback Action callback
 * @param  {string} [actionName] Name of the performed action
 * @example
 * clickHandler: function (event) {
 *   event.preventDefault();
 *   showConfirmationBeforeAction({type: "console.scripts.edit.script"}, deleteEntity);
 * }
 */
export default function showConfirmationBeforeAction (msg, actionCallback, actionName = t("common.form.delete")) {
    BootstrapDialog.confirm({
        type: BootstrapDialog.TYPE_DANGER,
        title: `${t("common.form.confirm")} ${actionName}`,
        message: msg.message ? msg.message : t("console.common.confirmDeleteText", { type: msg.type }),
        btnOKLabel: actionName,
        btnOKClass: "btn-danger",
        callback: (result) => {
            if (result && actionCallback) {
                actionCallback();
            }
        }
    });
}
