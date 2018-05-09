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
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/views/jsonSchema/editors/JSONEditorView",
    "templates/common/jsonSchema/editors/TogglableJSONEditorTemplate"
], ($, _, Backbone, JSONSchema, JSONValues, UIUtils, JSONEditorView, TogglableJSONEditorTemplate) => {
    const TogglableJSONEditorView = Backbone.View.extend({
        className: "jsoneditor-block",
        initialize (options) {
            if (!(options.schema instanceof JSONSchema)) {
                throw new TypeError("[TogglableJSONEditorView] \"schema\" argument is not an instance of JSONSchema.");
            }
            if (!(options.values instanceof JSONValues)) {
                throw new TypeError("[TogglableJSONEditorView] \"values\" argument is not an instance of JSONValues.");
            }

            this.options = options;
            this.options.enablePropertyKey = this.options.schema.getEnableKey();
            this.options.enablePropertyValue = this.options.values.raw[this.options.enablePropertyKey];
            this.options.schema = options.schema.omit(this.options.enablePropertyKey);
            this.options.values = options.values.omit(this.options.enablePropertyKey);
        },
        onEnabledChange (event) {
            const ANIMATION_DURATION_IN_MILLISECONDS = 250;
            const enabled = event.currentTarget.checked;

            this.options.enablePropertyValue = enabled;

            this.$el.find(".block-header").toggleClass("block-header-inactive");

            if (!this.options.schema.isEmpty()) {
                if (enabled) {
                    this.$el.find("[data-toggleable-json-editor]").slideDown(ANIMATION_DURATION_IN_MILLISECONDS);
                } else {
                    this.$el.find("[data-toggleable-json-editor]").slideUp(ANIMATION_DURATION_IN_MILLISECONDS);
                }
            }
        },
        render () {
            const html = TogglableJSONEditorTemplate(this.options);

            this.$el.html(html);

            this.$el.find("[data-json-editor-toggle]").change(_.bind(this.onEnabledChange, this));

            this.jsonEditor = new JSONEditorView({
                el: this.$el.find("[data-toggleable-json-editor]"),
                displayTitle: false,
                schema: this.options.schema,
                values: this.options.values
            });

            if (!this.options.enablePropertyValue) {
                this.$el.find("[data-toggleable-json-editor]").hide();
            }
            this.jsonEditor.render();

            return this;
        },
        getData () {
            const values = _.clone(this.jsonEditor.getData());
            values[this.options.enablePropertyKey] = this.options.enablePropertyValue;

            return values;
        }
    });

    return TogglableJSONEditorView;
});
