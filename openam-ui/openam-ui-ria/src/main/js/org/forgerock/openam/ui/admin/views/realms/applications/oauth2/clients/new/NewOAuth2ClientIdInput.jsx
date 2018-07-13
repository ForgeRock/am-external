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
 * Copyright 2017 ForgeRock AS.
 */

import { Col, ControlLabel, FormControl, Form, FormGroup } from "react-bootstrap";
import { t } from "i18next";
import { uniqueId } from "lodash";
import React, { PropTypes } from "react";

const NewOAuth2ClientIdInput = ({ clientId, onClientIdChange }) => {
    const handleClientIdChange = (event) => onClientIdChange(event.target.value);

    return (
        <Form horizontal>
            <FormGroup controlId={ uniqueId("clientId") }>
                <ControlLabel className="col-sm-4">
                    { t("console.applications.oauth2.clients.new.clientId") }
                </ControlLabel>
                <Col sm={ 6 }>
                    <FormControl
                        onChange={ handleClientIdChange }
                        type="text"
                        value={ clientId }
                    />
                </Col>
            </FormGroup>
        </Form>
    );
};

NewOAuth2ClientIdInput.propTypes = {
    clientId: PropTypes.string.isRequired,
    onClientIdChange: PropTypes.func.isRequired
};

export default NewOAuth2ClientIdInput;
