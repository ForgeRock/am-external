/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { bindActionCreators } from "redux";
import { isEmpty, map } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/federation/CirclesOfTrustService";
import { setSchema } from "store/modules/remote/config/realm/applications/federation/circlesoftrust/schema";
import { setTemplate } from "store/modules/remote/config/realm/applications/federation/circlesoftrust/template";
import connectWithStore from "components/redux/connectWithStore";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewCircleOfTrust from "./NewCircleOfTrust";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class NewCircleOfTrustContainer extends Component {
    constructor (props) {
        super(props);

        this.state = {
            isFetching: true,
            name: ""
        };
        this.handleNameChange = this.handleNameChange.bind(this);
    }

    componentDidMount () {
        const realm = this.props.router.params[0];
        getInitialState(realm).then(({ schema, values }) => {
            this.setState({
                isFetching: false
            });
            this.props.setSchema(schema[0]);
            this.props.setTemplate(values[0]);
        }, (response) => {
            this.setState({ isFetching: false });
            Messages.addMessage({ response, type: Messages.TYPE_DANGER });
        });
    }

    handleNameChange (name) {
        this.setState({ name });
    }

    render () {
        const handleCreate = (formData) => {
            const realm = this.props.router.params[0];
            const values = new JSONValues(formData);
            const valuesWithoutNullPasswords = values.removeNullPasswords(new JSONSchema(this.props.schema));

            create(realm, valuesWithoutNullPasswords.raw, this.state.name).then(() => {
                Router.routeTo(Router.configuration.routes.realmsApplicationsFederationCirclesOfTrustEdit,
                    { args: map([realm, this.state.name], encodeURIComponent), trigger: true });
            }, (response) => {
                Messages.addMessage({ response, type: Messages.TYPE_DANGER });
            });
        };

        return (
            <NewCircleOfTrust
                isCreateAllowed={ !isEmpty(this.state.name) }
                isFetching={ this.state.isFetching }
                onCreate={ handleCreate }
                onNameChange={ this.handleNameChange }
                schema={ this.props.schema }
                template={ this.props.template }
            />
        );
    }
}

NewCircleOfTrustContainer.propTypes = {
    router: withRouterPropType,
    schema: PropTypes.shape({
        type: PropTypes.string.isRequired
    }),
    setSchema: PropTypes.func.isRequired,
    setTemplate: PropTypes.func.isRequired,
    template: PropTypes.shape({
        type: PropTypes.string.isRequired
    })
};

NewCircleOfTrustContainer = connectWithStore(NewCircleOfTrustContainer,
    (state) => ({
        schema: state.remote.config.realm.applications.federation.circlesoftrust.schema,
        template: state.remote.config.realm.applications.federation.circlesoftrust.template
    }),
    (dispatch) => ({
        setSchema: bindActionCreators(setSchema, dispatch),
        setTemplate: bindActionCreators(setTemplate, dispatch)
    })
);
NewCircleOfTrustContainer = withRouter(NewCircleOfTrustContainer);

export default NewCircleOfTrustContainer;
