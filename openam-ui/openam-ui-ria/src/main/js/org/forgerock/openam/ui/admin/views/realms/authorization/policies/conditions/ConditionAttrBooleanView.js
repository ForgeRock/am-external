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
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrBoolean"
], ($, _, ConditionAttrBaseView, ConditionAttrBooleanTemplate) => {
    return ConditionAttrBaseView.extend({
        template: ConditionAttrBooleanTemplate,

        render (data, element, callback) {
            this.initBasic(data, element, "field-float-pattern data-obj button-field");

            this.events["click [data-btn]"] = _.bind(this.buttonControlClick, this);
            this.events["keyup [data-btn]"] = _.bind(this.buttonControlClick, this);

            this.parentRender(() => {
                if (callback) {
                    callback();
                }
            });
        },

        buttonControlClick (e) {
            if (e.type === "keyup" && e.keyCode !== 13) {
                return;
            }

            var btn = $(e.currentTarget),
                btnGroup = btn.parent(".btn-group"),
                parentBtnGroup,
                label,
                secondBtnGroup;

            if (btnGroup.hasClass("active")) {
                return;
            }

            parentBtnGroup = btnGroup.parent(".btn-group");
            label = parentBtnGroup.prev("label").data().title;

            secondBtnGroup = parentBtnGroup.find(".btn-group.active");

            this.data.itemData[label] = btn.data("val");

            secondBtnGroup.removeClass("active");
            btnGroup.addClass("active");
        }
    });
});
