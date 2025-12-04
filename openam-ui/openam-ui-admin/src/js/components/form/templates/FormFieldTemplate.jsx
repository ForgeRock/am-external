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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

import { Button, Clearfix, Col, Collapse, ControlLabel, FormGroup } from "react-bootstrap";
import { get, map, isEmpty } from "lodash";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { Component, Fragment } from "react";

import ControlLabelWithIcon from "components/ControlLabelWithIcon";
import HtmlFormattedHelpBlock from "components/HtmlFormattedHelpBlock";
import PanelCollapse from "./panel/PanelCollapse";

/**
 * Defines inner organization of each field (each form row).
 */
class FormFieldTemplate extends Component {
    /**
     * @param {object} props Component properties.
     * @param {ReactElement} props.children The field or widget component instance for this field row.
     * @param {boolean} props.horizontal The desired layout direction.
     * @param {string} props.id The id of the field in the hierarchy.
     * @param {string} props.label The computed label for this field, as a string.
     * @param {string} props.rawDescription Description of this field.
     * @param {string[]} props.rawErrors Array of errors.
     * @param {object} props.schema The schema for the property
     * @returns {ReactElement} custom field template
     */
    static propTypes = {
        children: PropTypes.node.isRequired,
        horizontal: PropTypes.bool.isRequired,
        id: PropTypes.string,
        label: PropTypes.string,
        rawDescription: PropTypes.string,
        rawErrors: PropTypes.arrayOf(PropTypes.string),
        required: PropTypes.bool,
        schema: PropTypes.objectOf(PropTypes.any).isRequired
    };
    state = {
        isDescriptionOpen: false
    };

    handleIconClick = () => {
        this.setState({
            isDescriptionOpen: !this.state.isDescriptionOpen
        });
    };

    render () {
        if (this.props.schema.type === "object" && this.props.schema.properties) {
            const isRoot = this.props.id.split("_").length < 2;
            const disableCollapsible = get(this.props, ("uiSchema.ui:options.disableCollapsible")) === true;
            if (isRoot || disableCollapsible) {
                return this.props.children;
            } else {
                return <PanelCollapse { ...this.props } />;
            }
        }

        const optional = this.props.required
            ? null
            : <span> - <i className="small">{ t("common.form.optional") }</i></span>;

        const hasErrors = !isEmpty(this.props.rawErrors);
        const errorList = hasErrors
            ? (
                <ul className="list-unstyled text-danger help-block">
                    { map(this.props.rawErrors, (error) =>
                        <li className="small" key={ error }>{ error }</li>
                    ) }
                </ul>
            )
            : null;

        const icon = isEmpty(this.props.rawDescription)
            ? null
            : (
                <Button
                    bsStyle="link"
                    onClick={ this.handleIconClick }
                >
                    <i
                        className={ this.state.isDescriptionOpen ? "fa fa-times-circle" : "fa fa-info-circle" }
                    />
                </Button>
            );

        const helpBlock = isEmpty(this.props.rawDescription)
            ? null
            : (
                <Collapse in={ this.state.isDescriptionOpen } >
                    <HtmlFormattedHelpBlock>
                        { this.props.rawDescription }
                    </HtmlFormattedHelpBlock>
                </Collapse>
            );

        const contents = this.props.horizontal
            ? (
                <Clearfix>
                    <Col className="wordwrap" componentClass={ ControlLabel } sm={ 4 } >
                        { this.props.label }{ optional }
                    </Col>
                    <Col sm={ 6 }>
                        { this.props.children }
                        { errorList }
                        { helpBlock }
                    </Col>
                    { icon }
                </Clearfix>
            )
            : (
                <Fragment>
                    <ControlLabelWithIcon icon={ icon }>
                        { this.props.label }{ optional }
                    </ControlLabelWithIcon>
                    { this.props.children }
                    { errorList }
                    { helpBlock }
                </Fragment>
            );

        return (
            <FormGroup controlId={ this.props.id } validationState={ hasErrors ? "error" : null }>
                { contents }
            </FormGroup>
        );
    }
}

export default FormFieldTemplate;
