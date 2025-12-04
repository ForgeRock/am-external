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
 * Copyright 2015-2025 Ping Identity Corporation.
 */

import "popoverclickaway";
import "selectize";

import _ from "lodash";
import $ from "jquery";
import JSONEditor from "exports-loader?window.JSONEditor!json-editor";
import JSONEditorTheme from "org/forgerock/openam/ui/admin/utils/JSONEditorTheme";
import {
    convertPlaceholderSchemaToReadOnly,
    flattenPlaceholder,
    containsPlaceholder,
    revertPlaceholdersToOriginalValue
} from "org/forgerock/commons/ui/common/util/PlaceholderUtils";

/**
 * @deprecated
 */

const Form = function Form (element, schema, values) {
    console.warn("[Form] \"Form\" is deprecated. Use a FlatJSONSchemaView or GroupedJSONSchemaView instead.");

    this.element = element;
    this.schema = convertPlaceholderSchemaToReadOnly(values, schema);
    this.values = flattenPlaceholder({ ...values });

    this.placeholderFound = false;
    const keys = Object.keys(this.values);

    for (let i = 0; i < keys.length; i++) {
        if (containsPlaceholder(this.values[keys[i]])) {
            this.placeholderFound = true;
            break;
        }
    }

    // Attributes that are identifiable as passwords
    const passwordProperties = _.filter(schema.properties, { format: "password" });
    this.passwordAttributes = _.map(passwordProperties, (property) => _.findKey(schema.properties, property));

    JSONEditor.plugins.selectize.enable = true;
    JSONEditor.defaults.themes.openam = JSONEditorTheme.getTheme(6, 4);

    this.editor = new JSONEditor(element, {
        "disable_collapse": true,
        "disable_edit_json": true,
        "iconlib": "fontawesome4",
        schema,
        "theme": "openam"
    });

    /**
     * Passwords are not delivered to the UI from the server. Thus we set a placeholder informing the user that
     * the password will remain unchanged if they do nothing
     */
    $(element).find("input:password").attr("placeholder", $.t("common.form.passwordPlaceholder"));
    $(element).find(".help-block:not(.errormsg)").addClass("hidden-lg hidden-md hidden-sm").each(function () {
        const group = $(this).parent();
        const element = $('<a class="btn info-button visible-lg-inline-block' +
                ' visible-md-inline-block visible-sm-inline-block" ' +
                'tabindex="0" data-toggle="popoverclickaway" ><i class="fa fa-info-circle"></i></a>');

        $(group).append(element);

        element.popoverclickaway({
            container: "#content",
            html: true,
            placement: "auto top",
            content: this.innerHTML
        });
        element.click((event) => {
            event.preventDefault();
        });
    });

    this.reset();
};

/**
 * Filters out empty, specified attributes from an object
 * @param  {object} object    Object to filter
 * @param  {Array} attributes Attribute names to filter
 * @returns {object}          Filtered object
 */
function filterEmptyAttributes (object, attributes) {
    return _.omitBy(object, (value, key) => {
        if (_.includes(attributes, key)) {
            return _.isEmpty(value);
        } else {
            return false;
        }
    });
}

Form.prototype.data = function () {
    const values = filterEmptyAttributes(this.editor.getValue(), this.passwordAttributes);
    if (this.placeholderFound) {
        return revertPlaceholdersToOriginalValue(values, this.schema);
    }
    return values;
};

Form.prototype.reset = function () {
    this.editor.setValue(_.pick(this.values, _.keys(this.schema.properties)));
};

Form.prototype.destroy = function () {
    this.editor.destroy();
};

export default Form;
