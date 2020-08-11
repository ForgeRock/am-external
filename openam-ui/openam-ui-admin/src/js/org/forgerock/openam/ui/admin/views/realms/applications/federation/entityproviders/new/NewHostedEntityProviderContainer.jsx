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
 * Copyright 2019-2020 ForgeRock AS.
 */
import { bindActionCreators } from "redux";
import { cloneDeep, endsWith, get, map } from "lodash";
import { Panel } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component } from "react";

import {
    COTServiceError,
    getAll as getAllCOT,
    update as updateCOT
} from "org/forgerock/openam/ui/admin/services/realm/federation/CirclesOfTrustService";
import {
    create,
    get as getInstance,
    getSchema,
    update
} from "org/forgerock/openam/ui/admin/services/realm/federation/SAML2EntityProviderService";
import {
    getDefaultUrl,
    replaceIdpPlaceHolders,
    replaceSpPlaceHolders
} from "components/form/schema/replaceSamlSchemaPlaceHolders";
import getDefaultValues from "components/form/schema/getDefaultValues";
import { set as setInstance } from "store/modules/remote/config/realm/applications/federation/entityproviders/instance";
import { set as setSchema } from "store/modules/remote/config/realm/applications/federation/entityproviders/schema";
import connectWithStore from "components/redux/connectWithStore";
import Loading from "components/Loading";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import NewHostedEntityProvider from "./NewHostedEntityProvider";
import Oops from "components/Oops";
import PageHeader from "components/PageHeader";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const createSchema = (id, baseUrl, metaAliasPrefix, idp, sp, cotIDs) => {
    const required = ["entityId", "baseUrl"];
    const requiredMetaAliases = [];
    if (idp || sp) {
        requiredMetaAliases.push("sp", "idp");
    }
    return {
        "$schema": "http://json-schema.org/draft-07/schema#",
        type: "object",
        properties: {
            entityId: {
                "default": id.length ? id : undefined,
                propertyOrder: 0,
                readOnly: !!id.length,
                title: t("console.applications.federation.entityProviders.new.hosted.entityId"),
                type: "string"
            },
            baseUrl: {
                "default": getDefaultUrl(),
                description: t("console.applications.federation.entityProviders.new.hosted.baseUrl.description"),
                propertyOrder: 1,
                readOnly: !!baseUrl.length,
                title: t("console.applications.federation.entityProviders.new.hosted.baseUrl.title"),
                type: "string"
            },
            metaAliases: {
                description: t("console.applications.federation.entityProviders.new.hosted.metaAliases.description"),
                title: "Meta Aliases",
                type: "object",
                propertyOrder: 2,
                properties: {
                    idp: {
                        "default": idp.length ? idp : undefined,
                        description: t("console.applications.federation.entityProviders.new.hosted.metaAliases.idp" +
                            ".description"),
                        prefix: idp.length ? null : metaAliasPrefix,
                        propertyOrder: 0,
                        readOnly: !!idp.length,
                        title: t("console.applications.federation.entityProviders.new.hosted.metaAliases.idp.title"),
                        type: "string"
                    },
                    sp: {
                        "default": sp.length ? sp : undefined,
                        description: t("console.applications.federation.entityProviders.new.hosted.metaAliases.sp" +
                            ".description"),
                        prefix: sp.length ? null : metaAliasPrefix,
                        propertyOrder: 1,
                        readOnly: !!sp.length,
                        title: t("console.applications.federation.entityProviders.new.hosted.metaAliases.sp.title"),
                        type: "string"
                    }
                },
                required: requiredMetaAliases
            },
            ...cotIDs.length && { cot: {
                propertyOrder: 3,
                title: t("console.applications.federation.entityProviders.new.hosted.cot"),
                items: {
                    type: "string",
                    "enum": cotIDs,
                    enumNames: cotIDs
                },
                type: "array"
            } }
        },
        required
    };
};

class NewHostedEntityProviderContainer extends Component {
    state = {
        fetchError: false,
        hasAllRoles: false,
        idpMetaAlias: "",
        isFetching: false,
        isSaving: false,
        spMetaAlias: "",
        cotInstances: [],
        baseUrl: ""
    };

    async componentDidMount () {
        const [realm, id] = this.props.router.params;

        /* eslint-disable react/no-did-mount-set-state */
        this.setState({ isFetching: true });

        try {
            const [cot, schema] = await Promise.all([
                getAllCOT(realm),
                getSchema(realm, "hosted")
            ]);

            this.setState({ cotInstances: cot.result });

            if (id) {
                const instance = await getInstance(realm, "hosted", id);
                const idpMetaAlias = get(instance, "identityProvider.services.metaAlias", "");
                const spMetaAlias = get(instance, "serviceProvider.services.metaAlias", "");
                const baseUrl = "";
                const hasAllRoles = !!idpMetaAlias && !!spMetaAlias;

                this.setState({
                    hasAllRoles,
                    idpMetaAlias,
                    spMetaAlias,
                    baseUrl
                });
                this.props.setInstance(instance);
            }

            this.props.setSchema(schema);
        } catch (error) {
            this.setState({ fetchError: true });
        } finally {
            this.setState({ isFetching: false });
        }
        /* eslint-enable react/no-did-mount-set-state */
    }

