package com.boilerplate.application.auth;

/** Output of a login / refresh use case: the user view plus the tokens to hand back. */
public record LoginResult(UserResponse user, IssuedTokens tokens) {}
