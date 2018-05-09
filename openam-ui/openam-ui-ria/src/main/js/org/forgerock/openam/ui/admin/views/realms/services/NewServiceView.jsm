/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import _ from "lodash";
import $ from "jquery";

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import GroupedJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/GroupedJSONSchemaView";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Router from "org/forgerock/commons/ui/common/main/Router";
import ServicesService from "org/forgerock/openam/ui/admin/services/realm/ServicesService";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import NewServiceTemplate from "templates/admin/views/realms/services/NewServiceTemplate";
import AlertPartial from "partials/alerts/_Alert";

function toggleCreate (el, enable) {
    el.find("[data-create]").prop("disabled", !enable);
}

class NewServiceView extends AbstractView {
    constructor () {
        super();

        this.template = NewServiceTemplate;
        this.partials = {
            "alerts/_Alert": AlertPartial
        };
        this.events = {
            "click [data-create]": "onCreateClick",
            "change [data-service-selection]": "onSelectService"
        };
    }
    render (args, callback) {
        this.data.realmPath = args[0];

        ServicesService.type.getCreatables(this.data.realmPath).then((creatableTypes) => {
            this.data.creatableTypes = creatableTypes;

            this.parentRender(() => {
                if (this.data.creatableTypes.length > 1) {
                    const serviceSelection = this.$el.find("[data-service-selection]");
                    serviceSelection.selectize({
                        onInitialize () {
                            this.$control_input.attr("id", "serviceSelection");
                        }
                    });
                } else if (this.data.creatableTypes[0] && this.data.creatableTypes[0]._id) {
                    this.selectService(this.data.creatableTypes[0]._id);
                }
                if (callback) { callback(); }
            });
        });
    }
    onSelectService (event) {
        this.selectService(event.target.value);
    }
    selectService (service) {
        toggleCreate(this.$el, false);

        if (service !== this.data.type && this.jsonSchemaView) {
            this.jsonSchemaView.remove();
            delete this.schema;
        }

        if (!_.isEmpty(service)) {
            this.data.type = service;

            ServicesService.instance.getInitialState(this.data.realmPath, this.data.type).then((response) => {
                this.schema = response.schema;
                const options = {
                    schema: this.schema,
                    values: response.values,
                    showOnlyRequiredAndEmpty: true,
                    onRendered: () => toggleCreate(this.$el, true)
                };

                if (this.schema.isCollection()) {
                    this.jsonSchemaView = new GroupedJSONSchemaView(options);
                } else {
                    this.jsonSchemaView = new FlatJSONSchemaView(options);
                }

                $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-json-form]"));
            }, () => {
                toggleCreate(this.$el, false);
            });
        }
    }
    onCreateClick () {
        const values = new JSONValues(this.jsonSchemaView.getData());
        const valuesWithoutNullPasswords = values.removeNullPasswords(this.schema);

        ServicesService.instance.create(this.data.realmPath, this.data.type, valuesWithoutNullPasswords.raw)
            .then(() => {
                Router.routeTo(Router.configuration.routes.realmsServiceEdit, {
                    args: _.map([this.data.realmPath, this.data.type], encodeURIComponent),
                    trigger: true
                });
            }, (response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
    }
}

export default NewServiceView;
