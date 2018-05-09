/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
        const items = this.state.items.map((item) => (
            <ListGroupItem href={ `#configure/authentication/${item._id}` } key={ item._id }>
                { item.name }
            </ListGroupItem>)
        );

        return (
            <Grid>
                <PageHeader title={ t("config.AppConfiguration.Navigation.links.configure.authentication") } />
                <PageDescription>{ t("console.configuration.authentication.description") }</PageDescription>

                <Panel>
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

                    <Block
                        description={ t("console.configuration.authentication.modules.title") }
                        header={ t("console.configuration.authentication.modules.title") }
                    >
                        <ListGroup>
                            { items }
                        </ListGroup>
                    </Block>
                </Panel>
            </Grid>
        );
    }
}
