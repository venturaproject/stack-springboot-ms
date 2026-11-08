package com.boilerplate.application.notifications;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create a notification addressed to the current user — the "send a test notification" affordance. */
public record SendNotificationCommand(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 2000) String message,
        @Nullable @Size(max = 20) String type) {}
