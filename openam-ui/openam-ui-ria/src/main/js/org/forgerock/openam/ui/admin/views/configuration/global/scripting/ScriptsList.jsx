/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { ListGroup, ListGroupItem } from "react-bootstrap";
import PropTypes from "prop-types";
import React, { Component } from "react";

import ScriptsService from "org/forgerock/openam/ui/admin/services/global/ScriptsService";

class ScriptsList extends Component {
    constructor (props) {
        super(props);

        this.state = {
            items: []
        };
    }
    componentDidMount () {
        ScriptsService.scripts.getAllDefault(this.props.subSchemaType).then((response) => {
            this.setState({ items: response });
        });
    }
    render () {
        const items = this.state.items.map((item) => (
            <ListGroupItem href={ `#realms/%2F/scripts/edit/${item._id}` } key={ item._id }>
                { item.name }
            </ListGroupItem>)
        );

        return (
            <ListGroup>
                { items }
            </ListGroup>
        );
    }
}

ScriptsList.propTypes = {
    subSchemaType: PropTypes.string.isRequired
};

export default ScriptsList;
