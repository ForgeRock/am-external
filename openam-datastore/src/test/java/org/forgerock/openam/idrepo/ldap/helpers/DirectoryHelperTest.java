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
package org.forgerock.openam.idrepo.ldap.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public class DirectoryHelperTest {

    private final DirectoryHelper helper = new DirectoryHelper();
    private final String DELETED = "Deleted";
    private final String ACTIVE = "Active";
    private final String INACTIVE = "Inactive";

    @Test
    public void convertToInetUserStatusReturnsInactiveForDeleted() {
        assertThat(helper.convertToInetUserStatus(DELETED,ACTIVE)).isEqualTo(INACTIVE);
    }

}
