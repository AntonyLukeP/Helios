package io.helios.common.codec;

/**
 * SPI for serializing opaque payloads (workflow input, activity input/result, signal/query bodies).
 *
 * <p>The engine is schema-agnostic: it stores {@link EncodedPayload} blobs and never interprets
 * them (ADR-001). The default converter is JSON; a codec chain (compression, then optional
 * client-side encryption — Topic 16) can wrap it. Serialization schemas evolve under
 * <em>tolerant-reader</em> rules (additive-only; never remove/repurpose a field — Topic 24 §1.5),
 * a discipline the engine cannot enforce because payloads are opaque.
 */
public interface PayloadCodec {

    /** Serialize a value into an opaque payload with its encoding descriptor. */
    EncodedPayload encode(Object value);

    /** Deserialize an opaque payload back into the requested type. */
    <T> T decode(EncodedPayload payload, Class<T> type);
}
