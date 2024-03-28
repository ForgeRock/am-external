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
 * Copyright 2014-2023 ForgeRock AS.
 */
START_TIME = 9   // 9am
END_TIME   = 17  // 5pm

logger.message("Starting authentication groovy script")
logger.message("User: " + username)

// Log out current cookies in the request
if (logger.messageEnabled()) {
    cookies = requestData.getHeaders('Cookie')
    for (cookie in cookies) {
        logger.message('Cookie: ' + cookie)
    }
}

if (username != null) {
    // Make REST call to the users endpoint if username is set.
    response = httpClient.get("http://localhost:8080/openam/json/users/" + username,[
        cookies : [],
        headers : []
    ]);
    // Log out response from users REST call
    logger.message("User REST Call. Status: " + response.getStatusCode() + ", Body: " + response.getEntity().getString())
}

now = new Date()
logger.message("Current time: " + now.getHours())
if (now.getHours() &lt; START_TIME || now.getHours() &gt; END_TIME) {
    logger.error("Login forbidden outside work hours!")
    authState = FAILED
} else {
    logger.message("Authentication allowed!")
    authState = SUCCESS
}
