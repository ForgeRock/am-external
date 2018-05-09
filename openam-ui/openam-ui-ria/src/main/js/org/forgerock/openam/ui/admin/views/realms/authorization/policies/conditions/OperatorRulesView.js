/*
 * Copyright 2014-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "templates/admin/views/realms/authorization/policies/conditions/OperatorRulesTemplate"
], ($, _, AbstractView, OperatorRulesTemplate) => {
    return AbstractView.extend({
        template: OperatorRulesTemplate,
        noBaseTemplate: true,
        events: {
            "change    > select": "onSelect",
            "onfocus   select": "checkOptions",
            "mousedown select": "checkOptions"
        },
        data: {},
        mode: "append",
        select: null,
        dropbox: null,

        operatorI18nKey: "console.authorization.policies.edit.operators.",

        render (args, element, itemID, firstChild, callback) {
            var self = this;

            this.data = $.extend(true, {}, args);
            this.data.itemID = itemID;
            this.data.firstChild = firstChild;

            _.each(this.data.operators, (operator) => {
                operator.i18nKey = self.operatorI18nKey + operator.title;
            });

            this.setElement(element);

            const tpl = this.template(this.data);
            self.$el.append(tpl);

            self.setElement(`#operator${itemID}`);
            self.select = self.$el.find("select");
            self.delegateEvents();

            self.select.focus().trigger("change");
            self.$el.data("logical", true);
            self.dropbox = self.$el.find(".dropbox");

            self.$el.find('.fa[data-toggle="popover"]').popover();

            if (callback) {
                callback();
            }
        },

        setValue (value) {
            this.select.focus().val(value).trigger("change");
        },

        rebindElement () {
            this.delegateEvents();
        },

        onSelect (e) {
            var item = $(e.currentTarget).parent(),
                value = e.currentTarget.value,
                itemData = {},
                schema = _.find(this.data.operators, (obj) => {
                    return obj.title === value;
                });

            itemData.type = schema.title;
            _.map(schema.config.properties, (value, key) => {
                itemData[key] = value;
            });

            item.data("itemData", itemData);

            _.each(this.data.operators, (obj) => {
                item.removeClass(obj.title.toLowerCase());
            });
            item.addClass(value.toLowerCase());
        },

        checkOptions (e) {
            var parent = $(e.target).parent(),
                dropbox = parent.children("ol.dropbox"),
                select = dropbox.parent().children("select"),
                option = null;

            if (dropbox.children(":not(.dragged)").length > 1) {
                _.each(this.data.operators, (obj) => {
                    option = select.find(`option[value="${obj.title}"]`);
                    var isDisabled = !!(obj.config.properties.condition || obj.config.properties.subject);
                    option.prop("disabled", isDisabled);
                });
            } else {
                select.children().prop("disabled", false);
            }
        }
    });
});
