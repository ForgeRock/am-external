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
 * Copyright 2017 ForgeRock AS.
 */
/**
 * jon.knight@forgerock.com
 *
 * A node that returns true if the user's email address is recorded as breached by the HaveIBeenPwned website (http://haveibeenpwned.com)
 * or false if no breach has been recorded
 */


package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.utils.Time;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;


/**
 * An authentication node integrating with iProov face recognition solution.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = iProovNode.Config.class)
public class iProovNode extends AbstractDecisionNode {

    /**
     * Configuration for the node.
     */
    public interface Config {
        @Attribute(order = 100)
        String APIkey();
        @Attribute(order = 200)
        String Secret();
        @Attribute(order = 300)
        String attribute();
        @Attribute(order = 400)
        int timeout();
    }

    private final Config config;
    private final CoreWrapper coreWrapper;
    private final static String DEBUG_FILE = "iProovNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    public enum State { SUCCESS, CREATE, FAILED };

    /**
     * Guice constructor.
     * @param config The node configuration.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public iProovNode(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException {
        this.config = config;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        // Either initial call to this node, or a revisit from a polling callback?
        if (!context.hasCallbacks()) {
            AMIdentity userIdentity = coreWrapper.getIdentity(context.sharedState.get(USERNAME).asString(), context.sharedState.get(REALM).asString());
            Object iProovUser = "";

            // Get iProov user ID from specified attribute in user profile
            try {
                Set idAttrs = userIdentity.getAttribute(config.attribute());
                if (idAttrs == null || idAttrs.isEmpty()) {
                    debug.error("[" + DEBUG_FILE + "]: " + "Unable to find iProov user attribute: " + config.attribute());
                } else {
                    iProovUser = idAttrs.iterator().next();
                }
            } catch (IdRepoException | SSOException e) {
                debug.error("[" + DEBUG_FILE + "]: " + "Error getting atttibute '{}' ", e);
            }

            // Initiate iProov session and return a polling callback
            String session_key = iProovStart(iProovUser);
            if (session_key.isEmpty()) {
                // Something gone wrong - no session key from iProov
                debug.error("[" + DEBUG_FILE + "]: " + "Got no session_key from iProov");
                return goTo(false).build();
            }
            else return send(new PollingWaitCallback("4000")).replaceSharedState(context.sharedState.copy().add("iproov_start", Time.currentTimeMillis()).add("iproov_session_key", session_key)).build();

        } else {
            Optional<PollingWaitCallback> answer = context.getCallback(PollingWaitCallback.class);
            if (answer.isPresent()) {
                String session_key = context.sharedState.get("iproov_session_key").asString();
                // Check status with iProov
                State state = iProovValidate(context.sharedState.get("iproov_session_key").asString());
                if (state == State.SUCCESS) return goTo(true).build();
                else if (state == State.FAILED) return goTo(false).build();
                // If not exceeded maximum timeout then poll again
                else if ((Time.currentTimeMillis() - context.sharedState.get("iproov_start").asLong()) < config.timeout())
                    return send(new PollingWaitCallback("4000")).build();
                else return goTo(false).build();
            }
        }
        return goTo(false).build();
    }


    private String iProovStart(Object iProovUser) {
        String iProovParams = "{\"_query\":\"create\", " +
                " \"api_key\": \"" + config.APIkey() + "\", " +
                " \"secret\": \"" + config.Secret() + "\", " +
                " \"user_id\": \"" + iProovUser + "\"} ";
        String response = postRequest("https://secure.iproov.me/api/v2/claim/verify/push", iProovParams);
        String session_key = "";
        try {
            JSONObject json = new JSONObject(response);
            if (json.has("session_key")) session_key = json.get("session_key").toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return(session_key);
    }



    private State iProovValidate(String session_key) {
        String state = "";
        String params = "{ \"api_key\": \"" + config.APIkey() + "\", \"secret\": \"" + config.Secret() + "\" }";
        String response = postRequest("https://secure.iproov.me/api/v2/claim/verify/push/" + session_key, params);
        try {
            JSONObject json = new JSONObject(response);
            if (json.has("state")) state = json.get("state").toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        switch (state) {
            case "Success":
                return State.SUCCESS;
            case "Created":
                return State.CREATE;
            default:
                return State.FAILED;
        }
    }


    private String postRequest(String urlPath, String params) {
        String response = "";
        try {
            URL url = new URL(urlPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");

            OutputStream os = conn.getOutputStream();
            os.write(params.getBytes("UTF-8"));
            os.close();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                debug.message("[" + DEBUG_FILE + "]: HTTP failed, response code:" + conn.getResponseCode());
                throw new RuntimeException("[" + DEBUG_FILE + "]: HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            while ((output = br.readLine()) != null) {
                response = response + output;
            }
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

}
