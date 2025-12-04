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
 * Copyright 2016-2025 Ping Identity Corporation.
 */

import { Grid, ListGroup, ListGroupItem, Panel } from "react-bootstrap";
import { t } from "i18next";
import React, { Component } from "react";

import Block from "components/Block";
import PageHeader from "components/PageHeader";
import PageDescription from "components/PageDescription";

import AuthenticationService from "org/forgerock/openam/ui/admin/services/global/AuthenticationService";

export default class ListAuthenticationView extends Component {
    constructor (props) {
        super(props);

        this.state = {
            items: []
        };
    }
    componentDidMount () {
        AuthenticationService.authentication.getAll().then((response) => {
            this.setState({ items: response });
        });
    }
    render () {
        return (
            <Grid>
                <PageHeader title={ t("config.AppConfiguration.Navigation.links.configure.authentication") } />
                <PageDescription>{ t("console.configuration.authentication.description") }</PageDescription>

                <Panel>
                    <Panel.Body>
                        <Block
                            description={ t("console.configuration.authentication.description") }
                            header={ t("console.configuration.authentication.core.title") }
                        >
                            <ListGroup>
                                <ListGroupItem href="#configure/authentication/core">
                                    { t("console.configuration.authentication.core.coreAttributes") }
                                </ListGroupItem>
                            </ListGroup>
                        </Block>
                    </Panel.Body>
                </Panel>
            </Grid>
        );
    }
}
