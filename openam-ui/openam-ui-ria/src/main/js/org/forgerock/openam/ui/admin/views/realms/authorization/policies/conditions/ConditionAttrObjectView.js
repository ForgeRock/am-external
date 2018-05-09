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
    "org/forgerock/openam/ui/admin/views/realms/authorization/policies/conditions/ConditionAttrBaseView",
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrObject",

    // jquery dependencies
    "selectize"
], ($, _, ConditionAttrBaseView, ConditionAttrObjectTemplate) => {
    return ConditionAttrBaseView.extend({
        template: ConditionAttrObjectTemplate,

        render (data, element, callback) {
            this.initBasic(data, element, "field-float-selectize data-obj");

            this.parentRender(function () {
                this.initSelectize();

                if (callback) {
                    callback();
                }
            });
        },

        initSelectize () {
            var view = this,
                title = "",
                itemData,
                options,
                keyValPair,
                propName,
                propVal,
                $item;

            this.$el.find("select.selectize").each(function () {
                $item = $(this);
                options = {
                    persist: false,
                    delimiter: ";",
                    onItemRemove (value) {
                        title = this.$input.parent().find("label").data().title;
                        itemData = view.data.itemData;
                        keyValPair = value.split(":");
                        delete itemData[title][keyValPair[0]];
                    },
                    onItemAdd (value) {
                        title = this.$input.parent().find("label").data().title;
                        itemData = view.data.itemData;
                        keyValPair = value.split(":");
                        propName = keyValPair[0];
                        propVal = keyValPair[1];

                        if (!itemData[title][propName]) {
                            itemData[title][propName] = [];
                        }

                        itemData[title][propName] = _.union(_.compact(propVal.split(",")), itemData[title][propName]);
                    },
                    create (input) {
                        return {
                            value: input,
                            text: input
                        };
                    },
                    onChange () {
                        title = this.$input.parent().find("label").data().title;
                        itemData = view.data.itemData;
                    },
                    createFilter (text) {
                        return (/^\w+:(?:\w+,?)+$/).test(text);
                    }
                };

                _.extend(options, { plugins: ["restore_on_backspace"] });
                $item.selectize(options);
            });
        }
    });
});
