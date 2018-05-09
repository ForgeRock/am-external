/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "templates/common/DefaultBaseTemplate",
    "org/forgerock/commons/ui/common/components/BootstrapDialog"
], function($, _, AbstractView, DefaultBaseTemplate, BootstrapDialog) {
    /**
     * @exports org/forgerock/commons/ui/common/components/BootstrapDialogView
     * @deprecated
     */
    var BootstrapDialogView = AbstractView.extend({
        contentTemplate: DefaultBaseTemplate,
        data: { },
        noButtons: false,
        closable : true,
        actions: [{
            label: function (){return $.t('common.form.close');},
            cssClass: "btn-default",
            type: "close"
        }],

        show: function(callback) {
            var self = this;
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

        loadContent: function() {
            var promise = $.Deferred();
            if (this.message === undefined) {
                const template = this.contentTemplate(this.data);
                promise.resolve(template);
            } else {
                promise.resolve(this.message);
            }
            return promise;
        },

        setTitle: function(title) {
            this.title = title;
        },

        addButton: function(button){
            if (!this.getButtons(button.label)){
                this.buttons.push(button);
            }
        },

        getButtons: function(label) {
            return _.find(this.buttons, function(a) {
                return a.label === label;
            });
        },

        setButtons: function() {
            var buttons = [];
            if (this.noButtons) {
                this.buttons = [];
            } else if (this.actions !== undefined && this.actions.length !== 0){
                $.each (this.actions, function(i, action){
                    if (action.type === "close") {
                        action.label = $.t('common.form.close');
                        action.cssClass = (action.cssClass ? action.cssClass : "btn-default");
                        action.action = function(dialog) {
                            dialog.close();
                        };
                    }
                    buttons.push(action);
                });
                this.buttons = buttons;
            }
        }
    });

    return BootstrapDialogView;
});
