/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { chain } from "lodash";
import { t } from "i18next";

import { getSchema, get, remove, update } from
    "org/forgerock/openam/ui/admin/services/realm/sts/STSService";
import { SOAP_STS } from "org/forgerock/openam/ui/admin/services/realm/sts/STSTypes";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import PartialBasedView from "org/forgerock/openam/ui/common/components/PartialBasedView";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import Router from "org/forgerock/commons/ui/common/main/Router";
import setFocusToFoundInput from "org/forgerock/openam/ui/admin/utils/form/setFocusToFoundInput";
import TabComponent from "org/forgerock/openam/ui/common/components/TabComponent";
import TabSearch from "org/forgerock/openam/ui/admin/views/common/TabSearch";
import EditSoapSTSTemplate from "templates/admin/views/realms/sts/EditSoapSTSTemplate";

class EditSoapSTS extends AbstractView {
    constructor () {
        super();

        this.template = EditSoapSTSTemplate;

        this.events = {
            "click [data-delete]": "onDelete",
            "click [data-save]": "onSave"
        };
    }

    createTabs (schema) {
        return chain(schema.raw.properties)
            .map((value, key) => ({ id: key, order: value.propertyOrder, title: value.title }))
            .sortBy("order")
            .value();
    }

    getJSONSchemaView () {
        return this.subview.getBody();
    }

    render ([realm, id]) {
        this.data = {
            title: id,
            headerActions: [{
                actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times"
            }]
        };

        this.realm = realm;

        Promise.all([
            getSchema(realm, SOAP_STS),
            get(realm, SOAP_STS, id)
        ]).then(([schema, values]) => {
            this.schema = new JSONSchema(schema[0]);
            this.values = new JSONValues(values[0]);

            this.parentRender(() => {
                const tabs = this.createTabs(this.schema);
                this.subview = new TabComponent({
                    tabs,
                    createBody: (id) => {
                        const view = new FlatJSONSchemaView({
                            schema: new JSONSchema(this.schema.raw.properties[id]),
                            values: new JSONValues(values[0][id])
                        });
                        return view;
                    },
                    createFooter: () => new PartialBasedView({ partial: "form/_JSONSchemaFooter" })
                });
                const options = {
                    properties: this.schema.raw.properties,
                    onChange: (id, value) => {
                        this.subview.setTabId(id);
                        setFocusToFoundInput(this.$el.find(`[data-schemapath="root.${value}"]`));
                    }
                };
                this.$el.find("[data-tab-search]").append(new TabSearch(options).render().$el);

                this.subview.setElement("[data-json-form]");
                this.subview.render();
            });
        });
    }

    onSave () {
        if (!this.getJSONSchemaView().isValid()) {
            Messages.addMessage({
                message: t("common.form.validation.errorsNotSaved"), type: Messages.TYPE_DANGER
            });
            return;
        }

        this.updateValues();

        const valuesWithoutNullPasswords = this.values.removeNullPasswords(this.schema);
        const id = valuesWithoutNullPasswords.raw._id;

        update(this.realm, SOAP_STS, id, valuesWithoutNullPasswords.raw).then(() => {
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    updateValues () {
        this.values = this.values.extend({
            [this.getSection()]: this.getJSONSchemaView().getData()
        });
    }

    getSection () {
        return this.subview.getTabId();
    }

    onDelete (event) {
        event.preventDefault();

        FormHelper.showConfirmationBeforeDeleting({
            message: t("console.common.confirmDeleteItem")
        }, () => {
            remove(this.realm, SOAP_STS, [this.values.raw]).then(() => {
                Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });

                Router.routeTo(Router.configuration.routes.realmsSts, {
                    args: [encodeURIComponent(this.realm)], trigger: true
                });
            }, (model, response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        });
    }
}

export default EditSoapSTS;