    validate ({ metaAliases }, errors) {
        if (!metaAliases.idp && !metaAliases.sp) {
            const atLeastOne = t("console.applications.federation.entityProviders.new.hosted.validation.atLeastOne");
            errors.metaAliases.idp.addError(atLeastOne);
            errors.metaAliases.sp.addError(atLeastOne);
        } else if (metaAliases.idp === metaAliases.sp) {
            errors.sp.addError(t("console.applications.federation.entityProviders.new.hosted.validation.duplicate"));
        }
        const whitespace = /\s+/;
        if (whitespace.test(metaAliases.idp)) {
            errors.metaAliases.idp.addError(
                t("console.applications.federation.entityProviders.new.hosted.validation.whitespace"));
        }
        if (whitespace.test(metaAliases.sp)) {
            errors.metaAliases.sp.addError(
                t("console.applications.federation.entityProviders.new.hosted.validation.whitespace"));
        }

        return errors;
    }

    handleSave = ({ entityId, baseUrl, metaAliases, cot }) => {
        const [realm, id] = this.props.router.params;

        this.setState({ isSaving: true }, async () => {
            const instance = id ? cloneDeep(this.props.instance) : {};
            const completeSchema = this.props.completeSchema;
            const metaAliasPrefix = endsWith(realm, "/") ? realm : `${realm}/`;
            let role;
            if (metaAliases.idp && !instance.identityProvider) {
                role = "identityProvider";
                instance.identityProvider = getDefaultValues(completeSchema.properties.identityProvider);
                instance.identityProvider = replaceIdpPlaceHolders(instance.identityProvider, baseUrl,
                    `${metaAliasPrefix}${metaAliases.idp}`);
            }
            if (metaAliases.sp && !instance.serviceProvider) {
                role = role || "serviceProvider";
                instance.serviceProvider = getDefaultValues(completeSchema.properties.serviceProvider);
                instance.serviceProvider = replaceSpPlaceHolders(instance.serviceProvider, baseUrl,
                    `${metaAliasPrefix}${metaAliases.sp}`);
            }
            instance.entityId = entityId;

            try {
                if (id) {
                    await update(realm, "hosted", entityId, instance);
                } else {
                    await create(realm, "hosted", instance);
                    if (Array.isArray(cot) && cot.length) {
                        await this.updateCirclesOfTrusts(realm, entityId, cot);
                    }
                }

                Messages.addMessage({ message: t("config.messages.CommonMessages.changesSaved") });
                this.routeToEditPage(realm, entityId, role);
            } catch (error) {
                if (error instanceof COTServiceError) {
                    Messages.addMessage(
                        { message: t("console.applications.federation.entityProviders.new.remote.addToCOTFailed") }
                    );
                    this.routeToEditPage(realm, entityId, role);
                } else {
                    console.error(error);
                }
            } finally {
                this.setState({ isSaving: false });
            }
        });
    };

    routeToEditPage = (realm, entityId, role) => {
        Router.routeTo(Router.configuration.routes.realmsApplicationsFederationEntityProvidersEdit, {
            args: map([realm, "hosted", role, entityId], encodeURIComponent), trigger: true
        });
    };

    updateCirclesOfTrusts = async (realm, entityId, cotIDs) => {
        const getAllResponse = await getAllCOT(realm);

        for (const cot of getAllResponse.result) {
            if (cotIDs.indexOf(cot._id) > -1) {
                await this.updateCircleOfTrust(cot, entityId, realm);
            }
        }
    };

    updateCircleOfTrust = async (cot, entityId, realm) => {
        delete cot._rev;
        cot.trustedProviders.push(`${entityId}|saml2`);

        const response = await updateCOT(realm, cot, cot._id);
        if (!response.trustedProviders) {
            throw new COTServiceError(response);
        }
    };

    render () {
        const [realm, id] = this.props.router.params;
        const metaAliasPrefix = endsWith(realm, "/") ? realm : `${realm}/`;
        if (this.state.fetchError) {
            return <Oops />;
        }

        const content = this.state.isFetching
            ? <Loading />
            : (
                <NewHostedEntityProvider
                    isCreateDisabled={ this.state.isSaving || this.state.hasAllRoles }
                    onSave={ this.handleSave }
                    schema={ createSchema(id, this.state.baseUrl, metaAliasPrefix, this.state.idpMetaAlias,
                        this.state.spMetaAlias, map(this.state.cotInstances, "_id")) }
                    validate={ this.validate }
                    { ...this.props }
                />
            );

        return (
            <>
                <PageHeader title={ t("console.applications.federation.entityProviders.new.hosted.title") } />
                <Panel>
                    <Panel.Body>
                        { content }
                    </Panel.Body>
                </Panel>
            </>
        );
    }
}

NewHostedEntityProviderContainer.propTypes = {
    completeSchema: PropTypes.objectOf(PropTypes.any),
    instance: PropTypes.objectOf(PropTypes.any),
    router: withRouterPropType,
    setInstance: PropTypes.func.isRequired,
    setSchema: PropTypes.func.isRequired
};

NewHostedEntityProviderContainer = connectWithStore(NewHostedEntityProviderContainer,
    (state) => {
        return {
            instance: state.remote.config.realm.applications.federation.entityproviders.instance,
            completeSchema: state.remote.config.realm.applications.federation.entityproviders.schema
        };
    },
    (dispatch) => ({
        setInstance: bindActionCreators(setInstance, dispatch),
        setSchema: bindActionCreators(setSchema, dispatch)
    })
);

NewHostedEntityProviderContainer = withRouter(NewHostedEntityProviderContainer);

export default NewHostedEntityProviderContainer;
