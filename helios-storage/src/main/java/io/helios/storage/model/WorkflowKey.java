package io.helios.storage.model;

import io.helios.common.ShardHasher;
import java.util.Objects;

public record WorkflowKey(String tenantId, String workflowId, String runId) {

    public WorkflowKey {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(workflowId, "workflowId");
        Objects.requireNonNull(runId, "runId");
    }

    public int shardId() {
        return ShardHasher.shardId(tenantId, workflowId);
    }
}
