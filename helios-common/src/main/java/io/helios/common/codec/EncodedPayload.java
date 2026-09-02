package io.helios.common.codec;

import java.util.Objects;

/**
 * An opaque serialized payload plus its {@code payload_encoding} descriptor (Topic 01, Topic 10 §E).
 *
 * <p>The engine stores {@link #bytes()} as {@code bytea} and never parses them; the
 * {@link #encoding()} descriptor (codec/compression/encryption) lets a reader hand the blob back to
 * the SDK without the engine understanding it. Only declared search attributes (Topic 11) are ever
 * extracted from payloads, and that happens outside this type.
 */
public record EncodedPayload(byte[] bytes, String encoding) {

    public EncodedPayload {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(encoding, "encoding");
    }

    public int size() {
        return bytes.length;
    }
}
