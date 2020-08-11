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
 * Copyright 2019 ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import PropTypes from "prop-types";
import React, { Component } from "react";

import {
    get as getInstance,
    getSchema
} from "org/forgerock/openam/ui/admin/services/realm/federation/SAML2EntityProviderService";
import { set as setInstance } from "store/modules/remote/config/realm/applications/federation/entityproviders/instance";
import { set as setSchema } from "store/modules/remote/config/realm/applications/federation/entityproviders/schema";
import connectWithStore from "components/redux/connectWithStore";
import convertFormatToIsPassword from "components/form/schema/convertFormatToIsPassword";
import EditEntityProvider from "./EditEntityProvider";
import Loading from "components/Loading";
import Oops from "components/Oops";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class EditEntityProviderContainer extends Component {
    static propTypes = {
        router: withRouterPropType,
        schema: PropTypes.objectOf(PropTypes.any),
        setInstance: PropTypes.func.isRequired,
        setSchema: PropTypes.func.isRequired
    };
    state = { fetchError: false, isFetching: true };

    async componentDidMount () {
        const [realm, location,, entityId] = this.props.router.params;
        try {
            const [instance, schema] = await Promise.all([
                getInstance(realm, location, entityId),
                getSchema(realm, location)
            ]);

            this.props.setInstance(instance);
            this.props.setSchema(convertFormatToIsPassword(schema));
        } catch (error) {
            this.setState({ fetchError: true }); // eslint-disable-line react/no-did-mount-set-state
        } finally {
            this.setState({ isFetching: false }); // eslint-disable-line react/no-did-mount-set-state
        }
    }

    render () {
        if (this.state.isFetching) {
            return <Loading />;
        } else if (this.state.fetchError) {
            return <Oops />;
        } else {
            const [,, role] = this.props.router.params;
            const schema = this.props.schema.properties[role];
            return schema
                ? <EditEntityProvider schema={ schema } />
                : <Oops />;
        }
    }
}

EditEntityProviderContainer = connectWithStore(EditEntityProviderContainer,
    (state) => {
        return {
            schema: state.remote.config.realm.applications.federation.entityproviders.schema
        };
    },
    (dispatch) => ({
        setInstance: bindActionCreators(setInstance, dispatch),
        setSchema: bindActionCreators(setSchema, dispatch)
    })
);

EditEntityProviderContainer = withRouter(EditEntityProviderContainer);

export default EditEntityProviderContainer;
