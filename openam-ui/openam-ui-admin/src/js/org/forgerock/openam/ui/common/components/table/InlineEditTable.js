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
import $ from "jquery";
import Backbone from "backbone";

import EditRow from "org/forgerock/openam/ui/common/components/table/InlineEditRow";
import InlineEditTableTemplate from "templates/common/components/table/InlineEditTable";

const defaultKeyValueSchema = {
    required: ["key"],
    properties: {
        key: { title: $.t("common.form.propertyName"), propertyOrder: 0 },
        value: { title: $.t("common.form.propertyValue"), propertyOrder: 1 }
    }
};

export default Backbone.View.extend({
    template: InlineEditTableTemplate,

    /**
     * Initializes the table with editables rows. Only single row is allowed to be in edit mode at a time.
     * @param {object} props Component properties.
     * @param {Array<object>} props.values=[] Data array to be passed to the rows
     * @param {object} props.rowSchema The Schema of an item. Should be valid JSON Schema.
     */
    initialize ({ values = [], rowSchema = defaultKeyValueSchema }) {
        this.values = values;
        this.rowSchema = rowSchema;
        this.rows = [];
    },

    getHeaders () {
        const headers = [];
        _.each(this.rowSchema.properties, (item) => {
            headers[item.propertyOrder] = item.title;
        });
        return headers;
    },

    getRenderData () {
        return { headers: this.getHeaders() };
    },

    render () {
        this.$el.empty();

        const template = this.template(this.getRenderData());
        this.$el.html(template);

        this.tBody = this.$el.find("tbody");

        _.each(this.values, (value) => {
            const row = this.initRow(value);
            this.tBody.append(row.renderInReadOnlyMode().$el);
            this.rows.push(row);
        });

        this.appendEmptyNewRowToTheBottom();

        return this;
    },

    initRow (rowData = {}) {
        const row = new EditRow(rowData, this.rowSchema);

        const enterEditMode = (row) => {
            if (row === this.currentlyEditedRow || row === this.newRow) {
                return;
            }

            if (this.currentlyEditedRow) {
                this.currentlyEditedRow.renderInReadOnlyMode();
            }

            row.renderInEditMode().focus();
            this.currentlyEditedRow = row;
            this.newRow.$el.hide();
        };

        const exitEditMode = () => {
            if (this.currentlyEditedRow) {
                this.currentlyEditedRow.renderInReadOnlyMode();
                this.currentlyEditedRow = undefined;
            }
            this.newRow.$el.show();
        };

        const addRow = (row) => {
            this.rows.push(row);
            row.renderInReadOnlyMode();
            this.appendEmptyNewRowToTheBottom();
        };

        const deleteRow = (row) => {
            this.rows = _.without(this.rows, row);
            row.remove();
        };

        row.on("edit", enterEditMode);
        row.on("exitEditMode", exitEditMode);
        row.on("delete", deleteRow);
        row.on("add", addRow);

        return row;
    },

    appendEmptyNewRowToTheBottom () {
        const row = this.initRow();
        this.tBody.append(row.renderInNewMode().$el);
        this.newRow = row;
    },

    getData () {
        return _.map(this.rows, (row) => row.getData());
    },

    isValid () {
        return true;
    },

    setData (data) {
        this.values = data;
        this.rows = [];
        this.render();
    }
});
