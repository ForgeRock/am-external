/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { bindActionCreators } from "redux";
import { get } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { get as getGroup, getSchema, update }
    from "org/forgerock/openam/ui/admin/services/realm/identities/AllAuthenticatedService";
import { setSchema } from "store/modules/remote/config/realm/identities/groups/schema";
import { addOrUpdate as addOrUpdateValues } from "store/modules/local/config/realm/identities/groups/values";
import connectWithStore from "components/redux/connectWithStore";
import EditAllAuthenticated from "./EditAllAuthenticated";
import { addMessage, TYPE_DANGER } from "org/forgerock/commons/ui/common/components/Messages";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class EditAllAuthenticatedContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true
        };

        this.handleSave = this.handleSave.bind(this);
    }

    componentDidMount () {
        const realm = this.props.router.params[0];

        Promise.all([getSchema(realm), getGroup(realm)]).then(([schema, values]) => {
            this.setState({ isFetching: false });
            this.props.setSchema(schema[0]);
            this.props.addOrUpdateValues(values[0]);
        }, (response) => {
            this.setState({ isFetching: false });
            addMessage({ response, type: TYPE_DANGER });
        });
    }

    handleSave (privileges) {
        const realm = this.props.router.params[0];

        update(realm, { privileges }).then(() => {
            addMessage({ message: t("config.messages.AppMessages.changesSaved") });
        }, (response) => {
            addMessage({ response, type: TYPE_DANGER });
        });
    }

    render () {
        return (
            <EditAllAuthenticated
                isFetching={ this.state.isFetching }
                onSave={ this.handleSave }
                schema={ get(this.props.schema, "properties.privileges") }
                values={ get(this.props.values, "privileges") }
            />
        );
    }
}

EditAllAuthenticatedContainer.propTypes = {
    addOrUpdateValues: PropTypes.func.isRequired,
    router: withRouterPropType,
    schema: PropTypes.shape({
        type: PropTypes.string
    }),
    setSchema: PropTypes.func.isRequired,
    values: PropTypes.shape({
        type: PropTypes.string
    })
};

EditAllAuthenticatedContainer = connectWithStore(EditAllAuthenticatedContainer,
    (state) => {
        return {
            schema: state.remote.config.realm.identities.groups.schema,
            values: get(state.local.config.realm.identities.groups.values, "allauthenticatedusers")
        };
    },
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        addOrUpdateValues: bindActionCreators(addOrUpdateValues, dispatch)
    })
);
EditAllAuthenticatedContainer = withRouter(EditAllAuthenticatedContainer);

export default EditAllAuthenticatedContainer;
