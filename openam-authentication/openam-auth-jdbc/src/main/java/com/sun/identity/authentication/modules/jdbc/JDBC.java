/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: JDBC.java,v 1.5 2008/08/28 21:56:45 madan_ranganath Exp $
 *
 * Portions Copyrighted 2011-2025 Ping Identity Corporation.
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package com.sun.identity.authentication.modules.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.ResourceBundle;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;

public class JDBC extends AMLoginModule {
    private String userTokenId;
    private String userName;
    private String password;
    private String resultPassword;
    private char[] passwordCharArray;
    private java.security.Principal userPrincipal = null;
    private String errorMsg = null;
    
    private static final String amAuthJDBC = "amAuthJDBC";
    private static Logger debug = LoggerFactory.getLogger(JDBC.class);
    private ResourceBundle bundle = null;
    private static final String INVALID_CHARS = "forgerock-am-auth-jdbc-invalid-chars";
    
    private Map options;
    
    private static String CONNECTIONTYPE =
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "JDBCConnectionType";
    private static String JNDINAME = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + 
        "JDBCJndiName";
    private static String DRIVER = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + 
        "JDBCDriver";
    private static String URL =
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "JDBCUrl";
    private static String DBUSER = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + 
        "JDBCDbuser";
    private static String DBPASSWORD = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + 
        "JDBCDbpassword";
    private static String PASSWORDCOLUMN =
        ISAuthConstants.AUTH_ATTR_PREFIX_NEW + "JDBCPasswordColumn";
    private static String STATEMENT = ISAuthConstants.AUTH_ATTR_PREFIX_NEW + 
        "JDBCStatement";
    private static String TRANSFORM =ISAuthConstants.AUTH_ATTR_PREFIX_NEW +  
        "JDBCPasswordSyntaxTransformPlugin";
    private static String DEFAULT_TRANSFORM =
        "com.sun.identity.authentication.modules.jdbc.ClearTextTransform";
    
    private String driver;
    private String connectionType;
    private String jndiName;
    private String url;
    private String dbuser;
    private String dbpassword;
    private String passwordColumn;
    private String statement;
    private String transform;
    private Map sharedState;
    private boolean getCredentialsFromSharedState = false;
    private static final int MAX_NAME_LENGTH = 80;
    
    private boolean useJNDI = false;
    
    /**
     * Constructor.
     */
    public JDBC() {
        debug.debug("JDBC()");
    }

