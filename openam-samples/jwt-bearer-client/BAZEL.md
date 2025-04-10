<!--
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
-->
<!--
  Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 
 ! This code is to be used exclusively in connection with Ping Identity
 ! Corporation software or services. Ping Identity Corporation only offers
 ! such software or services to legal entities who have entered into a
 ! binding license agreement with Ping Identity Corporation.
-->

# Bazel build for Sample JWT Bearer Client

## Running the build
Run the following command for a thin jar without the dependencies included:<br>
`bazel build //openam-samples/jwt-bearer-client:jwt-bearer-client`

Run the following command for a full jar with the dependencies included:<br>
`bazel build //openam-samples/jwt-bearer-client:jwt-bearer-client_deploy.jar`<br>
A jar file with the suffix `_deploy` will be generated in the bazel output folder.
