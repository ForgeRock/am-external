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
 * Copyright 2019 ForgeRock AS.
 */

import { noop } from "lodash";
import PropTypes from "prop-types";
import React, { useCallback, useState } from "react";

import DeleteModalContainer from "./DeleteModalContainer";

const ControlledDeleteModelContainer = ({ onCancel, onConfirm, ...restProps }) => {
    const [show, setShow] = useState(true);

    const handleCancel = useCallback(() => {
        setShow(false);
        onCancel();
    }, [onCancel, setShow]);

    const handleConfirm = useCallback(() => {
        // TODO: Remove wrapping Promise.resolve when jQuery Deferreds are no longer used.
        return Promise.resolve(onConfirm()).finally(() => setShow(false));
    }, [onConfirm, setShow]);

    return (
        <DeleteModalContainer
            onCancel={ handleCancel }
            onConfirm={ handleConfirm }
            show={ show }
            { ...restProps }
        />
    );
};

ControlledDeleteModelContainer.defaultProps = {
    onCancel: noop
};

ControlledDeleteModelContainer.propTypes = {
    onCancel: PropTypes.func,
    onConfirm: PropTypes.func.isRequired
};

export default ControlledDeleteModelContainer;
