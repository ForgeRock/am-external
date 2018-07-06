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
 * Copyright 2016-2017 ForgeRock AS.
 */

import _ from "lodash";
import { Button, Col, ControlLabel, Grid, FormControl, FormGroup, Modal } from "react-bootstrap";
import { t } from "i18next";
import Checkbox from "components/Checkbox";
import React, { Component, PropTypes } from "react";

class PolicySettingsModal extends Component {
    constructor (props) {
        super(props);

        this.handleCheckBoxChange = this.handleCheckBoxChange.bind(this);
        this.handleClose = this.handleClose.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleTextFieldChange = this.handleTextFieldChange.bind(this);

        this.state = {
            showModal: true,
            ...props.data
        };
    }

    componentWillReceiveProps (nextProps) {
        this.setState({
            showModal: true,
            ...nextProps.data
        });
    }

    handleCheckBoxChange (e) {
        this.setState({ active: e.target.checked });
    }

    handleClose () {
        this.setState({ showModal: false });
    }

    handleSubmit () {
        this.setState({ showModal: false });
        this.props.handleSubmit(_.pick(this.state, ["active", "name", "description"]));
    }

    handleTextFieldChange (e) {
        this.setState({ [e.target.getAttribute("data-entity-prop")]: e.target.value });
    }

    render () {
        return (
            <Modal onExit={ this.handleOnExit } onHide={ this.handleClose } show={ this.state.showModal }>
                <Modal.Header closeButton>
                    <Modal.Title>{ t("common.form.editDetails") }</Modal.Title>
                </Modal.Header>

                <Modal.Body>
                    <form className="form-horizontal">
                        <Grid bsClass="">
                            <FormGroup className="clearfix" controlId="policyName">
                                <Col sm={ 2 }>
                                    <ControlLabel>
                                        { t("console.common.name") }
                                    </ControlLabel>
                                </Col>
                                <Col sm={ 9 }>
                                    <FormControl
                                        data-entity-prop="name"
                                        onChange={ this.handleTextFieldChange }
                                        placeholder={ t("common.form.validation.required") }
                                        type="text"
                                        value={ this.state.name }
                                    />
                                </Col>
                            </FormGroup>

                            <FormGroup className="clearfix" controlId="policyDescription">
                                <Col sm={ 2 }>
                                    <ControlLabel>
                                        { t("console.common.description") }
                                    </ControlLabel>
                                </Col>
                                <Col sm={ 9 }>
                                    <FormControl
                                        componentClass="textarea"
                                        data-entity-prop="description"
                                        onChange={ this.handleTextFieldChange }
                                        value={ this.state.description }
                                    />
                                </Col>
                            </FormGroup>

                            <FormGroup className="clearfix">
                                <Col sm={ 2 }>
                                    <ControlLabel htmlFor="policyActive">
                                        { t("common.user.active") }
                                    </ControlLabel>
                                </Col>
                                <Col sm={ 9 }>
                                    <Checkbox
                                        checked={ this.state.active }
                                        id="policyActive"
                                        onChange={ this.handleCheckBoxChange }
                                    />
                                </Col>
                            </FormGroup>
                        </Grid>
                    </form>
                </Modal.Body>

                <Modal.Footer>
                    <Button onClick={ this.handleClose }>{ t("common.form.close") }</Button>
                    <Button bsStyle="primary" onClick={ this.handleSubmit }>{ t("common.form.saveChanges") }</Button>
                </Modal.Footer>
            </Modal>
        );
    }
}

PolicySettingsModal.propTypes = {
    data: React.PropTypes.objectOf({
        active: PropTypes.bool.isRequired,
        description: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired
    }),
    handleSubmit: PropTypes.func.isRequired
};

export default PolicySettingsModal;
