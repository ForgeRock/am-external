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
 * Copyright 2018 ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import { forEach, sortBy, values } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";
import uuidv4 from "uuid/v4";

import { addOrUpdate, set as setInstances, remove as removeInstance } from
    "store/modules/local/config/global/secretStores/instances/current/mappings/instances";
import { create, getAllByType, getSchema, getTemplate, update, remove } from
    "org/forgerock/openam/ui/admin/services/global/secretStores/SecretStoreMappingsService";
import { set as setSchema } from
    "store/modules/local/config/global/secretStores/instances/current/mappings/schema";
import { set as setTemplate } from
    "store/modules/local/config/global/secretStores/instances/current/mappings/template";
import connectWithStore from "components/redux/connectWithStore";
import ListMappings from "org/forgerock/openam/ui/admin/views/common/secretStores/edit/ListMappings";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class MappingsContainer extends Component {
    constructor (props) {
        super(props);

        this.state = { isFetching: true };

        this.handleCreate = this.handleCreate.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
        this.handleRefetchSchema = this.handleRefetchSchema.bind(this);
        this.handleSave = this.handleSave.bind(this);
    }

    async componentDidMount () {
        const [type, typeId] = this.props.router.params;

        try {
            const [{ result }, schema, template] = await Promise.all([
                getAllByType(type, typeId),
                getSchema(type, typeId),
                getTemplate(type, typeId)
            ]);

            this.props.setSchema(schema);
            this.props.setTemplate(template);
            this.props.setInstances(result);
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        } finally {
            this.setState({ isFetching: false }); // eslint-disable-line react/no-did-mount-set-state
        }
    }

    handleDelete (selectedItems) {
        const [type, typeId] = this.props.router.params;
        showConfirmationBeforeAction({
            message: t("console.secretStores.edit.mappings.confirmDeleteSelected", { count: selectedItems.length })
        }, async () => {
            try {
                const removeResponse = await remove(type, typeId, selectedItems);
                forEach(removeResponse, ({ _id }) => {
                    this.props.removeInstance(_id);
                });
                Messages.messages.displayMessageFromConfig("changesSaved");
            } catch (error) {
                Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
            }
        });
    }

    async handleRefetchSchema () {
        const [type, typeId] = this.props.router.params;

        try {
            const schema = await getSchema(type, typeId);

            this.props.setSchema(schema);
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
        }
    }

    async handleSave (values, id) {
        const [type, typeId] = this.props.router.params;
        try {
            const instance = await update(type, typeId, values, id);
            this.props.addOrUpdate(instance);
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
            throw error;
        }
    }

    async handleCreate (values) {
        const [type, typeId] = this.props.router.params;
        const id = uuidv4();
        try {
            const instance = await create(type, typeId, values, id);
            this.props.addOrUpdate(instance);
            Messages.addMessage({ message: t("console.secretStores.edit.mappings.mappingAdded") });
        } catch (error) {
            Messages.addMessage({ response: error, type: Messages.TYPE_DANGER });
            throw error;
        }
    }

    render () {
        return (
            <ListMappings
                instances={ this.props.instances }
                isFetching={ this.state.isFetching }
                onCreate={ this.handleCreate }
                onDelete={ this.handleDelete }
                onRefetchSchema={ this.handleRefetchSchema }
                onSave={ this.handleSave }
                schema={ this.props.schema }
                template={ this.props.template }
            />
        );
    }
}

MappingsContainer.propTypes = {
    addOrUpdate: PropTypes.func.isRequired,
    instances: PropTypes.arrayOf(PropTypes.object).isRequired,
    removeInstance: PropTypes.func.isRequired,
    router: withRouterPropType,
    schema:  PropTypes.objectOf(PropTypes.any),
    setInstances: PropTypes.func.isRequired,
    setSchema: PropTypes.func.isRequired,
    setTemplate: PropTypes.func.isRequired,
    template: PropTypes.objectOf(PropTypes.any)
};

MappingsContainer = connectWithStore(MappingsContainer,
    (state) => ({
        instances: sortBy(values(state.local.config.global.secretStores.instances.current.mappings.instances), "_id"),
        schema: state.local.config.global.secretStores.instances.current.mappings.schema,
        template: state.local.config.global.secretStores.instances.current.mappings.template
    }),
    (dispatch) => ({
        addOrUpdate: bindActionCreators(addOrUpdate, dispatch),
        removeInstance: bindActionCreators(removeInstance, dispatch),
        setInstances: bindActionCreators(setInstances, dispatch),
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
MappingsContainer = withRouter(MappingsContainer);

export default MappingsContainer;