    /**
     * Initializes parameters.
     *
     * @param subject
     * @param sharedState
     * @param options
     */
    public void init(Subject subject, Map sharedState, Map options) {
        debug.debug("in initialize...");
        java.util.Locale locale  = getLoginLocale();
        bundle = amCache.getResBundle(amAuthJDBC, locale);
        
        if (debug.isDebugEnabled()) {
            debug.debug("amAuthJDBC Authentication resource bundle locale="+
                          locale);
        }

        this.options = options;
        this.sharedState = sharedState;
        
        if(options != null) {
            try {
                // First, figure out the type of connection
                connectionType = CollectionHelper.getMapAttr(
                    options, CONNECTIONTYPE);
                if (connectionType == null) {
                    debug.debug("No CONNECTIONTYPE for configuring");
                    errorMsg ="noCONNECTIONTYPE";
                    return;
                } else { 
                    if (debug.isDebugEnabled()) {
                        debug.debug("Found config for CONNECTIONTYPE: " + 
                                      connectionType);
                    }
               
                    if (connectionType.equals("JNDI")) { 
                        useJNDI = true;
                    }
                    
                    // If its pooled, get the JNDI name
                    if ( useJNDI ) {
                        debug.debug("Using JNDI Retrieved Connection pool");
                        jndiName = CollectionHelper.getMapAttr(
                            options, JNDINAME);
                        if (jndiName == null) {
                            debug.debug("No JNDINAME for configuring");
                            errorMsg ="noJNDINAME";
                            return;
                        } else  { 
                            if (debug.isDebugEnabled()) {
                                debug.debug("Found config for JNDINAME: " + 
                                              jndiName);
                            }
                        }

                    // If its a non-pooled, then get the JDBC config
                    } else {
                        debug.debug("Using non pooled JDBC");
                        driver = CollectionHelper.getMapAttr(options, DRIVER);
                        if (driver == null) {
                            debug.debug("No DRIVER for configuring");
                            errorMsg ="noDRIVER";
                            return;
                        } else  {
                            if (debug.isDebugEnabled()) 
                                debug.debug("Found config for DRIVER: " + 
                                              driver);
                        }

                        url = CollectionHelper.getMapAttr(options, URL);
                        if (url == null) {
                            debug.debug("No URL for configuring");
                            errorMsg ="noURL";
                            return;
                        } else {
                            if (debug.isDebugEnabled()) {
                                debug.debug("Found config for URL: " + url);
                            }
                        }
                        dbuser = CollectionHelper.getMapAttr(options, DBUSER);
                        if (dbuser == null) {
                            debug.debug("No DBUSER for configuring");
                            errorMsg = "noDBUSER";
                            return;
                        } else  {
                            if (debug.isDebugEnabled()) {
                                debug.debug("Found config for DBUSER: " +
                                              dbuser);
                            }
                        }

                        dbpassword = CollectionHelper.getMapAttr(
                            options, DBPASSWORD, "");
                        if (dbpassword == null) {
                            debug.debug("No DBPASSWORD for configuring");
                            errorMsg = "noDBPASSWORD";
                            return;
                        }
                    }
                }
                
                // and get the props that apply to both connection types 
                passwordColumn = CollectionHelper.getMapAttr(
                    options, PASSWORDCOLUMN);
                if (passwordColumn == null) {
                    debug.debug("No PASSWORDCOLUMN for configuring");
                    errorMsg = "noPASSWORDCOLUMN";
                    return;
                } else {
                    if (debug.isDebugEnabled()) { 
                        debug.debug("Found config for PASSWORDCOLUMN: " +
                                      passwordColumn);
                    }
                }          
                statement = CollectionHelper.getMapAttr(options, STATEMENT);
                if (statement == null) {
                    debug.debug("No STATEMENT for configuring");
                    errorMsg = "noSTATEMENT";
                }                   
                transform = CollectionHelper.getMapAttr(options, TRANSFORM);
                if (transform == null) {
                    if (debug.isDebugEnabled()) {
                        debug.debug("No TRANSFORM for configuring."+
                                      "Using clear text");
                    }
                    transform = DEFAULT_TRANSFORM;
                } else {
                    if (debug.isDebugEnabled()) {
                        debug.debug("Plugin for TRANSFORM: " + transform);
                    }
                }
                      
            } catch(Exception ex) {
                debug.error("JDBC Init Exception", ex);
            }
        }
    }
    
