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

import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";
import DefaultBaseTemplate from "themes/default/templates/common/DefaultBaseTemplate";

/**
 * @exports org/forgerock/commons/ui/common/components/BootstrapDialogView
 * @deprecated
 */
const BootstrapDialogView = AbstractView.extend({
    contentTemplate: DefaultBaseTemplate,
    data: { },
    noButtons: false,
    closable : true,
    actions: [{
        label () { return $.t("common.form.close"); },
        cssClass: "btn-default",
        type: "close"
    }],

    show (callback) {
        const self = this;
        self.setButtons();
        self.loadContent().then((content) => {
            self.type = self.type || BootstrapDialog.TYPE_DEFAULT;
            self.size = self.size || BootstrapDialog.SIZE_NORMAL;

            self.message = $("<div></div>").append(content);
            BootstrapDialog.show(self);
            if (callback) {
                callback();
            }
        });
    },

    loadContent () {
        const promise = $.Deferred();
        if (this.message === undefined) {
            const template = this.contentTemplate(this.data);
            promise.resolve(template);
        } else {
            promise.resolve(this.message);
        }
        return promise;
    },

    setTitle (title) {
        this.title = title;
    },

    addButton (button) {
        if (!this.getButtons(button.label)) {
            this.buttons.push(button);
        }
    },

    getButtons (label) {
        return _.find(this.buttons, (a) => {
            return a.label === label;
        });
    },

    setButtons () {
        const buttons = [];
        if (this.noButtons) {
            this.buttons = [];
        } else if (this.actions !== undefined && this.actions.length !== 0) {
            $.each (this.actions, (i, action) => {
                if (action.type === "close") {
                    action.label = $.t("common.form.close");
                    action.cssClass = (action.cssClass ? action.cssClass : "btn-default");
                    action.action = function (dialog) {
                        dialog.close();
                    };
                }
                buttons.push(action);
            });
            this.buttons = buttons;
        }
    }
});

export default BootstrapDialogView;
