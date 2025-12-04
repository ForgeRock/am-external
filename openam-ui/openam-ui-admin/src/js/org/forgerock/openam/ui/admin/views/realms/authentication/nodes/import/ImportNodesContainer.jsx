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
 * Copyright 2025 Ping Identity Corporation.
 */
import React, { Component } from "react";
import { Panel } from "react-bootstrap";
import { t } from "i18next";

import { importNodes } from "org/forgerock/openam/ui/admin/services/global/authentication/NodeDesignerService";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import ImportNodes from "./ImportNodes";
import Router from "org/forgerock/commons/ui/common/main/Router";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";
import PageHeader from "components/PageHeader";
import Base64 from "org/forgerock/commons/ui/common/util/Base64";

const createSchema = () => {
    return {
        "$schema": "http://json-schema.org/draft-07/schema#",
        type: "object",
        properties: {
            importFile: {
                propertyOrder: 0,
                title: t("console.authentication.nodes.import.importFile"),
                type: "string",
                format: "file",
                acceptedFiles: ".json"
            }
        },
        required: ["importFile"]
    };
};

class ImportNodesContainer extends Component {
    constructor () {
        super();
    }

    state = {
        isImporting: false,
        listNodesLink: ""
    };

    async componentDidMount () {
        /* eslint-disable react/no-did-mount-set-state */
        const [realm] = this.props.router.params;
        this.setState({
            listNodesLink:
                `#${Router.getLink(Router.configuration.routes.realmsAuthenticationNodes, [encodeURIComponent(realm)])}`
        });
        /* eslint-enable react/no-did-mount-set-state */
    }

    handleImport = ({ importFile }) => {
        // File uploader base64 encodes the uploaded file, we just need the json, so decode
        const json = Base64.decodeUTF8(importFile);
        const [realm] = this.props.router.params;
        importNodes(json).then(() => {
            Router.routeTo(Router.configuration.routes.realmsAuthenticationNodes,
                { args: [encodeURIComponent(realm)], trigger: true }
            );
        }).catch((response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER }));
    };

    render () {
        return (
            <>
                <PageHeader title={ t("console.authentication.nodes.import.title") } />
                <Panel>
                    <Panel.Body>
                        <ImportNodes
                            backLink={ this.state.listNodesLink }
                            isImportDisabled={ this.state.isImporting }
                            onImport={ this.handleImport }
                            schema={ createSchema() }
                            { ...this.props }
                        />
                    </Panel.Body>
                </Panel>
            </>
        );
    }
}

ImportNodesContainer.propTypes = {
    router: withRouterPropType
};

ImportNodesContainer = withRouter(ImportNodesContainer);

export default ImportNodesContainer;
