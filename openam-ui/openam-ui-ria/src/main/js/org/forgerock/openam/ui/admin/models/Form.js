/*
 * Copyright 2015-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @deprecated
 */
define([
    "jquery",
    "lodash",
    "exports-loader?window.JSONEditor!json-editor",
    "org/forgerock/openam/ui/admin/utils/JSONEditorTheme",
    "popoverclickaway", // depends on jquery and bootstrap
    "selectize" // jquery dependencies
], ($, _, JSONEditor, JSONEditorTheme) => {
    var obj = function Form (element, schema, values) {
        console.warn("[Form] \"Form\" is deprecated. Use a FlatJSONSchemaView or GroupedJSONSchemaView instead.");

        this.element = element;
        this.schema = schema;
        this.values = values;

        // Attributes that are identifiable as passwords
        const passwordProperties = _.where(schema.properties, { format: "password" });
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
            var group = $(this).parent(),
                element = $('<a class="btn info-button visible-lg-inline-block' +
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
     * @param  {Object} object    Object to filter
     * @param  {Array} attributes Attribute names to filter
     * @returns {Object}          Filtered object
     */
    function filterEmptyAttributes (object, attributes) {
        return _.omit(object, (value, key) => {
            if (_.contains(attributes, key)) {
                return _.isEmpty(value);
            } else {
                return false;
            }
        });
    }

    obj.prototype.data = function () {
        return filterEmptyAttributes(this.editor.getValue(), this.passwordAttributes);
    };

    obj.prototype.reset = function () {
        this.editor.setValue(_.pick(this.values, _.keys(this.schema.properties)));
    };

    obj.prototype.destroy = function () {
        this.editor.destroy();
    };

    return obj;
});
