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
 * Copyright 2019-2020 ForgeRock AS.
 */

package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.EncryptionAlgorithm;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;

/**
 * {@link ValueMapper} implementation that exposes details of encryption key descriptors.
 *
 * @since 7.0.0
 */
public final class EncryptionAlgorithmMapper
        extends ValueMapper<List<String>, List<EncryptionAlgorithm>> {

    @Override
    public List<EncryptionAlgorithm> map(List<String> value, EnricherContext context) {
        return value.stream()
                .map(EncryptionAlgorithm::fromValue)
                .distinct()
                .collect(toList());
    }

    @Override
    public List<String> inverse(List<EncryptionAlgorithm> encryptionAlgorithms, EnricherContext context) {
        return encryptionAlgorithms.stream()
                .distinct()
                .map(EncryptionAlgorithm::value)
                .collect(toList());
    }

}
