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
 * Copyright 2017-2018 ForgeRock AS.
 */

import { Button, Collapse, FormGroup } from "react-bootstrap";
import { map, isEmpty } from "lodash";
import PropTypes from "prop-types";
import React, { Component } from "react";

import ControlLabelWithIcon from "components/ControlLabelWithIcon";
import HtmlFormattedHelpBlock from "components/HtmlFormattedHelpBlock";

/**
 * Defines inner organization of each field (each form row).
 * @module components/form/fields/VerticalFormFieldTemplate
 * @param {ReactElement} props.children The field or widget component instance for this field row.
 * @param {string} props.id The id of the field in the hierarchy.
 * @param {string} props.label The computed label for this field, as a string.
 * @param {string} props.rawDescription Description of this field.
 * @param {string[]} props.rawErrors Array of errrors.
 * @returns {ReactElement} custom field template
 */
class VerticalFormFieldTemplate extends Component {
    constructor () {
        super();
        this.state = { isOpen: false };
        this.handleIconClick = this.handleIconClick.bind(this);
    }

    handleIconClick () {
        this.setState({ isOpen: !this.state.isOpen });
    }

    render () {
        // Root object needs to not display label or other formatting.
        const isRoot = this.props.id.split("_").length < 2;
        if (isRoot) { return <div>{ this.props.children }</div>; }

        const hasErrors = !isEmpty(this.props.rawErrors);
        const errorList = hasErrors
            ? (
                <ul className="list-unstyled text-danger am-error-detail">
                    { map(this.props.rawErrors, (error) =>
                        <li className="small">{ error }</li>
                    ) }
                </ul>
            )
            : null;

        const icon = isEmpty(this.props.rawDescription)
            ? null
            : (
                <Button
                    bsClass="btn-link"
                    onClick={ this.handleIconClick }
                >
                    <i
                        className={ this.state.isOpen ? "fa fa-times-circle" : "fa fa-info-circle" }
                    />
                </Button>
            );

        const helpBlock = isEmpty(this.props.rawDescription)
            ? null
            : (
                <Collapse in={ this.state.isOpen } >
                    <HtmlFormattedHelpBlock>
                        { this.props.rawDescription }
                    </HtmlFormattedHelpBlock>
                </Collapse>
            );

        return (
            <FormGroup controlId={ this.props.id } validationState={ hasErrors ? "error" : null }>
                <ControlLabelWithIcon icon={ icon }>{ this.props.label } </ControlLabelWithIcon>
                { this.props.children }
                { errorList }
                { helpBlock }
            </FormGroup>
        );
    }
}

VerticalFormFieldTemplate.propTypes = {
    children: PropTypes.objectOf({
        props: PropTypes.objectOf({
            schema: PropTypes.objectOf(PropTypes.any).isRequired
        }).isRequired
    }).isRequired,
    id: PropTypes.string,
    label: PropTypes.string,
    rawDescription: PropTypes.string,
    rawErrors: PropTypes.arrayOf(PropTypes.string)
};

export default VerticalFormFieldTemplate;
