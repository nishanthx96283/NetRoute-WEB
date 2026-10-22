package com.netroute.repository;

/**
 * Thrown when an attempt is made to save a topology under a name that already exists
 * in the database, without explicitly specifying {@code overwrite = true}.
 * <p>
 * This enables the UI layer to catch the conflict and offer the user options:
 * Cancel, Choose a new name, or Overwrite the existing topology.
 */
public class DuplicateTopologyNameException extends Exception {

    private final String topologyName;
    private final int existingTopologyId;

    public DuplicateTopologyNameException(String topologyName, int existingTopologyId) {
        super("A saved topology named '" + topologyName + "' already exists (ID: " + existingTopologyId + ").");
        this.topologyName = topologyName;
        this.existingTopologyId = existingTopologyId;
    }

    public String getTopologyName() {
        return topologyName;
    }

    public int getExistingTopologyId() {
        return existingTopologyId;
    }
}
