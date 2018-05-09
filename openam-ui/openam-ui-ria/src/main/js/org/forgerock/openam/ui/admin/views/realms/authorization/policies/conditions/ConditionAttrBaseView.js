/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView"
], ($, _, AbstractView) => {
    return AbstractView.extend({
        noBaseTemplate: true,
        data: {},
        events: {
            "change select:not(.type-selection):not(.selectize)": "changeInput",
            "change input": "changeInput",
            "keyup  input": "changeInput",
            "dp.change input": "changeInput"
        },

        initBasic (data, el, cssClasses) {
            var elWrapper = $(`<div class="condition-attr form-inline ${cssClasses}"></div>`);
            el.append(elWrapper);

            this.data = data;
            this.element = el.find(elWrapper);
        },

        changeInput (e) {
            e.stopPropagation();

            var target = $(e.currentTarget),
                propTitle;

            if (!target.parent().children("label").length) {
                return; // this is a temporary workaround needed for a event leakage
            }

            propTitle = target.parent().children("label").data().title;

            this.data.itemData[propTitle] = e.currentTarget.value;

            this.populateInputGroup(target);
            this.populateAutoFillGroup(target);

            if (this.attrSpecificChangeInput) {
                this.attrSpecificChangeInput(e);
            }
        },

        populateInputGroup (target) {
            var group = target.closest(".attr-group"),
                inputs = group.find(":input"),
                populated = _.find(inputs, (el) => {
                    return el.value !== "";
                });

            inputs.each(function () {
                $(this).prop("required", !!populated);
            });
        },

        populateAutoFillGroup (target) {
            var group = target.closest("li").find("div.auto-fill-group"),
                first = group.eq(0),
                second = group.eq(1),
                firstVal, firstLabel,
                secondVal, secondLabel,
                data;

            if (first.length && second.length) {
                firstVal = first.find("input").val();
                firstLabel = first.find("label").data().title;
                secondVal = second.find("input").val();
                secondLabel = second.find("label").data().title;
                data = this.data.itemData;
            }

            if (firstVal !== "" && secondVal === "") {
                data[secondLabel] = data[firstLabel];
            } else if (firstVal === "" && secondVal !== "") {
                data[firstLabel] = data[secondLabel];
            } else if (firstVal === "" && secondVal === "") {
                delete data[firstLabel];
                delete data[secondLabel];
            }
        }
    });
});
