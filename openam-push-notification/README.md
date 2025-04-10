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
 * Copyright 2018-2025 Ping Identity Corporation.
-->
# AM Push Notification

A library to generate and send Push messages, and route their responses back to be handled within AM.

The push package contains the push service implemented within AM, as well as interfaces for generating new delegates.

Delegates are implementations of remote push services, which AM will use to send messages. The sns package contains the default Amazon SNS implementation which ships with AM.

The dispatch package contains the MessageDispatcher, which uses the CTS to communicate messages received at a delegate's exposed endpoint across a cluster of AMs.

The utils package contains helpful additional classes.

For information on how to use the openam-push-notification package to build new delegates, please see the [ForgeRock Knowledge Base](https://backstage.forgerock.com/knowledge/kb/home).

## Disclaimer

ForgeRock does not warrant, guarantee or make any representations regarding the use, results of use, accuracy, timeliness or completeness of any data or information relating to the sample code. ForgeRock disclaims all warranties, expressed or implied, and in particular, disclaims all warranties of merchantability, and warranties related to the code, or any service or software related thereto.

ForgeRock shall not be liable for any direct, indirect or consequential damages or costs of any type arising out of any action taken by you or others related to the sample code.

[forgerock_platform]: https://www.forgerock.com/platform/  
