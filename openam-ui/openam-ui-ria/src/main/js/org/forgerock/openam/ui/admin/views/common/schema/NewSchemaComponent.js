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
 * Copyright 2016-2017 ForgeRock AS.
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
                } else {
                    this.jsonSchemaView = new FlatJSONSchemaView(options);
                }

                UIUtils.compileTemplate(this.template, this.data).then((html) => {
                    this.$el.html(html);
                    this.$el.find("[data-json-form]").html(this.jsonSchemaView.render().$el);
                });
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
