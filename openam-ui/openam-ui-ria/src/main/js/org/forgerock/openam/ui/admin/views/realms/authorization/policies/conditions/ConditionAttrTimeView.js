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
    "templates/admin/views/realms/authorization/policies/conditions/ConditionAttrTime",
    "clockPicker"
], ($, _, ConditionAttrBaseView, ConditionAttrTimeTemplate) => {
    return ConditionAttrBaseView.extend({
        template: ConditionAttrTimeTemplate,

        clickClockPicker (e) {
            e.stopPropagation();
            $(e.currentTarget).prev("input").clockpicker("show");
        },

        render (data, element, callback) {
            this.initBasic(data, element, "pull-left attr-group");

            this.events["click [data-clock]"] = _.bind(this.clickClockPicker, this);

            this.parentRender(function () {
                this.initClockPickers();

                if (callback) {
                    callback();
                }
            });
        },

        initClockPickers () {
            this.$el.find(".clockpicker").each(function () {
                var clock = $(this);
                clock.clockpicker({
                    placement: "top",
                    autoclose: true,
                    afterDone () {
                        clock.trigger("change");
                    }
                });
            });
        }
    });
});
