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
 * Copyright 2015-2019 ForgeRock AS.
 */

import { each } from "lodash";
import BootstrapDialogLib from "bootstrap-dialog";

function forceFocus (dialog) {
    dialog.$modalContent.find("[autofocus]").focus();
}

function setButtonStates (dialog) {
    each(dialog.options.buttons, (button) => {
        if (button.disabled === true) {
            dialog.getButton(button.id).disable();
        }
    });
}

function onShown (event) {
    forceFocus(event.data.dialog);
    setButtonStates(event.data.dialog);
}

/**
 * @exports org/forgerock/commons/ui/common/components/BootstrapDialogLib
 */
const BootstrapDialog = {};

BootstrapDialog.TYPE_DEFAULT = BootstrapDialogLib.TYPE_DEFAULT;
BootstrapDialog.TYPE_INFO = BootstrapDialogLib.TYPE_INFO;
BootstrapDialog.TYPE_PRIMARY = BootstrapDialogLib.TYPE_PRIMARY;
BootstrapDialog.TYPE_SUCCESS = BootstrapDialogLib.TYPE_SUCCESS;
BootstrapDialog.TYPE_WARNING = BootstrapDialogLib.TYPE_WARNING;
BootstrapDialog.TYPE_DANGER = BootstrapDialogLib.TYPE_DANGER;
BootstrapDialog.SIZE_NORMAL = BootstrapDialogLib.SIZE_NORMAL;
BootstrapDialog.SIZE_SMALL = BootstrapDialogLib.SIZE_SMALL;
BootstrapDialog.SIZE_WIDE = BootstrapDialogLib.SIZE_WIDE;
BootstrapDialog.SIZE_LARGE = BootstrapDialogLib.SIZE_LARGE;

each(["show", "confirm", "warning", "danger", "success"], (method) => {
    BootstrapDialog[method] = function (options) {
        const dialog = new BootstrapDialogLib[method](options);
        let type = options.type || BootstrapDialog.TYPE_PRIMARY;

        // Gives the dialog header the native bootstrap text color classes.
        // The title then inherits from this.
        type = type.replace("type", "text");
        dialog.getModalHeader().addClass(type);

        /**
         * Workaround for autofocus having no effect in Bootstrap modals
         * @see http://getbootstrap.com/javascript/#modals
         */
        dialog.getModal().on("shown.bs.modal", { dialog }, onShown);

        return dialog;
    };
});

export default BootstrapDialog;
