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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
import { bindActionCreators } from "redux";
import { forEach, sortBy, values } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { addOrUpdate, set as setInstances, remove as removeInstance } from
    "store/modules/local/config/realm/secretStores/instances/current/mappings/instances";
import { create, getAllByType, getSchema, getTemplate, update, remove } from
    "org/forgerock/openam/ui/admin/services/realm/secretStores/SecretStoreMappingsService";
import { set as setSchema } from
    "store/modules/local/config/realm/secretStores/instances/current/mappings/schema";
import { set as setTemplate } from
    "store/modules/local/config/realm/secretStores/instances/current/mappings/template";
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
    }

    async componentDidMount () {
        const [realm, type, typeId] = this.props.router.params;

        try {
            const [{ result }, schema, template] = await Promise.all([
                getAllByType(realm, type, typeId),
                getSchema(realm, type, typeId),
                getTemplate(realm, type, typeId)
            ]);

            this.props.setSchema(schema);
            this.props.setTemplate(template);
            this.props.setInstances(result);
        } finally {
            this.setState({ isFetching: false }); // eslint-disable-line react/no-did-mount-set-state
        }
    }

    handleDelete = (selectedItems) => {
        const [realm, type, typeId] = this.props.router.params;
        showConfirmationBeforeAction({
            message: t("console.secretStores.edit.mappings.confirmDeleteSelected", { count: selectedItems.length })
        }, async () => {
            const removeResponse = await remove(realm, type, typeId, selectedItems);
            forEach(removeResponse, ({ _id }) => {
                this.props.removeInstance(_id);
            });
            Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
        });
    };

    handleRefetchSchema = async () => {
        const [realm, type, typeId] = this.props.router.params;
        const schema = await getSchema(realm, type, typeId);
        this.props.setSchema(schema);
    };

    handleSave = async (values, id) => {
        const [realm, type, typeId] = this.props.router.params;
        const instance = await update(realm, type, typeId, values, id);
        this.props.addOrUpdate(instance);
        Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
    };

    handleCreate = async (values) => {
        const [realm, type, typeId] = this.props.router.params;
        const instance = await create(realm, type, typeId, values, values.secretId);
        this.props.addOrUpdate(instance);
        Messages.addMessage({ message: t("console.secretStores.edit.mappings.mappingAdded") });
    };

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
        instances: sortBy(values(state.local.config.realm.secretStores.instances.current.mappings.instances), "_id"),
        schema: state.local.config.realm.secretStores.instances.current.mappings.schema,
        template: state.local.config.realm.secretStores.instances.current.mappings.template
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
