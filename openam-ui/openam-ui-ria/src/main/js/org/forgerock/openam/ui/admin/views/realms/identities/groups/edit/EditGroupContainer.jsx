/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { bindActionCreators } from "redux";
import { get, reduce } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { get as getGroup, getSchema, update, remove }
    from "org/forgerock/openam/ui/admin/services/realm/identities/GroupsService";
import { getAll as getAllUsers } from "org/forgerock/openam/ui/admin/services/realm/identities/UsersService";
import { setSchema } from "store/modules/remote/config/realm/identities/groups/schema";
import { addOrUpdate as addOrUpdateValues } from "store/modules/local/config/realm/identities/groups/values";
import connectWithStore from "components/redux/connectWithStore";
import EditGroup from "./EditGroup";
import FormHelper from "org/forgerock/openam/ui/admin/utils/FormHelper";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class EditGroupContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true
        };

        this.handleAddAll = this.handleAddAll.bind(this);
        this.handleDelete = this.handleDelete.bind(this);
        this.handleRemoveAll = this.handleRemoveAll.bind(this);
        this.handleSave = this.handleSave.bind(this);
    }

    componentDidMount () {
        const realm = this.props.router.params[0];
        const id = this.props.router.params[1];

        Promise.all([
            getSchema(realm),
            getGroup(realm, id),
            getAllUsers(realm)
        ]).then(([schema, values, users]) => {
            this.setState({ isFetching: false });
            this.addUsersSelectionToSchema(schema, users[0].result);
            this.props.setSchema(schema[0]);
            this.props.addOrUpdateValues(values[0]);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    addUsersSelectionToSchema (schema, allUsers) {
        const membersProperty = get(schema, "[0].properties.members.properties.uniqueMember.items");
        membersProperty.enum = [];
        membersProperty.options = { "enum_titles": [] };

        if (membersProperty) {
            reduce(allUsers, (users, user) => {
                users.enum.push(user.username);
                users.options.enum_titles.push(user.username);
                return users;
            }, membersProperty);
        } else {
            console.error("[EditGroup] Unable to add available identities to 'uniqueMember' property.");
        }
    }

    handleAddAll () {
        this.props.addOrUpdateValues({
            ...this.props.values,
            members: {
                uniqueMember: get(this.props.schema, "properties.members.properties.uniqueMember.items.enum")
            }
        });
    }

    handleRemoveAll () {
        this.props.addOrUpdateValues({
            ...this.props.values,
            members: {
                uniqueMember: []
            }
        });
    }

    handleSave (data) {
        const realm = this.props.router.params[0];
        const id = this.props.router.params[1];

        update(realm, data, id).then(() => {
            Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        }, (response) => {
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });

        this.props.addOrUpdateValues(data);
    }

    handleDelete (event) {
        event.preventDefault();

        FormHelper.showConfirmationBeforeDeleting({
            message: t("console.common.confirmDeleteItem")
        }, () => {
            const realm = this.props.router.params[0];
            const id = this.props.router.params[1];

            remove(realm, [id]).then(() => {
                Messages.addMessage({ message: t("config.messages.AppMessages.changesSaved") });

                Router.routeTo(Router.configuration.routes.realmsIdentities, {
                    args: [encodeURIComponent(realm)], trigger: true
                });
            }, (model, response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        });
    }

    render () {
        return (
            <EditGroup
                id={ this.props.router.params[1] }
                isFetching={ this.state.isFetching }
                onAddAll={ this.handleAddAll }
                onDelete={ this.handleDelete }
                onRemoveAll={ this.handleRemoveAll }
                onSave={ this.handleSave }
                schema={ this.props.schema }
                values={ this.props.values }
            />
        );
    }
}

EditGroupContainer.propTypes = {
    addOrUpdateValues: PropTypes.func.isRequired,
    router: withRouterPropType,
    schema: PropTypes.shape({ type: PropTypes.string }),
    setSchema: PropTypes.func.isRequired,
    values: PropTypes.shape({ type: PropTypes.string })
};

EditGroupContainer = connectWithStore(EditGroupContainer,
    (state, props) => {
        const id = props.router.params[1];

        return {
            schema: state.remote.config.realm.identities.groups.schema,
            values: get(state.local.config.realm.identities.groups.values, id)
        };
    },
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        addOrUpdateValues: bindActionCreators(addOrUpdateValues, dispatch)
    })
);
EditGroupContainer = withRouter(EditGroupContainer);

export default EditGroupContainer;
