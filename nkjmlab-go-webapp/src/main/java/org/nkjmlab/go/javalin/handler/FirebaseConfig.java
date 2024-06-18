package org.nkjmlab.go.javalin.handler;

public record FirebaseConfig(
    String apiKey,
    String authDomain,
    String databaseURL,
    String projectId,
    String storageBucket,
    String messagingSenderId,
    String appId,
    String measurementId) {}
