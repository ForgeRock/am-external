/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "templates/common/components/table/ReadOnlyRow",
    "templates/common/components/table/EditRow",
    "templates/common/components/table/NewRow"
], ($, _, Backbone, EventManager, ValidatorsManager, ReadOnlyRowTemplate, EditRowTemplate, NewRowTemplate) => {
    const getSelectOptions = (options, selectedValue) => {
        return _.map(options, (value) => ({ value, isSelected: value === selectedValue }));
    };

    const getColumns = (rowData, rowSchema) => {
        const columns = [];
        _.each(rowSchema.properties, (property, propertyName) => {
            columns[property.propertyOrder] = {
                data: rowData[propertyName],
                required: _.includes(rowSchema.required, propertyName) ? "required" : false
            };
            if (property.enum) {
                columns[property.propertyOrder].selectOptions = getSelectOptions(property.enum, rowData[propertyName]);
            }
        });
        return columns;
    };

    const getRenderData = (rowData, rowSchema) => {
        return { columns: getColumns(rowData, rowSchema) };
    };

    const getRowDataFromDom = (rowSchema, domElement) => {
        const rowData = {};
        _.each(rowSchema.properties, (property, propertyName) => {
            let element = "input";
            if (property.enum) {
                element = "select";
            }
            const domValue = domElement.find(`${element}[data-row-${property.propertyOrder}]`).val();
            rowData[propertyName] = domValue ? domValue.trim() : domValue;
        });
        return rowData;
    };

    return Backbone.View.extend({
        events: {
            "click button[data-add-row]": "addRow",
            "keyup [data-add-row]": "addRow",
            "click button[data-save-row]": "saveRow",
            "blur [data-save-row]" : "stopEvent",
            "keyup [data-save-row]": "saveRow",
            "dblclick td": "editRow",
            /* The event handler for the [data-edit-row] is attached to the "keyup" event instead of a "click",
            because inside this handler we are changing the focus to the input element and if the orignal event handler
            is attached to "click", then by the time the focus has changed the key is still pressed and when it is
            finally released, it triggers the "keyup" event handler of the input, which tries to save the row */
            "keyup [data-edit-row]": "editRow",
            "mouseup [data-edit-row]": "editRow",
            "click [data-delete-row]": "deleteRow",
            "click [data-undo-edit-row]": "exitEditMode"
        },
        tagName: "tr",

        initialize (rowData, rowSchema) {
            this.rowData = rowData;
            this.rowSchema = rowSchema;
        },

        renderTemplate (template) {
            const compiledTemplate = template(getRenderData(this.rowData, this.rowSchema));

            this.$el.html(compiledTemplate);
            ValidatorsManager.bindValidators(this.$el);
        },

        renderInReadOnlyMode () {
            this.$el.removeClass("am-inline-edit-table-row");
            this.renderTemplate(ReadOnlyRowTemplate);
            return this;
        },

        renderInEditMode () {
            this.$el.addClass("am-inline-edit-table-row");
            this.renderTemplate(EditRowTemplate);
            return this;
        },

        renderInNewMode () {
            this.$el.addClass("am-inline-edit-table-row");
            this.renderTemplate(NewRowTemplate);
            return this;
        },

        editRow (event) {
            if (event.type === "keyup" && event.keyCode !== 13) { return; }
            this.trigger("edit", this);
        },

        deleteRow () {
            this.trigger("delete", this);
        },

        exitEditMode () {
            this.trigger("exitEditMode", this);
        },

        focus () {
            this.$el.find("input:first").focus();
        },

        addRow (event) {
            if (event.type === "keyup" && event.keyCode !== 13) { return; }
            ValidatorsManager.validateAllFields(this.$el);
            const domData = getRowDataFromDom(this.rowSchema, this.$el);
            if (this.isDataValid(domData, this.rowSchema)) {
                this.rowData = domData;
                this.trigger("add", this);
            }
        },

        saveRow (event) {
            if (event.type === "keyup" && event.keyCode !== 13) { return; }
            ValidatorsManager.validateAllFields(this.$el);
            const domData = getRowDataFromDom(this.rowSchema, this.$el);
            if (this.isDataValid(domData, this.rowSchema)) {
                this.rowData = domData;
                this.trigger("exitEditMode", this);
            }
        },

        isDataValid (data, schema) {
            return _.every(data, (propertyValue, propertyName) => {
                return propertyValue !== undefined && propertyValue.length > 0 ||
                    !_.includes(schema.required, propertyName);
            });
        },

        getData () {
            return this.rowData;
        }
    });
});
