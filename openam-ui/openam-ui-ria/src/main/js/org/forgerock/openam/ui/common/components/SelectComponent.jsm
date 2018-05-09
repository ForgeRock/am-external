/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import "selectize";
import $ from "jquery";
import { View } from "backbone";
import _ from "lodash";
import SelectComponentTemplate from "templates/common/components/SelectComponent";

const SELECTIZE_VALUE_KEY = "__selectize_key";

function requiredField (value, name) {
    if (value == null) {
        throw new Error(`Field ${name} is required`);
    }
}

function getRenderer (component) {
    return component ? (data) => {
        component.setData(data);
        return component.render().$el.html();
    } : undefined;
}

/**
 * A component for a "combobox" - a single-value select box with a textfield to allow the user to search for a
 * value.
 *
 * The user must select an existing option. i.e. They cannot enter whatever text they want.
 */
export default class SelectComponent extends View {
    /**
     * @callback SelectComponent~onChange
     * @param [Object] selectedOption - The object
     */

    /**
     * Constructs a new SelectComponent.
     *
     * If itemComponent and optionComponent are not specified, labelField must be specified.
     *
     * @param {Object[]} options - An array of objects that represent the available options the user can select.
     * @param {String[]} searchFields - The fields of the options objects to match the users input against.
     * @param {SelectComponent~onChange} onChange - Called when the selected option changes.
     * @param {String} [labelField] - The field of the options objects to be used to display in the list of options.
     * @param {Component} [itemComponent] - A component for rendering the currently selected option.
     * @param {Component} [optionComponent] - A component for rendering an option in the list of options.
     * @param {Object} [selectedOption] - The selected option (i.e. the current value of element).
     * @param {String} [placeholderText] - A string to display when no value has been selected.
     */
    initialize ({ options, searchFields, labelField, selectedOption, onChange, itemComponent, optionComponent,
        placeholderText = $.t("common.form.select") }) {
        requiredField(options, "options");
        requiredField(options, "searchFields");
        if (itemComponent == null || optionComponent == null) {
            requiredField(options, "labelField");
        }
        _.assign(this, { options, searchFields, labelField, selectedOption, onChange, itemComponent, optionComponent,
            placeholderText });
    }
    render () {
        const html = SelectComponentTemplate({ placeholder: this.placeholderText });
        const options = _.map(this.options, (option, index) => {
            const internalOption = _.clone(option);
            internalOption[SELECTIZE_VALUE_KEY] = index;
            return internalOption;
        });
        this.$el.html(html).find("select").selectize({
            options,
            items: [_.indexOf(this.options, this.selectedOption)],
            valueField: SELECTIZE_VALUE_KEY,
            searchField: this.searchFields,
            labelField: this.labelField,
            render: {
                item: getRenderer(this.itemComponent),
                option: getRenderer(this.optionComponent)
            },
            onChange: (index) => {
                this.onChange(this.options[index]);
            }
        });
        return this;
    }
}
