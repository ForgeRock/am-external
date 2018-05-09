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
    "templates/admin/views/realms/authorization/policies/attributes/SubjectAttributesTemplate",

    // jquery dependencies
    "selectize"
], ($, _, AbstractView, SubjectAttributesTemplate) => {
    var SubjectResponseAttributesView = AbstractView.extend({
        element: "#userAttrs",
        template: SubjectAttributesTemplate,
        noBaseTemplate: true,
        attrType: "User",

        render (args, callback) {
            var self = this,
                attr;

            this.data.selectedUserAttributes = args[0];
            this.data.allUserAttributes = [];

            _.each(args[1], (propertyName) => {
                attr = {};
                attr.propertyName = propertyName;
                attr.selected = _.find(self.data.selectedUserAttributes, (obj) => {
                    return obj.propertyName === propertyName;
                });
                self.data.allUserAttributes.push(attr);
            });

            this.parentRender(() => {
                self.initSelectize();

                if (callback) {
                    callback();
                }
            });
        },

        getAttrs () {
            var data = [],
                attr,
                self = this;

            _.each(this.data.selectedUserAttributes, (value) => {
                attr = {};
                attr.type = self.attrType;
                attr.propertyName = value.propertyName || value;
                data.push(attr);
            });

            data = _.sortBy(data, "propertyName");

            return data;
        },

        initSelectize () {
            var self = this;

            this.$el.find(".selectize").each(function () {
                $(this).selectize({
                    plugins: ["restore_on_backspace"],
                    delimiter: false,
                    persist: false,
                    create: false,
                    hideSelected: true,
                    onChange (value) {
                        self.data.selectedUserAttributes = value;
                    }
                });
            });
        }
    });

    return new SubjectResponseAttributesView();
});
