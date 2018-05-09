/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "lodash",
    "backbone",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/Promise",
    "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView",
    "org/forgerock/openam/ui/common/views/jsonSchema/GroupedJSONSchemaView",
    "org/forgerock/openam/ui/common/models/JSONValues"
], (_, Backbone, Messages, Router, UIUtils, Promise, FlatJSONSchemaView, GroupedJSONSchemaView, JSONValues) =>
    Backbone.View.extend({
        events: {
            "click [data-save]": "onSave",
            "click [data-cancel]": "onCancel",
            "keyup [data-instance-id]": "onIdChange",
            "change [data-instance-id]": "onIdChange"
        },

        onIdChange (event) {
            const isEmpty = _.isEmpty(event.currentTarget.value);
            this.setCreateEnabled(!isEmpty);
        },

        setCreateEnabled (enabled) {
            this.$el.find("[data-save]").prop("disabled", !enabled);
        },

        // TODO: document the interface and put guard clauses
        initialize ({
            data,
            listRoute,
            listRouteArgs,
            editRoute,
            editRouteArgs,
            template,
            getInitialState,
            createInstance
        }) {
            this.data = data;
            this.listRoute = listRoute;
            this.listRouteArgs = listRouteArgs;
            this.editRoute = editRoute;
            this.editRouteArgs = editRouteArgs;
            this.template = template;
            this.getInitialState = getInitialState;
            this.createInstance = createInstance;
        },

        render () {
            this.getInitialState().then((response) => {
                this.schema = response.schema;
                const options = {
                    schema: this.schema,
                    values: response.values,
                    showOnlyRequiredAndEmpty: true
                };

                if (this.schema.isCollection()) {
                    this.jsonSchemaView = new GroupedJSONSchemaView(options);
                    this.jsonSchemaView.render();
                    this.data.displayForm = this.jsonSchemaView.displayForm;
                } else {
                    this.jsonSchemaView = new FlatJSONSchemaView(options);
                    this.jsonSchemaView.render();
                    this.data.displayForm = this.jsonSchemaView.subview.options.displayForm;
                }

                const html = this.template(this.data);

                this.$el.html(html);
                this.$el.find("[data-json-form]").html(this.jsonSchemaView.$el);
            });

            return this;
        },

        onSave () {
            const formData = _.cloneDeep(this.jsonSchemaView.getData());
            const instanceId = this.$el.find("[data-instance-id]").val();
            formData["_id"] = instanceId;
            const values = new JSONValues(formData);
            const valuesWithoutNullPasswords = values.removeNullPasswords(this.schema);

            this.createInstance(valuesWithoutNullPasswords.raw).then(() => {
                Router.routeTo(this.editRoute, {
                    args: this.editRouteArgs(encodeURIComponent(instanceId)),
                    trigger: true
                });
            }, (response) => { Messages.addMessage({ response, type: Messages.TYPE_DANGER }); });
        },

        onCancel () {
            Router.routeTo(this.listRoute, { args: this.listRouteArgs, trigger: true });
        }
    })
);
