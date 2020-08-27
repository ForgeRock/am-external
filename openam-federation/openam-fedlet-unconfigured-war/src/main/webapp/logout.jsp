<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved

   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.

   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.

   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: logout.jsp,v 1.2 2009/06/17 03:07:37 exu Exp $

   Portions Copyrighted 2016 ForgeRock AS.
--%><%@ page language="java"
        import="java.util.Enumeration,
org.slf4j.LoggerFactory,
org.slf4j.Logger" %><%! private final Logger logger = LoggerFactory.getLogger("jsp.logout"); %>
%><%
    /**
     * If header SLOStatus doesn't exist, it's IDP initiated SLO. Log user out.
     *    Besides "Cookie" header, other SLO related headers are: "IDP", "SP",
     *    "NameIDValue", "SessionIndex", "Binding".
     * If header SLOStatus exist, it's fedlet initiated SLO. Do processing 
     *     according to the SLOStatus. Besides "Cookie" header, other SLO 
     *     related headers are: "IDP", "SP", "NameIDValue", "SessionIndex", 
     *     "Binding", "SLOStatus".
     */
    if (logger.isDebugEnabled()) {
        logger.debug("in fedlet logout.jsp.");
    }
    Enumeration headers = request.getHeaderNames();
    while (headers.hasMoreElements()) {
        String name = (String) headers.nextElement();
        if (logger.isDebugEnabled()) {
            logger.debug(
                "header name=" + name + " value=" + request.getHeader(name));
        }
    }
%>
