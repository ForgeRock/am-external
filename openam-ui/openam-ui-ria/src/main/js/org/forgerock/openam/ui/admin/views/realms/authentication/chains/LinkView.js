/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "templates/admin/views/realms/authentication/chains/LinkTemplate",
    "templates/admin/views/realms/authentication/chains/PopoverTemplate",
    "templates/admin/views/realms/authentication/chains/_CriteriaFooter"
], ($, _, AbstractView, BootstrapDialog, LinkTemplate, PopoverTemplate,
    CriteriaFooterTemplate) => {
    var LinkView = AbstractView.extend({
        template: LinkTemplate,
        popoverTemplate: PopoverTemplate,
        mode: "replace",
        partials : {
            "templates/admin/views/realms/authentication/chains/_CriteriaFooter": CriteriaFooterTemplate
        },
        events: {
            "click [data-edit]"                   : "editItem",
            "click [data-delete]"                 : "deleteItem",
            "change [data-select-criteria]"       : "selectCriteria",
            "mouseenter [data-auth-criteria-info]": "showPopover",
            "focusin    [data-auth-criteria-info]": "showPopover",
            "mouseleave [data-auth-criteria-info]": "hidePopover",
            "focusout   [data-auth-criteria-info]": "hidePopover"
        },

        deleteItem () {
            this.parent.data.form.chainData.authChainConfiguration.splice(this.$el.index(), 1);
            this.parent.validateChain();
            this.remove();
        },

        editItem () {
            this.parent.editItem(this);
        },

        showPopover () {
            var self = this,
                popover = this.$el.find("[data-auth-criteria-info]").data("bs.popover"),
                selected = this.$el.find("[data-select-criteria] option:selected"),
                index = selected.index(),
                data = {
                    criteria : selected.val().toLowerCase(),
                    passText : $.t(`console.authentication.editChains.criteria.${index}.passText`),
                    failText : $.t(`console.authentication.editChains.criteria.${index}.failText`)
                };

            const template = self.popoverTemplate(data);
            popover.options.content = template;
            popover.options.title = selected.text();
            self.$el.find("[data-auth-criteria-info]").popover("show");
        },

        hidePopover () {
            this.$el.find("[data-auth-criteria-info]").popover("hide");
        },

        renderArrows () {
            const html = CriteriaFooterTemplate({ type: this.data.linkConfig.criteria });
            this.$el.find(".criteria-view").html(html);
        },

        selectCriteria () {
            this.data.linkConfig.criteria = this.$el.find("[data-select-criteria] option:selected").val();
            this.parent.validateChain();
            this.renderArrows();
        },

        render () {
            var self = this;

            this.data.optionsLength = _.keys(this.data.linkConfig.options).length;
            this.data.typeDescription = this.getModuleDesciption();
            this.parent.validateChain();

            this.parentRender(() => {
                self.renderArrows();
                self.$el.find("[data-auth-criteria-info]").popover({
                    trigger : "manual",
                    container : "body",
                    html : "true",
                    placement : "right",
                    template: '<div class="popover am-link-popover" role="tooltip"><div class="arrow"></div>' +
                        '<h3 class="popover-title"></h3><div class="popover-content"></div></div>'
                });
            });
        },

        getModuleDesciption () {
            // The server allows for deletion of modules that are in use within a chain.
            // The chain itself will still have a reference to the deleetd module.
            // Below we are checking if the module is present. If it isn't the typeDescription is left blank;
            var name = _.find(this.data.allModules, { _id : this.data.linkConfig.module });

            if (this.data.linkConfig.module && name) {
                return name.typeDescription;
            }
            return;
        }

    });

    return LinkView;
});
