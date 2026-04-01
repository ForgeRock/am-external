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

import Messages from "org/forgerock/commons/ui/common/components/Messages";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import {
    actionNodeGetUpgradedConfig,
    actionNodeGetVersionInfo
} from "org/forgerock/openam/ui/admin/services/realm/authentication/NodeService";
import Loading from "components/Loading";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

/* eslint-disable react/no-danger */

function UpdateNodeModal ({
    isVisible,
    onClose,
    onUpdateNode,
    updateData,
    router
}) {
    const [isUpdating, setIsUpdating] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [changelog, setChangelog] = useState("");
    const [realm] = router.params;

    async function updateNode () {
        setIsUpdating(true);
        try {
            const data = await actionNodeGetUpgradedConfig(
                realm, updateData.nodeType, updateData.fromVersion, updateData.toVersion, updateData.template
            );
            data.nodeType = updateData.nodeType;
            onUpdateNode(data);
        } catch {
            // If the api call above fails, an error message will handled by
            // by the crest js middleware of the NodeService
        }
        setIsUpdating(false);
        onClose();
    }

    async function initialise () {
        try {
            const data = await actionNodeGetVersionInfo(realm, updateData.nodeType);
            setChangelog(data.find((versionInfo) => versionInfo.version === updateData.toVersion)?.changelog);
        } catch {
            Messages.addMessage({
                message: t("console.authentication.trees.edit.nodes.updateNodeModal.errorLoadingVersionInfo"),
                type: Messages.TYPE_DANGER
            });
        }
        setIsLoading(false);
    }

    const modalBody = () => {
        if (isLoading) {
            return <Loading />;
        } else if (isUpdating) {
            return (
                <Loading loadingText={ t("console.authentication.trees.edit.nodes.updateNodeModal.updatingNode") } />
            );
        } else {
            return (
                <>
                    <h4 className="mt-0">
                        { t("console.authentication.trees.edit.nodes.updateNodeModal.whatsNew") }
                    </h4>
                    <p dangerouslySetInnerHTML={ { __html: changelog } } />
                    <a
                        className="more-info-link"
                        href="https://docs.pingidentity.com/pingam/latest/am-authentication/node-versions.html"
                        rel="noopener noreferrer"
                        target="_blank"
                    >
                        { t("console.authentication.trees.edit.nodes.updateNodeModal.moreInfo") }
                        <i className="fa fa-external-link ml-2" />
                    </a>
                </>
            );
        }
    };

    return (
        <Modal
            backdrop="static"
            className="update-node-modal"
            onEnter={ initialise }
            onHide={ onClose }
            show={ isVisible }
        >
            <Modal.Header closeButton>
                <Modal.Title>
                    { t("console.authentication.trees.edit.nodes.updateNodeModal.updateNodeVersion") }
                </Modal.Title>
            </Modal.Header>
            <Modal.Body>
                { modalBody() }
            </Modal.Body>
            <Modal.Footer>
                <Clearfix>
                    <div className="pull-right">
                        <div className="am-btn-action-group">
                            <Button onClick={ onClose }>
                                { t("common.form.cancel") }
                            </Button>
                            {
                                !isUpdating &&
                                <Button
                                    bsStyle="primary"
                                    onClick={ updateNode }
                                >
                                    { t("common.form.update") }
                                </Button>
                            }
                        </div>
                    </div>
                </Clearfix>
            </Modal.Footer>
        </Modal>
    );
}

UpdateNodeModal.propTypes = {
    isVisible: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    onUpdateNode: PropTypes.func.isRequired,
    router: withRouterPropType,
    updateData: PropTypes.objectOf(PropTypes.any)
};

export default withRouter(UpdateNodeModal);
