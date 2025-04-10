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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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
    }

    componentDidMount () {
        const realm = this.props.router.params[0];
        getInitialState(realm).then(({ schema, values }) => {
            this.props.setSchema(schema);
            this.props.setTemplate(values);
        }).finally(() => {
            this.setState({ isFetching: false });
        });
    }

    handleCreate = (formData) => {
        const realm = this.props.router.params[0];
        const values = new JSONValues(formData);
        const valuesWithoutNullPasswords = values.removeNullPasswords(new JSONSchema(this.props.schema));

        create(realm, valuesWithoutNullPasswords.raw, this.state.name).then(() => {
            Router.routeTo(Router.configuration.routes.realmsApplicationsFederationCirclesOfTrustEdit, {
                args: map([realm, this.state.name], encodeURIComponent),
                trigger: true
            });
        });
    };

    handleNameChange = (name) => {
        this.setState({ name });
    };

    render () {
        return (
            <NewCircleOfTrust
                isCreateAllowed={ !isEmpty(this.state.name) }
                isFetching={ this.state.isFetching }
                onCreate={ this.handleCreate }
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
