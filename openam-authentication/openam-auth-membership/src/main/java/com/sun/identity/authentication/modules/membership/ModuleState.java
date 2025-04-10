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
 * Copyright 2011-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package com.sun.identity.authentication.modules.membership;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * The enum wraps up all of the possible user states in the Membership module
 * 
 * @author steve
 */
public enum ModuleState {
    /* the module is completed
     */
    COMPLETE(-1, "complete"),
    
    /* start of the login process
     */
    LOGIN_START(1, "loginStart"),
    
    DISCLAIMER_DECLINED(2, "disclaimerDeclined"),
    
    PROFILE_ERROR(3, "profileError"),
    
    /* having the registration and disclaimer at the end of
     * the properties file allows the disclaimer page to
     * become optional. If the user does not want to display
     * the disclaimer they just need to remove its state definition
     * from the properties file. Following a successful completion
     * of the registration page the next state will be
     * the disclaimer page if it exists. If it is not there
     * the registration page by default becomes the last page and
     * the verification process ends
     */
    REGISTRATION(4, "registration"),
            
    /* if the mode is set to turn on user name generator, then
     * this displays some possible choices of user names
     * generated by the pluggable user name generator
     */
    CHOOSE_USERNAMES(5, "chooseUsernames"),

    /* if disclaimer page exists the user is created after
     * the user agrees to disclaimer
     */
    DISCLAIMER(6, "disclaimer");

    private static final Map<Integer,ModuleState> lookup = 
            new HashMap<Integer,ModuleState>();

    static {
        for (ModuleState ls : EnumSet.allOf(ModuleState.class)) {
            lookup.put(ls.intValue(), ls);
        }
    }

    private final int state;
    private final String name;

    private ModuleState(final int state, final String name) {
        this.state = state;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static ModuleState get(int screen) {
        return lookup.get(screen);
    }

    int intValue() {
        return state;
    }   
}

