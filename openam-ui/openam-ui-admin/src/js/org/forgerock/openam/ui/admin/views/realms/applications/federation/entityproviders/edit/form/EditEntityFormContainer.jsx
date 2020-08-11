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
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import { update } from "org/forgerock/openam/ui/admin/services/realm/federation/SAML2EntityProviderService";
import connectWithStore from "components/redux/connectWithStore";
import EditEntityForm from "./EditEntityForm";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

class EditEntityFormContainer extends Component {
    static propTypes = {
        instance: PropTypes.objectOf(PropTypes.any).isRequired,
        router: withRouterPropType,
        selectedTab: PropTypes.string.isRequired,
        subInstance: PropTypes.objectOf(PropTypes.any).isRequired,
        subSchema: PropTypes.objectOf(PropTypes.any).isRequired
    };

    handleSave = async (data) => {
        const [realm, location, role, id] = this.props.router.params;
        const body = {
            ...this.props.instance,
            [role]: {
                ...this.props.instance[role],
                [this.props.selectedTab]: data
            }
        };

        await update(realm, location, id, body);
        Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
    };

    render () {
        const { subInstance, subSchema } = this.props;

        return (
            <EditEntityForm
                formData={ subInstance }
                onSave={ this.handleSave }
                schema={ subSchema }
            />
        );
    }
}

EditEntityFormContainer = connectWithStore(EditEntityFormContainer,
    (state, { router, selectedTab }) => {
        const [,, role] = router.params;
        return {
            instance: state.remote.config.realm.applications.federation.entityproviders.instance,
            subInstance: state.remote.config.realm.applications.federation.entityproviders.instance[role][selectedTab],
            subSchema: state.remote.config.realm.applications.federation.entityproviders.schema.properties[role]
                .properties[selectedTab]
        };
    }
);

EditEntityFormContainer = withRouter(EditEntityFormContainer);

export default EditEntityFormContainer;
