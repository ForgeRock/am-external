/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * View that takes <code>JSONSchema</code> and <code>JSONValue</code> objects and renders them in a flat structure.
  * <p/>
  * This view only supports JSONSchema objects which <strong>are not collections</strong> (determined by
  * <code>#isCollection</code> upon <code>JSONSchema</code>) and outputs a simple list of input fields related to the
  * specification of the JSON Schema.
  * <p/>
  * e.g.<br>
  * <code>
  * <hr/>
  * Label 1 | &lt;input here&gt; |<br>
  * Label 2 | &lt;input here&gt; |<br>
  * <hr/>
  * </code>
  * @module org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView
  */
define([
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/views/jsonSchema/editors/JSONEditorView"
], ($, _, Backbone, JSONSchema, JSONValues, JSONEditorView) => {
    /**
     * There is no reliable method of knowing if the form rendered by the JSON Editor has finished being added to the
     * DOM. We do however wish to signal when render is complete so views can perform actions (e.g. enabling buttons
     * when the form is ready for input). The workaround is to add the callback to the browser event queue using
     * setTimeout meaning his callback will be executed after the render cycle has complete.
     * @param  {Function} callback Function to invoke after the timeout has expired
     */
    function invokeOnRenderedAfterTimeout (callback) {
        if (callback) {
            setTimeout(callback, 0);
        }
    }

    const FlatJSONSchemaView = Backbone.View.extend({
        initialize (options) {
            if (!(options.schema instanceof JSONSchema)) {
                throw new TypeError("[FlatJSONSchemaView] \"schema\" argument is not an instance of JSONSchema.");
            }
            if (!(options.values instanceof JSONValues)) {
                throw new TypeError("[FlatJSONSchemaView] \"values\" argument is not an instance of JSONValues.");
            }

            this.options = _.defaults(options, {
                showOnlyRequiredAndEmpty: false
            });
        },
        render () {
            let schema = this.options.schema;
            let displayForm = true;

            if (this.options.showOnlyRequiredAndEmpty) {
                const requiredSchemaKeys = this.options.schema.getRequiredPropertyKeys();
                const emptyValueKeys = this.options.values.getEmptyValueKeys();
                const requiredAndEmptyKeys = _.intersection(requiredSchemaKeys, emptyValueKeys);
                schema = schema.removeUnrequiredNonDefaultProperties().addDefaultProperties(requiredAndEmptyKeys);
                displayForm = !_.isEmpty(requiredAndEmptyKeys);
            }

            this.subview = new JSONEditorView({
                displayForm,
                hideInheritance: this.options.hideInheritance,
                displayTitle: false,
                el: this.$el,
                schema,
                values: this.options.values
            }).render();

            invokeOnRenderedAfterTimeout(this.options.onRendered);

            return this;
        },
        isValid () {
            return !this.subview || this.subview.isValid();
        },
        getData (...args) {
            if (this.subview) {
                return this.subview.getData(...args);
            }
        },
        setData (data) {
            if (this.subview) {
                // Update our local copy of values so a later invocation of #render will render the values that were last set
                this.options.values = this.options.values.extend(data);
                return this.subview.setData(data);
            }
        },
        watch (path, callback) {
            if (this.subview) {
                this.subview.watch(path, callback);
            }
        },
        destroy () {
            if (this.subview) {
                this.subview.destroy();
                this.subview = null;
            }
        }
    });

    return FlatJSONSchemaView;
});