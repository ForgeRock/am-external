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
 * Copyright 2020-2025 Ping Identity Corporation.
 */
import { map } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import React, { Component } from "react";

import {
    COTServiceError,
    getAll as getAllCOT,
    update as updateCOT
} from "org/forgerock/openam/ui/admin/services/realm/federation/CirclesOfTrustService";

import {
    create
} from "org/forgerock/openam/ui/admin/services/realm/federation/SAML2EntityProviderService";
import Loading from "components/Loading";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewRemoteEntityProvider from "./NewRemoteEntityProvider";
import Oops from "components/Oops";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const createSchema = (cotIDs) => {
    return {
        "$schema": "http://json-schema.org/draft-07/schema#",
        type: "object",
        properties: {
            importFile: {
                propertyOrder: 0,
                title: t("console.applications.federation.entityProviders.new.remote.importFile"),
                type: "string",
                format: "file",
                acceptedFiles: ".xml"
            },
            ...cotIDs.length && {
                cot: {
                    propertyOrder: 1,
                    title: t("console.applications.federation.entityProviders.new.remote.cot"),
                    items: {
                        type: "string",
                        "enum": cotIDs,
                        enumNames: cotIDs
                    },
                    type: "array"
                }
            },
            updateType: {
                propertyOrder: 2,
                title: t("console.applications.federation.entityProviders.new.remote.updateType"),
                type: "string",
                "enum": ["CREATE", "UPDATE_CERTIFICATES"]
            }
        },
        required: ["importFile"]
    };
};

const addSuffixes = (providers, suffix) => {
    return map(providers, (provider) => `${provider}|${suffix}`);
};

class NewRemoteEntityProviderContainer extends Component {
    state = {
        fetchError: false,
        isFetching: false,
        isSaving: false,
        cotInstances: []
    };

    async componentDidMount () {
        const [realm] = this.props.router.params;

        /* eslint-disable react/no-did-mount-set-state */
        this.setState({ isFetching: true });

        try {
            const cot = await getAllCOT(realm);

            this.setState({ cotInstances: cot.result });
        } catch (error) {
            this.setState({ fetchError: true });
        } finally {
            this.setState({ isFetching: false });
        }
        /* eslint-enable react/no-did-mount-set-state */
    }

    handleSave = ({ importFile, cot, updateType }) => {
        const location = "remote";
        const [realm] = this.props.router.params;
        const standardMetadata = importFile;

        this.setState({ isSaving: true }, async () => {
            try {
                // request - response
                const response = await create(realm, location, { standardMetadata, updateType });

                if (Array.isArray(cot) && cot.length) {
                    await this.updateCirclesOfTrusts(realm, response.importedEntities, cot);
                }

                Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
                this.routeToListPage(realm);
            } catch (error) {
                if (error instanceof COTServiceError) {
                    Messages.addMessage(
                        { message: t("console.applications.federation.entityProviders.new.remote.addToCOTFailed") }
                    );
                    this.routeToListPage(realm);
                } else {
                    console.error(error);
                }
            } finally {
                this.setState({ isSaving: false });
            }
        });
    };

    routeToListPage = (realm) => {
        Router.routeTo(Router.configuration.routes.realmsApplicationsFederationEntityProviders, {
            args: map([realm, location], encodeURIComponent),
            trigger: true
        });
    };

    updateCirclesOfTrusts = async (realm, entityIds, cotIDs) => {
        const getAllResponse = await getAllCOT(realm);

        for (const cot of getAllResponse.result) {
            if (cotIDs.indexOf(cot._id) > -1) {
                await this.updateCircleOfTrust(cot, entityIds, realm);
            }
        }
    };

    updateCircleOfTrust = async (cot, entityIds, realm) => {
        delete cot._rev;

        cot.trustedProviders = [
            ...new Set([...(addSuffixes(entityIds, "saml2")), ...cot.trustedProviders])
        ];

        const response = await updateCOT(realm, cot, cot._id);
        if (!response.trustedProviders) {
            throw new COTServiceError(response);
        }
    };

    render () {
        if (this.state.fetchError) {
            return <Oops />;
        }

        const content = this.state.isFetching
            ? <Loading />
            : (
                <NewRemoteEntityProvider
                    isCreateDisabled={ this.state.isSaving }
                    onSave={ this.handleSave }
                    schema={ createSchema(map(this.state.cotInstances, "_id")) }
                    { ...this.props }
                />
            );

        return (
            <>
                <PageHeader title={ t("console.applications.federation.entityProviders.new.remote.title") } />
                <Panel>
                    <Panel.Body>
                        {content}
                    </Panel.Body>
                </Panel>
            </>
        );
    }
}

NewRemoteEntityProviderContainer.propTypes = {
    router: withRouterPropType
};

NewRemoteEntityProviderContainer = withRouter(NewRemoteEntityProviderContainer);

export default NewRemoteEntityProviderContainer;
