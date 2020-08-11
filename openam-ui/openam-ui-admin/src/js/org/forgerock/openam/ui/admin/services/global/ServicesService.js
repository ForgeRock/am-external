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
 * Copyright 2016-2019 ForgeRock AS.
 */

import _ from "lodash";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";

/**
 * @module org/forgerock/openam/ui/admin/services/global/ServicesService
 */
const ServicesService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

const getServiceSchema = function (type, options) {
    return ServicesService.serviceCall(_.merge({
        url: fetchUrl(`/global-config/services/${type}?_action=schema`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST"
    }, options)).then((response) => {
        return new JSONSchema(response);
    });
};
const getServiceSubSchema = function (serviceType, subSchemaType) {
    return ServicesService.serviceCall({
        url: fetchUrl(
            `/global-config/services/${serviceType}/${subSchemaType}?_action=schema`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST"
    }).then((response) => new JSONSchema(response));
};
const getServiceSubSubSchema = function (serviceType, subSchemaType, subSchemaInstance, subSubSchemaType) {
    return ServicesService.serviceCall({
        url: fetchUrl(
            `/global-config/services/${serviceType}/${subSchemaType}/${subSchemaInstance}/${subSubSchemaType}` +
            "?_action=schema",
            { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST"
    }).then((response) => new JSONSchema(response));
};

ServicesService.instance = {
    getAll () { // TODO this is the only difference in GLOBAL and REALM service rest calls
        return ServicesService.serviceCall({
            url: fetchUrl("/global-config/services?_action=nextdescendents&forUI=true", { realm: false }),
            type: "POST",
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then((response) =>
            _(response.result).map((item) => {
                item["name"] = item._type.name;
                return item;
            }).sortBy("name").value()
        );
    },
    get (type, options) {
        const getInstance = () => ServicesService.serviceCall(_.merge({
            url: fetchUrl(`/global-config/services/${type}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }, options));

        return Promise.all([getServiceSchema(type, options), getInstance()]).then((response) => ({
            name: response[1]._type.name,
            schema: response[0],
            values: new JSONValues(response[1])
        }));
    },
    getInitialState (type) {
        function getTemplate () {
            return ServicesService.serviceCall({
                url: fetchUrl(`/global-config/services/${type}?_action=template`, { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }).then((response) => new JSONValues(response));
        }

        return Promise.all([getServiceSchema(type), getTemplate()]).then((response) => ({
            schema: response[0],
            values: response[1]
        }));
    },
    update (type, data) {
        return ServicesService.serviceCall({
            url: fetchUrl(`/global-config/services/${type}`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "PUT",
            data
        }).then((response) => new JSONValues(response));
    },
    create (type, data) {
        return ServicesService.serviceCall({
            url: fetchUrl(`/global-config/services/${type}?_action=create`, { realm: false }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data: JSON.stringify(data)
        });
    }
};

ServicesService.type = {
    subSchema: {
        type: {
            getAll (serviceType) {
                return ServicesService.serviceCall({
                    url: fetchUrl(
                        `/global-config/services/${serviceType}?_action=getAllTypes`, { realm: false }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                });
            },
            getCreatables (serviceType) {
                return ServicesService.serviceCall({
                    url: fetchUrl(
                        `/global-config/services/${serviceType}?_action=getCreatableTypes&forUI=true`,
                        { realm: false }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                });
            },
            subSchema: {
                type: {
                    getAll (serviceType, subSchemaType) {
                        return ServicesService.serviceCall({
                            url: fetchUrl(
                                `/global-config/services/${serviceType}/${subSchemaType}?_action=getAllTypes`,
                                { realm: false }),
                            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                            type: "POST"
                        });
                    },
                    getCreatables (serviceType, subSchemaType, subSchemaInstance) {
                        return ServicesService.serviceCall({
                            url: fetchUrl(
                                `/global-config/services/${serviceType}/${subSchemaType}/${subSchemaInstance}` +
                                "?_action=getCreatableTypes&forUI=true",
                                { realm: false }),
                            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                            type: "POST"
                        });
                    }
                },
                instance: {
                    getAll (serviceType, subSchemaType, subSchemaInstance) {
                        return ServicesService.serviceCall({
                            url: fetchUrl(
                                `/global-config/services/${serviceType}/${subSchemaType}/${subSchemaInstance}` +
                                "?_action=nextdescendents",
                                { realm: false }),
                            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                            type: "POST"
                        });
                    },
                    get (serviceType, subSchemaType, subSchemaInstance, subSubSchemaType) {
                        function getInstance () {
                            return ServicesService.serviceCall({
                                url: fetchUrl(
                                    "/global-config/services/" +
                                    `${serviceType}/${subSchemaType}/${subSchemaInstance}/${subSubSchemaType}`,
                                    { realm: false }),
                                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                            }).then((response) => new JSONValues(response));
                        }

                        return Promise.all([getServiceSubSubSchema(serviceType, subSchemaType, subSchemaInstance,
                            subSubSchemaType), getInstance()])
                            .then((response) => ({
                                schema: response[0],
                                values: response[1]
                            }));
                    },
                    update (serviceType, subSchemaType, subSchemaInstance, subSubSchemaType, data) {
                        return ServicesService.serviceCall({
                            url: fetchUrl(
                                "/global-config/services/" +
                                `${serviceType}/${subSchemaType}/${subSchemaInstance}/${subSubSchemaType}`,
                                { realm: false }),
                            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                            type: "PUT",
                            data
                        });
                    }
                }
            }
        },
        instance: {
            getAll (serviceType) {
                return ServicesService.serviceCall({
                    url: fetchUrl(
                        `/global-config/services/${serviceType}?_action=nextdescendents&forUI=true`,
                        { realm: false }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                });
            },
            get (serviceType, subSchemaType, subSchemaInstance) {
                function getInstance () {
                    return ServicesService.serviceCall({
                        url: fetchUrl(
                            `/global-config/services/${serviceType}/${subSchemaType}/${subSchemaInstance}`,
                            { realm: false }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    }).then((response) => new JSONValues(response));
                }

                return Promise.all([getServiceSubSchema(serviceType, subSchemaType), getInstance()])
                    .then((response) => ({
                        schema: response[0],
                        values: response[1]
                    }));
            },

            getInitialState (serviceType, subSchemaType) {
                function getTemplate (serviceType, subSchemaType) {
                    return ServicesService.serviceCall({
                        url: fetchUrl(
                            `/global-config/services/${serviceType}/${subSchemaType}?_action=template`,
                            { realm: false }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "POST"
                    }).then((response) => new JSONValues(response));
                }

                return Promise.all([
                    getServiceSubSchema(serviceType, subSchemaType),
                    getTemplate(serviceType, subSchemaType)
                ]).then((response) => ({
                    schema: response[0],
                    values: response[1]
                }));
            },

            remove (serviceType, subSchemaType, subSchemaInstance) {
                return ServicesService.serviceCall({
                    url: fetchUrl(
                        `/global-config/services/${serviceType}/${subSchemaType}/${subSchemaInstance}`,
                        { realm: false }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "DELETE",
                    suppressSpinner: true
                });
            },

            update (serviceType, subSchemaType, subSchemaInstance, data) {
                return ServicesService.serviceCall({
                    url: fetchUrl(
                        `/global-config/services/${serviceType}/${subSchemaType}/${subSchemaInstance}`,
                        { realm: false }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "PUT",
                    data
                });
            },

            create (serviceType, subSchemaType, data) {
                return ServicesService.serviceCall({
                    url: fetchUrl(
                        `/global-config/services/${serviceType}/${subSchemaType}?_action=create`, { realm: false }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST",
                    data: JSON.stringify(data)
                });
            }
        }
    }
};

export default ServicesService;
