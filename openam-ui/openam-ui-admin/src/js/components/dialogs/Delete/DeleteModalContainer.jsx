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

import { delay, noop } from "lodash";
import { t } from "i18next";
import ms from "ms";
import PropTypes from "prop-types";
import React, { useCallback, useState } from "react";

import DeleteModal from "./DeleteModal";

const PERIOD_BEFORE_IN_PROGRESS_SHOWN = ms("0.5s");

const DeleteModalContainer = ({ children, names, onCancel, onConfirm, onExited, objectName, show }) => {
    const [disabled, setDisabled] = useState(false);
    const [inProgress, setInProgress] = useState(false);

    const handleConfirm = useCallback(() => {
        setDisabled(true);

        const timerId = delay(() => setInProgress(true), PERIOD_BEFORE_IN_PROGRESS_SHOWN);

        onConfirm().finally(() => {
            clearTimeout(timerId);
            setDisabled(false);
        });
    }, [onConfirm, setDisabled, setInProgress]);

    const handleExited = useCallback(() => {
        setInProgress(false);
        onExited();
    }, [onExited, setInProgress]);

    return (
        <DeleteModal
            disabled={ disabled }
            inProgress={ inProgress }
            names={ names }
            objectName={ t(`objects.${objectName}`, { count: names.length }) }
            onCancel={ onCancel }
            onConfirm={ handleConfirm }
            onExited={ handleExited }
            show={ show }
        >
            { children }
        </DeleteModal>
    );
};

DeleteModalContainer.defaultProps = {
    onExited: noop
};

DeleteModalContainer.propTypes = {
    children: PropTypes.node,
    names: PropTypes.arrayOf(PropTypes.string).isRequired,
    objectName: PropTypes.string.isRequired,
    onCancel: PropTypes.func,
    onConfirm: PropTypes.func.isRequired,
    onExited: PropTypes.func,
    show: PropTypes.bool
};

export default DeleteModalContainer;
