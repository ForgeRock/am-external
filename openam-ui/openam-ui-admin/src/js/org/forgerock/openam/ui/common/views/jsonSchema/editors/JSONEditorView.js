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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import "popoverclickaway";

import _ from "lodash";
import $ from "jquery";
import Backbone from "backbone";
import JSONEditor from "exports-loader?window.JSONEditor!json-editor";

import HelpPopoverPartial from "templates/common/jsonSchema/editors/_HelpPopover";
import JSONEditorTheme from "org/forgerock/openam/ui/admin/utils/JSONEditorTheme";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Selectize from "selectize";
import {
    convertPlaceholderSchemaToReadOnly,
    flattenPlaceholder
} from "org/forgerock/commons/ui/common/util/PlaceholderUtils";

function convertHelpBlocksToPopOvers (element) {
    const html = HelpPopoverPartial();
    $(element).find(".help-block:not(.errormsg)").addClass("hidden-lg hidden-md hidden-sm")
        .each((index, value) => {
            const helpPopOver = $(html);
            helpPopOver.popoverclickaway({
                container: "#content",
                html: true,
                placement: "auto top",
                content: value.innerHTML
            }).click((event) => {
                event.preventDefault();
            });

            $(value).parent().append(helpPopOver);
        });
}
/**
 * Passwords are not delivered to the UI from the server. Thus we set a placeholder informing the user that
 * the password will remain unchanged if they do nothing.
 * @param {DOMElement} element The element to perform the element search from
 */
function setPlaceholderOnPasswords (element) {
    $(element).find("input:password").attr("placeholder", $.t("common.form.passwordPlaceholder"));
}

/**
 * The "allowEmptyOption" setting on selectize defaults to false to accomodate the common <select> practice of
 * having the first empty option to act as a placeholder. The following enum plugin checks for the presence of this
 * empty option and applies the functionality accordingly. If an empty option is present, then the "dropdown_header"
 * plugin will be applied, with the display name of the enum title. Otherwise if no empty option is present, the
 * backspace will be disabled stopping invalid empty values being set on the enum.
 */
Selectize.define("enum_plugin", function (options) {
    const schema = options.schema;
    const emptyOptionIndex = _.indexOf(schema.enum, "");
    const hasEmptyOption = emptyOptionIndex > -1;

    const noBackspacePlugin = () => {
        const notBackspace = (event) => !event || event.keyCode !== 8;
        this.deleteSelection = (() => {
            const original = this.deleteSelection;
            return (event) => {
                if (notBackspace(event)) {
                    return original.apply(this, arguments);
                }
                return false;
            };
        })();
    };

    const dropdownHeaderPlugin = () => {
        this.plugins.settings["dropdown_header"] = {
            title: schema.options.enum_titles[emptyOptionIndex]
        };
        this.require("dropdown_header");
        this.setup = (() => {
            const original = this.setup;
            return () => {
                original.apply(this, arguments);
                this.$dropdown.on("mousedown", ".selectize-dropdown-header", () => {
                    this.setValue("");
                    this.close();
                    this.blur();
                    return false;
                });
            };
        })();
    };

    if (hasEmptyOption) {
        dropdownHeaderPlugin();
    } else {
        noBackspacePlugin();
    }
});

function applyJSONEditorToElement (element, options) {
    const { schema, values, hideInheritance = false } = options;
    const GRID_COLUMN_WIDTH_1 = 6;
    const GRID_COLUMN_WIDTH_2 = 4;

    JSONEditor.plugins.selectize.enable = true;
    JSONEditor.plugins.selectize.plugins = (schema) => [{ name: "enum_plugin", options: { schema } }];
    JSONEditor.defaults.themes.openam = JSONEditorTheme.getTheme(GRID_COLUMN_WIDTH_1, GRID_COLUMN_WIDTH_2);

    let actualSchema = schema.toFlatWithInheritanceMeta(values);
    let actualValues = values.removeInheritance();

    actualSchema = actualSchema.raw;
    actualValues = actualValues.raw;

    actualSchema = convertPlaceholderSchemaToReadOnly(actualValues, actualSchema);
    const editor = new JSONEditor(element[0], {
        "disable_collapse": true,
        "disable_edit_json": true,
        "disable_properties": true,
        "hide_inheritance": hideInheritance,
        "iconlib": "fontawesome4",
        "schema": actualSchema,
        "theme": "openam",
        values: actualValues
    });

    convertHelpBlocksToPopOvers(element);
    setPlaceholderOnPasswords(element);
    actualValues = flattenPlaceholder({ ...actualValues });
    editor.setValue(actualValues);

    return editor;
}

const JSONEditorView = Backbone.View.extend({
    className: "jsoneditor-block",
    initialize (options) {
        if (!(options.schema instanceof JSONSchema)) {
            throw new TypeError("[JSONEditorView] \"schema\" argument is not an instance of JSONSchema.");
        }
        if (!(options.values instanceof JSONValues)) {
            throw new TypeError("[JSONEditorView] \"values\" argument is not an instance of JSONValues.");
        }

        this.options = _.defaults(options, {
            displayTitle: true
        });
    },
    toggleInheritance (propertySchemaPath, propValue, isInherited) {
        // update the data to hold whatever is now on UI before doing further manipulations
        this.options.values = this.options.values.extend(this.getData());

        this.options.values = this.options.values.addValueForPath([propertySchemaPath, "inherited"], isInherited);
        this.options.values = this.options.values.addValueForPath([propertySchemaPath, "value"], propValue);

        this.render();
    },
    render () {
        this.$el.empty();

        const watchlist = _.get(this.jsonEditor, "watchlist");

        this.jsonEditor = applyJSONEditorToElement(
            this.$el,
            this.options
        );

        this.jsonEditor.watchlist = watchlist;

        if (!this.options.displayTitle) {
            this.$el.find("[data-header]").parent().hide();
        }

        return this;
    },
    isValid () {
        return this.jsonEditor.validate().length === 0;
    },
    /**
    * Returns form data.
    * @returns {object} form data
    */
    getData () {
        let values = new JSONValues(this.jsonEditor.getValue());

        // Returns only the subset of values that were displayed to the user (via defaultProperties)
        if (this.options.schema.hasDefaultProperties()) {
            values = values.pick(this.options.schema.raw.defaultProperties);
        }

        values = values.nullifyEmptyPasswords(this.options.schema.getPasswordKeys());
        values = values.addInheritance(this.options.values.raw);

        return values.raw;
    },
    setData (data) {
        this.options.values = this.options.values.extend(data);
    },
    watch (path, callback) {
        this.jsonEditor.watch(path, callback);
    },
    destroy () {
        // unwatch all properties before destroy
        const watchlistKeys = _.keys(this.jsonEditor.watchlist);
        _.forEach(watchlistKeys, (watchlistKey) => {
            this.jsonEditor.unwatch(watchlistKey);
        });

        this.jsonEditor.destroy();
        this.jsonEditor = null;
    }
});

export default JSONEditorView;
