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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { Button, Col, Modal, Row } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React from "react";

const DeleteModal = ({ children, disabled, inProgress, names, objectName, onCancel, onConfirm, onExited, show }) => {
    const content = inProgress
        ? (
            <Modal.Header className="text-center">
                <span className="fa-stack fa-lg fa-2x text-danger">
                    <i className="fa fa-circle fa-stack-2x" />
                    <i className="fa fa-refresh fa-spin fa-stack-1x fa-inverse" />
                </span>
                <Modal.Title>{ t("components.dialogs.delete.titleInProgress") }</Modal.Title>
            </Modal.Header>
        )
        : (
            <>
                <Modal.Header className="text-center" closeButton={ !disabled } onHide={ onCancel }>
                    <span className="fa-stack fa-lg fa-2x text-danger">
                        <i className="fa fa-circle fa-stack-2x" />
                        <i className="fa fa-trash fa-stack-1x fa-inverse" />
                    </span>
                    <Modal.Title>{ t("components.dialogs.delete.title", { objectName }) }</Modal.Title>
                </Modal.Header>

                <Modal.Body>
                    <p
                        className="text-center wordwrap"
                        dangerouslySetInnerHTML={ { // eslint-disable-line react/no-danger
                            __html: t("components.dialogs.delete.message_interval", {
                                count: names.length,
                                names: names.join(", "),
                                objectName,
                                postProcess: "interval"
                            })
                        } }
                    />
                    { children }
                </Modal.Body>

                <Modal.Footer>
                    <Row>
                        <Col md={ 6 }>
                            <Button block className="fr-btn-secondary" disabled={ disabled } onClick={ onCancel }>
                                { t("components.dialogs.delete.no", { objectName }) }
                            </Button>
                        </Col>
                        <Col md={ 6 }>
                            <Button block bsStyle="danger" disabled={ disabled } onClick={ onConfirm }>
                                { t("components.dialogs.delete.yes", { objectName }) }
                            </Button>
                        </Col>
                    </Row>
                </Modal.Footer>
            </>
        );

    return (
        <Modal animation backdrop="static" dialogClassName="dialog-danger" onExited={ onExited } show={ show }>
            { content }
        </Modal>
    );
};

DeleteModal.propTypes = {
    children: PropTypes.node,
    disabled: PropTypes.bool,
    inProgress: PropTypes.bool,
    names: PropTypes.arrayOf(PropTypes.string).isRequired,
    objectName: PropTypes.string.isRequired,
    onCancel: PropTypes.func,
    onConfirm: PropTypes.func,
    onExited: PropTypes.func,
    show: PropTypes.bool
};

export default DeleteModal;
