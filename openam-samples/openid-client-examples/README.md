<!-- 
The contents of this file are subject to the terms of the Common Development and
Distribution License (the License). You may not use this file except in 
compliance with the License.

You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for 
the specific language governing permission and limitations under the License.
When distributing Covered Software, include this CDDL Header Notice in each file
and include the License file at legal/CDDLv1.0.txt. If applicable, add the 
following below the CDDL Header, with the fields enclosed by brackets [] 
replaced by your own identifying information: 
"Portions copyright [year] [name of copyright owner]".

Copyright 2013-2017 ForgeRock AS.
-->
# OpenID Connect Examples


## About

These simple examples use the OpenID Connect 1.0 provider support in
the [Forgerock Identity Platform](https://www.forgerock.com/platform/) 5.0 (AKA OpenAM 14.0.0). 

These samples are intended to support and demonstrate the ['Using OpenID Connect 1.0'](https://backstage.forgerock.com/docs/am/5/oidc1-guide/#chap-oidc1-usage) chapter of the AM [OpenID Connect 1.0 Guide](https://backstage.forgerock.com/docs/am/5/oidc1-guide/). 

1.   Clone the project for deployment in your container alongside OpenAM.
     For example, with OpenAM in `/path/to/tomcat/webapps/openam`,
     clone this under `/path/to/tomcat/webapps`
     into `/path/to/tomcat/webapps/openid`.
2.   Adjust the configuration as necessary.
     See the `*.js` files.
3.   Create the agent profile as described in the examples.
4.   Try it out.

The examples are not secure. Instead they are completely transparent,
showing the requests and the steps for the Basic and Implicit Profiles,
showing how to register with OpenID Connect Dynamic Client Registration,
and showing OpenAM as OP and Authenticator for GSMA Mobile Connect.
(Mobile Connect support requires OpenAM 12 or later.)

## Licensing

The he files in this project are licensed under the [CDDL License](https://forum.forgerock.com/cddlv1-0/) and as such may be used as a basis for your own projects.

