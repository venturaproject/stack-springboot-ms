package com.boilerplate.application.orders;

import jakarta.validation.constraints.Positive;

public record UpdateOrderLineQuantityCommand(@Positive int quantity) {}
