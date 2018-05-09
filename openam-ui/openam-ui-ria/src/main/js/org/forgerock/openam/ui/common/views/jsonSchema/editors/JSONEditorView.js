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
    "exports-loader?window.JSONEditor!json-editor",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/admin/utils/JSONEditorTheme",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "templates/common/jsonSchema/editors/_HelpPopover",
    "selectize",

    "popoverclickaway" // depends on jquery and bootstrap
], ($, _, Backbone, JSONEditor, JSONSchema, JSONValues, JSONEditorTheme, UIUtils, HelpPopoverPartial, Selectize) => {
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
                    if (notBackspace (event)) {
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

        const editor = new JSONEditor(element[0], {
            "disable_collapse": true,
            "disable_edit_json": true,
            "disable_properties": true,
            "hide_inheritance": hideInheritance,
            "iconlib": "fontawesome4",
            "schema": actualSchema,
            "theme": "openam"
        });

        convertHelpBlocksToPopOvers(element);
        setPlaceholderOnPasswords(element);

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
        * @returns {Object} form data
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

    return JSONEditorView;
});
