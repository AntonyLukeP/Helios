package io.helios.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ShardHasher {

    public static final int SHARD_COUNT = 512;

    private ShardHasher() {
    }

    public static int shardId(String tenantId, String workflowId) {
        return shardId(tenantId, workflowId, SHARD_COUNT);
    }

    public static int shardId(String tenantId, String workflowId, int shardCount) {
        if (tenantId == null || workflowId == null) {
            throw new IllegalArgumentException("tenantId and workflowId must be non-null");
        }
        if (shardCount <= 0) {
            throw new IllegalArgumentException("shardCount must be positive");
        }
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available", e);
        }
        md.update(tenantId.getBytes(StandardCharsets.UTF_8));
        md.update((byte) 0);
        md.update(workflowId.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        long h = 0L;
        for (int i = 0; i < 8; i++) {
            h = (h << 8) | (digest[i] & 0xffL);
        }
        return (int) Math.floorMod(h, (long) shardCount);
    }
}
