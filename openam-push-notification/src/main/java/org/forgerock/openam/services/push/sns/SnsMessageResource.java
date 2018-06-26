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
 * Copyright 2016-2018 ForgeRock AS.
 */
package org.forgerock.openam.services.push.sns;

import static org.forgerock.openam.i18n.apidescriptor.ApiDescriptorConstants.*;

import org.forgerock.api.annotations.Action;
import org.forgerock.api.annotations.ApiError;
import org.forgerock.api.annotations.Handler;
import org.forgerock.api.annotations.Operation;
import org.forgerock.api.annotations.RequestHandler;
import org.forgerock.api.annotations.Schema;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.services.push.PushMessageResource;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import com.google.inject.Inject;

/**
 * <p>Amazon SNS implementation of a CREST endpoint for the Push Notification Service.</p>
 *
 * <p>Decouples the internal operations relating to using the message dispatcher from the endpoint itself by using the
 * internal {@link PushMessageResource}. This would allow for this class to perform message manipulation if
 * desired, prior to being handled by OpenAM.</p>
 */
@RequestHandler(@Handler(mvccSupported = false,
        title = SNS_MESSAGE_RESOURCE + TITLE, description = SNS_MESSAGE_RESOURCE + DESCRIPTION))
public class SnsMessageResource {

    private final PushMessageResource pushMessageResource;

    /**
     * Generates a new Amazon SNS Message Resource endpoint.
     *
     * @param pushMessageResource The internal {@link PushMessageResource} which handles operations.
     */
    @Inject
    public SnsMessageResource(PushMessageResource pushMessageResource) {
        this.pushMessageResource = pushMessageResource;
    }

    /**
     * For an authentication message - passes this message into the internal {@link MessageDispatcher} to
     * attempt to resolve it against a message that was expected by {@link MessageDispatcher}.
     *
     * @param context Context of this request used to retrieve the current realm location.
     * @param actionRequest The request triggering the dispatch.
     * @return An empty HTTP 200 if everything was okay, or an HTTP 400 if the request was malformed.
     */
    @Action(operationDescription = @Operation(
            description = SNS_MESSAGE_RESOURCE + ACTION + "authenticate." + DESCRIPTION,
            errors = @ApiError(code = 400, description = SNS_MESSAGE_RESOURCE + ERROR_400_DESCRIPTION)),
            request = @Schema(schemaResource = "SnsMessageResource.authenticate.schema.json"),
            response = @Schema(schemaResource = "SnsMessageResource.response.schema.json"))
    public Promise<ActionResponse, ResourceException> authenticate(Context context, ActionRequest actionRequest) {
        return pushMessageResource.handle(context, actionRequest);
    }

    /**
     * For a registration message - passes this message into the internal {@link MessageDispatcher} to
     * attempt to resolve it against a message that was expected by {@link MessageDispatcher}.
     *
     * @param context Context of this request used to retrieve the current realm location.
     * @param actionRequest The request triggering the dispatch.
     * @return An empty HTTP 200 if everything was okay, or an HTTP 400 if the request was malformed.
     */
    @Action(operationDescription = @Operation(
            description = SNS_MESSAGE_RESOURCE + ACTION + "register." + DESCRIPTION,
            errors = @ApiError(code = 400, description = SNS_MESSAGE_RESOURCE + ERROR_400_DESCRIPTION)),
            request = @Schema(schemaResource = "SnsMessageResource.register.schema.json"),
            response = @Schema(schemaResource = "SnsMessageResource.response.schema.json"))
    public Promise<ActionResponse, ResourceException> register(Context context, ActionRequest actionRequest) {
        return pushMessageResource.handle(context, actionRequest);
    }
}
