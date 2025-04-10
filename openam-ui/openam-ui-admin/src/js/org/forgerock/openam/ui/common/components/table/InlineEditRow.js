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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import _ from "lodash";
import Backbone from "backbone";

import EditRowTemplate from "templates/common/components/table/EditRow";
import NewRowTemplate from "templates/common/components/table/NewRow";
import ReadOnlyRowTemplate from "templates/common/components/table/ReadOnlyRow";
import ValidatorsManager from "org/forgerock/commons/ui/common/main/ValidatorsManager";

const PASSWORD_COLUMNS = ["org.forgerock.openam.httpclienthandler.system.proxy.password"];

const getSelectOptions = (options, selectedValue) => {
    return _.map(options, (value) => ({ value, isSelected: value === selectedValue }));
};

const isPasswordProperty = (propertyName) => {
    return _.includes(PASSWORD_COLUMNS, propertyName);
};

const getColumns = (rowData, rowSchema) => {
    const columns = [];
    // Apply this to the value column only
    const isPasswordRow = isPasswordProperty(rowData["key"]);
    _.each(rowSchema.properties, (property, propertyName) => {
        columns[property.propertyOrder] = {
            data: rowData[propertyName],
            required: _.includes(rowSchema.required, propertyName) ? "required" : false
        };
        if (property.enum) {
            columns[property.propertyOrder].selectOptions = getSelectOptions(property.enum, rowData[propertyName]);
        }
        // May not be on advanced tab.
        if (rowSchema.properties.value && property.propertyOrder === rowSchema.properties.value.propertyOrder) {
            columns[property.propertyOrder].usePlaceholder = isPasswordRow;
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

const setTypeOfValueField = (rowSchema, domElement) => {
    // The edit value field should use password type input to mask the value.
    const rowData = getRowDataFromDom(rowSchema, domElement);
    const element = "input";
    if (rowSchema.properties.value.enum) {
        // Only change text/password values, not selects.
        return;
    }
    if (isPasswordProperty(rowData["key"])) {
        domElement.find(`${element}[data-row-${rowSchema.properties.value.propertyOrder}]`).attr("type", "password");
    } else {
        domElement.find(`${element}[data-row-${rowSchema.properties.value.propertyOrder}]`).attr("type", "text");
    }
};

export default Backbone.View.extend({
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
        if (event.type === "keyup") {
            setTypeOfValueField(this.rowSchema, this.$el);
            if (event.keyCode !== 13) {
                return;
            }
        }
        ValidatorsManager.validateAllFields(this.$el);
        const domData = getRowDataFromDom(this.rowSchema, this.$el);
        if (this.isDataValid(domData, this.rowSchema)) {
            this.rowData = domData;
            this.trigger("add", this);
        }
    },

    saveRow (event) {
        if (event.type === "keyup") {
            setTypeOfValueField(this.rowSchema, this.$el);
            if (event.keyCode !== 13) {
                return;
            }
        }
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
