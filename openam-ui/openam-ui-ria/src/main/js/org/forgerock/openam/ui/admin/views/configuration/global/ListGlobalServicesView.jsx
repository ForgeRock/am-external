/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { Grid, ListGroup, ListGroupItem } from "react-bootstrap";
import { t } from "i18next";
import React, { Component } from "react";

import PageHeader from "components/PageHeader";
import PageDescription from "components/PageDescription";
import ServicesService from "org/forgerock/openam/ui/admin/services/global/ServicesService";

class ListGlobalServicesView extends Component {
    constructor (props) {
        super(props);
        this.state = {
            items: []
        };
    }
    componentDidMount () {
        ServicesService.instance.getAll().then((response) => {
            this.setState({ items: response });
        });
    }
    render () {
        const items = this.state.items.map((item) => (
            <ListGroupItem href={ `#configure/global-services/${encodeURIComponent(item._id)}` } key={ item._id }>
                { item.name }
            </ListGroupItem>)
        );

        return (
            <Grid>
                <PageHeader title={ t("config.AppConfiguration.Navigation.links.configure.global-services") } />
                <PageDescription>{ t("console.configuration.globalServices.description") }</PageDescription>

                <ListGroup>
                    { items }
                </ListGroup>
            </Grid>
        );
    }
}

export default ListGlobalServicesView;
