/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
* View that takes <code>JSONSchema</code> and <code>JSONValue</code> objects and renders them in a grouped structure.
* <p/>
* This view only supports JSONSchema objects which <strong>are collections</strong> (determined by
* <code>#isCollection</code> upon <code>JSONSchema</code>) and outputs headers for groups followed by a simple list of
* input fields related to that grouped within the specification of the JSON Schema.
* <p/>
* e.g.<br>
* <code>
* <hr/>
* <i>Header 1</i><br>
* Label 1 | &lt;input here&gt; |<br>
* Label 2 | &lt;input here&gt; |<br>
* <br>
* <i>Header 2</i><br>
* Label 1 | &lt;input here&gt; |<br>
* Label 2 | &lt;input here&gt; |<br>
* <hr/>
* </code>
 * @module org/forgerock/openam/ui/common/views/jsonSchema/GroupedJSONSchemaView
 */
define([
    "jquery",
    "lodash",
    "backbone",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/views/jsonSchema/iteratees/createJSONEditorView",
    "org/forgerock/openam/ui/common/views/jsonSchema/iteratees/emptyProperties",
    "org/forgerock/openam/ui/common/views/jsonSchema/iteratees/setDefaultPropertiesToRequired",
    "org/forgerock/openam/ui/common/views/jsonSchema/iteratees/setDefaultPropertiesToRequiredAndEmpty",
    "org/forgerock/openam/ui/common/views/jsonSchema/iteratees/showEnablePropertyIfAllPropertiesHidden"
], ($, _, Backbone, JSONSchema, JSONValues, createJSONEditorView, emptyProperties, setDefaultPropertiesToRequired,
    setDefaultPropertiesToRequiredAndEmpty, showEnablePropertyIfAllPropertiesHidden) => {
    createJSONEditorView = createJSONEditorView.default;
    emptyProperties = emptyProperties.default;
    setDefaultPropertiesToRequired = setDefaultPropertiesToRequired.default;
    setDefaultPropertiesToRequiredAndEmpty = setDefaultPropertiesToRequiredAndEmpty.default;
    showEnablePropertyIfAllPropertiesHidden = showEnablePropertyIfAllPropertiesHidden.default;

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

    const GroupedJSONSchemaView = Backbone.View.extend({
        initialize (options) {
            if (!(options.schema instanceof JSONSchema)) {
                throw new TypeError("[GroupedJSONSchemaView] \"schema\" argument is not an instance of JSONSchema.");
            }
            if (!options.schema.isCollection()) {
                throw new Error("[GroupedJSONSchemaView] Only JSONSchema collections are supported by this view.");
            }
            if (!(options.values instanceof JSONValues)) {
                throw new TypeError("[GroupedJSONSchemaView] \"values\" argument is not an instance of JSONValues.");
            }

            this.options = _.defaults(options, {
                showOnlyRequired: false,
                showOnlyRequiredAndEmpty: false
            });
        },
        render () {
            const schemas = this.options.schema.getPropertiesAsSchemas();
            const values = this.options.values.raw;
            const orderedSchemaPropertyKeys = this.options.schema.getKeys(true);

            // Create an array of objects which each contain the schema and values paired together
            let orderedSchemaValuePairs = _.map(orderedSchemaPropertyKeys, (key) => ({
                key,
                hideInheritance: this.options.hideInheritance,
                schema: schemas[key],
                values: new JSONValues(values[key])
            }));

            if (this.options.showOnlyRequiredAndEmpty) {
                orderedSchemaValuePairs = _(orderedSchemaValuePairs)
                    .map(setDefaultPropertiesToRequiredAndEmpty)
                    .map(showEnablePropertyIfAllPropertiesHidden)
                    .omit(emptyProperties)
                    .value();
            } else if (this.options.showOnlyRequired) {
                orderedSchemaValuePairs = _(orderedSchemaValuePairs)
                    .map(setDefaultPropertiesToRequired)
                    .map(showEnablePropertyIfAllPropertiesHidden)
                    .omit(emptyProperties)
                    .value();
            }

            this.displayForm = !_.isEmpty(orderedSchemaValuePairs);

            this.subviews = _(orderedSchemaValuePairs)
                .map(createJSONEditorView)
                .invoke("render")
                .each((view) => { view.$el.appendTo(this.$el); })
                .value();

            invokeOnRenderedAfterTimeout(this.options.onRendered);

            return this;
        },
        getData () {
            const values = _.map(this.subviews, (view) => {
                let viewData;
                if (view.options.key) {
                    viewData = { [view.options.key]: view.getData() };
                } else {
                    viewData = view.getData();
                }
                return viewData;
            });

            return _.reduce(values, _.merge, {});
        }
    });

    return GroupedJSONSchemaView;
});
