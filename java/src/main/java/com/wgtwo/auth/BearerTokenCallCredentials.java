package com.wgtwo.auth;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public class BearerTokenCallCredentials extends CallCredentials {
    private static final Metadata.Key<String> key = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    private final Supplier<String> tokenSource;

    public BearerTokenCallCredentials(@NotNull Supplier<String> tokenSource) {
        this.tokenSource = tokenSource;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier metadataApplier) {
        executor.execute(() -> {
            String token = tokenSource.get();
            try {
                Metadata metadata = new Metadata();
                metadata.put(key, "Bearer " + token);
                metadataApplier.apply(metadata);
            } catch (Throwable e) {
                Status status = Status.UNAUTHENTICATED.withDescription("Could not apply access token").withCause(e);
                metadataApplier.fail(status);
            }
        });
    }

    @Override
    public void thisUsesUnstableApi() {
    }
}