    /**
     * Processes the authentication request.
     *
     * @return <code>ISAuthConstants.LOGIN_SUCCEED</code> as succeeded; 
     *         <code>ISAuthConstants.LOGIN_IGNORE</code> as failed.
     * @exception AuthLoginException upon any failure. login state should be 
     *            kept on exceptions for status check in auth chaining.
     */
    public int process(Callback[] callbacks, int state) 
        throws AuthLoginException {
        // return if this module is already done
        if (errorMsg != null) {
            throw new AuthLoginException(amAuthJDBC, errorMsg, null);
        }
        if (debug.isDebugEnabled()) {
            debug.debug("State: " + state);
        }
        
        if (state != ISAuthConstants.LOGIN_START) {
            throw new AuthLoginException(amAuthJDBC, "invalidState", null);
        }

        if (callbacks != null && callbacks.length == 0) {
            userName = (String) sharedState.get(getUserKey());
                 password = (String) sharedState.get(getPwdKey());
                 if (userName == null || password == null) {
                 return ISAuthConstants.LOGIN_START;
            }
            getCredentialsFromSharedState = true;
        } else {        
            userName = ((NameCallback) callbacks[0]).getName();
            if (debug.isDebugEnabled()) {
                debug.debug("Authenticating this user: " + userName);
            }
                
            passwordCharArray = ((PasswordCallback) callbacks[1]).getPassword();
            password = new String(passwordCharArray);
                
            if (userName == null || userName.length() == 0) {
                throw new AuthLoginException(amAuthJDBC, "noUserName", null);
            }
        }
        
        storeUsernamePasswd(userName, password);

        // Check if they'return being a bit malicious with their UID.
        // SQL attacks will be handled by prepared stmt escaping.
        if (userName.length() > MAX_NAME_LENGTH ) {
            throw new AuthLoginException(amAuthJDBC, "userNameTooLong", null);
        } 
   
        validateUserName(userName, CollectionHelper.getMapAttr(options, INVALID_CHARS));

        Connection database = null;
        PreparedStatement thisStatement = null;
        ResultSet results = null;
        try {
            if ( useJNDI ) {        
                Context initctx = new InitialContext();
                DataSource ds = (DataSource)initctx.lookup(jndiName);

                if (debug.isDebugEnabled()) { 
                    debug.debug("Datasource Acquired: " + ds.toString());
                }
                database = ds.getConnection();
                debug.debug("Using JNDI Retrieved Connection pool");
                
            } else {
                Class.forName (driver);
                database = DriverManager.getConnection(url,dbuser,dbpassword);
            }          
            if (debug.isDebugEnabled()) {
                debug.debug("Connection Acquired: " + database.toString());
            }           
            //Prepare the statement for execution
            if (debug.isDebugEnabled()) {
                debug.debug("PreparedStatement to build: " + statement);
            }
            thisStatement = 
                database.prepareStatement(statement);
            thisStatement.setString(1,userName);
            if (debug.isDebugEnabled()) {
                    debug.debug("Statement to execute: " + thisStatement);
            }
            
            // execute the query
            results = thisStatement.executeQuery();
            
            if (results == null) {
                debug.debug("returned null from executeQuery()");
                throw new AuthLoginException(amAuthJDBC, "nullResult", null);
            }
            
            //parse the results.  should only be one item in one row.
            int index = 0;
            while (results.next()) {
                // do normal processing..its the first and last row
                index ++;
                if (index > 1) {
                    if (debug.isDebugEnabled()) {
                        debug.debug("Too many results."+
                                "UID should be a primary key");
                    }
                    throw new AuthLoginException(amAuthJDBC, "multiEntry",null);
                }
                resultPassword = results.getString(passwordColumn).trim();
            } 
            if (index == 0) {
                // no results
                if (debug.isDebugEnabled()) {
                    debug.debug("No results from your SQL query."+
                            "UID should be valid");
                }
                throw new AuthLoginException(amAuthJDBC, "nullResult", null);
             }
        } catch (Throwable e) {
            if (getCredentialsFromSharedState && !isUseFirstPassEnabled()) {
                getCredentialsFromSharedState = false;
                return ISAuthConstants.LOGIN_START;
            }
            if (debug.isDebugEnabled()) {
                debug.debug("JDBC Exception:",  e);
            }
            throw new AuthLoginException(e);
        } finally {
            // close the resultset
            if (results != null) {
                  try {
                    results.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            // close the statement
            if (thisStatement != null) {
                  try {
                    thisStatement.close();
                } catch (Exception e) {
                    // ignore
                }
            }
            // close the connection when done
            if (database != null) {
                  try {
                    database.close();
                } catch (Exception dbe) {
                    debug.error("Error in closing database connection: " + 
                        dbe.getMessage());
                    if (debug.isDebugEnabled()) {
                        debug.debug("Fail to close database:", dbe);
                    }
                }
            }
        }  
            
        if (!transform.equals(DEFAULT_TRANSFORM)) {
            try {
                  JDBCPasswordSyntaxTransform syntaxTransform = 
                    (JDBCPasswordSyntaxTransform)Class.forName(transform)
                        .newInstance();
                if (debug.isDebugEnabled()) {
                    debug.debug("Got my Transform Object" + 
                            syntaxTransform.toString() );
                }
                password = syntaxTransform.transform(password);

                if (debug.isDebugEnabled()) {
                    debug.debug("Password transformed by: " + transform );
                }
            } catch (Throwable e) {
                if (debug.isDebugEnabled()) {
                    debug.debug("Syntax Transform Exception:"+ e.toString());
                  }
                throw new AuthLoginException(e);               
            }
        }
        // see if the passwords match
        if (password != null && password.equals(resultPassword)) {
            userTokenId = userName;
            return ISAuthConstants.LOGIN_SUCCEED;
        } else {           
            debug.debug("password not match. Auth failed.");
            setFailureID(userName);
            throw new InvalidPasswordException(amAuthJDBC, "loginFailed",
                null, userName, isReturningPrincipalAsDn(), null);
        }
    }

    /**
     * Returns principal of the authenticated user.
     *
     * @return Principal of the authenticated user.
     */
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userTokenId != null) {
            userPrincipal = new JDBCPrincipal(userTokenId);
            return userPrincipal;   
        } else {
            return null;
        }
    }

    /**
     * Cleans up the login state.
     */
    public void destroyModuleState() {
        userTokenId = null;
        userPrincipal = null;
    }

    public void nullifyUserdVars() {
        userName = null;
        password = null;
        resultPassword = null;
        passwordCharArray = null;
        errorMsg = null;
        bundle = null;
        options = null;
        driver = null;
        connectionType = null;
        jndiName = null;
        url = null;
        dbuser = null;
        dbpassword = null;
        passwordColumn = null;
        statement = null;
        transform = null;
        sharedState = null;
    }
}
