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
 * Copyright 2026 Ping Identity Corporation.
 */

import { Button, Clearfix, Modal } from "react-bootstrap";
import { t } from "i18next";
import PropTypes from "prop-types";
import React, { useState } from "react";

import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import { getNodeTypeTemplate } from "org/forgerock/openam/ui/admin/services/realm/authentication/NodeService";
import Loading from "components/Loading";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

/* eslint-disable react/no-danger */

function RevertNodeModal ({
    isVisible,
    onClose,
    onRevertNode,
    updateData,
    router
}) {
    const [isReverting, setIsReverting] = useState(false);
    const [realm] = router.params;

    async function revertNode () {
        setIsReverting(true);
        try {
            const data = await getNodeTypeTemplate(realm, updateData.nodeType, updateData.toVersion);
            data.nodeType = updateData.nodeType;
            data._id = updateData.nodeId;
            onRevertNode(data);
        } catch {
            // If the api call above fails, an error message will handled by
            // by the crest js middleware of the NodeService
        }
        onClose();
        setIsReverting(false);
    }

    return (
        <Modal
            backdrop="static"
            onHide={ onClose }
            show={ isVisible }
        >
            <Modal.Header closeButton>
                <Modal.Title>
                    { t("console.authentication.trees.edit.nodes.revertNodeModal.title") }
                </Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {
                    isReverting
                        ? <Loading loadingText={ t("console.authentication.trees.edit.nodes.revertNodeModal.revertingNode") }/> // eslint-disable-line
                        : <p dangerouslySetInnerHTML={ { __html: t("console.authentication.trees.edit.nodes.revertNodeModal.desc", { version: updateData.toVersion })} } /> // eslint-disable-line
                }
            </Modal.Body>
            <Modal.Footer>
                <Clearfix>
                    <div className="pull-right">
                        <div className="am-btn-action-group">
                            <Button
                                className="fr-btn-secondary"
                                onClick={ onClose }
                            >
                                { t("common.form.cancel") }
                            </Button>
                            {
                                !isReverting &&
                                <Button
                                    bsStyle="danger"
                                    onClick={ revertNode }
                                >
                                    { t("common.form.revert") }
                                </Button>
                            }
                        </div>
                    </div>
                </Clearfix>
            </Modal.Footer>
        </Modal>
    );
}

RevertNodeModal.propTypes = {
    isVisible: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    onRevertNode: PropTypes.func.isRequired,
    router: withRouterPropType,
    updateData: PropTypes.objectOf(PropTypes.any)
};

export default withRouter(RevertNodeModal);
