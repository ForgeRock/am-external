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
 * Copyright 2015-2018 ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";

export default AbstractView.extend({
    noBaseTemplate: true,
    data: {},
    events: {
        "change select:not(.type-selection):not(.selectize)": "changeInput",
        "change input": "changeInput",
        "keyup  input": "changeInput",
        "dp.change input": "changeInput"
    },

    initBasic (data, el, cssClasses) {
        const elWrapper = $(`<div class="condition-attr form-inline ${cssClasses}"></div>`);
        el.append(elWrapper);

        this.data = data;
        this.element = el.find(elWrapper);
    },

    changeInput (e) {
        e.stopPropagation();

        const target = $(e.currentTarget);

        if (!target.parent().children("label").length) {
            return; // this is a temporary workaround needed for a event leakage
        }

        const propTitle = target.parent().children("label").data().title;

        this.data.itemData[propTitle] = e.currentTarget.value;

        this.populateInputGroup(target);
        this.populateAutoFillGroup(target);

        if (this.attrSpecificChangeInput) {
            this.attrSpecificChangeInput(e);
        }
    },

    populateInputGroup (target) {
        const group = target.closest(".attr-group");
        const inputs = group.find(":input");
        const populated = _.find(inputs, (el) => {
            return el.value !== "";
        });

        inputs.each(function () {
            $(this).prop("required", !!populated);
        });
    },

    populateAutoFillGroup (target) {
        const group = target.closest("li").find("div.auto-fill-group");
        const first = group.eq(0);
        const second = group.eq(1);
        let firstVal;
        let firstLabel;
        let secondVal;
        let secondLabel;
        let data;

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
