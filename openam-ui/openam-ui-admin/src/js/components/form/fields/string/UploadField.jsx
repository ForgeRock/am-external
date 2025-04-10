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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import PropTypes from "prop-types";
import React, { useCallback, useState } from "react";
import { useDropzone } from "react-dropzone";
import { t } from "i18next";
import Base64 from "org/forgerock/commons/ui/common/util/Base64";

const UploadField = (props) => {
    const {
        name,
        required,
        schema,
        uiSchema,
        onChange
    } = props;
    const {
        multiple,
        icon,
        instructions
    } = uiSchema.options;

    // There is a naming collision acceptedFiles required by jsonSchemaForm and reactDropzone, cannot destructure.
    const accept = schema.acceptedFiles;
    const [fileList, setFile] = useState([]);

    const onDrop = useCallback((files) => {
        // multiple files not supported at this time
        const file = files[0];
        const reader = new FileReader();
        reader.onload = () => {
            const fileContentBase64 = Base64.encodeBase64Url(reader.result);
            setFile([file]);
            onChange(fileContentBase64);
        };
        reader.readAsText(file);
    }, [onChange]);

    const {
        acceptedFiles,
        getInputProps,
        getRootProps
    } = useDropzone({
        accept,
        onDrop,
        multiple
    });

    // Partials
    const files = acceptedFiles.map((file) => (
        <li className="list-group-item am-upload-field-queue-upload-item" key={ file.path }>
            <span aria-hidden="true" className={ `${icon} am-upload-field-queue-upload-icon` } />{file.path}
        </li>
    ));

    return (
        <section>
            <div { ...getRootProps({ className: "form-control am-upload-field" }) }>
                <input { ...getInputProps({ name, required }) } />
                {icon ? <span aria-hidden="true" className={ `${icon} fa-4x am-upload-field-icon-glyph` } /> : ""}
                <p className="upload-field-description">{ instructions }</p>
            </div>
            <div>
                {fileList.length
                    ? <ul className="list-group am-upload-field-files">{files}</ul>
                    : ""}
            </div>
            <p className="badge">
                { t(
                    "console.applications.federation.entityProviders.new.remote.uploadFieldFilesCount",
                    { filesCount: fileList.length }
                ) }
            </p>
        </section>
    );
};

UploadField.propTypes = {
    formData: PropTypes.string,
    name: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    required: PropTypes.bool,
    schema: PropTypes.shape({
        acceptedFiles: PropTypes.string.isRequired
    }).isRequired,
    uiSchema: PropTypes.shape({
        options: PropTypes.shape({
            icon: PropTypes.string.isRequired,
            instructions: PropTypes.string.isRequired,
            multiple: PropTypes.bool
        }).isRequired
    }).isRequired
};

export default UploadField;
