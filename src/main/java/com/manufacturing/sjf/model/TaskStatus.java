package com.manufacturing.sjf.model;

/**
 * Represents the lifecycle state of a manufacturing task.
 */
public enum TaskStatus {
    /** Task has been added but the simulation has not started. */
    PENDING("Pending", "#9CA3AF"),

    /** Task has arrived and is eligible for scheduling, waiting in queue. */
    READY("Ready", "#F59E0B"),

    /** Task is currently being processed on the machine. */
    PROCESSING("Processing", "#14B8A6"),

    /** Task has finished processing. */
    COMPLETED("Completed", "#22C55E"),

    /** Task encountered a validation or runtime error. */
    FAILED("Failed", "#F87171");

    private final String displayName;
    private final String hexColour;

    TaskStatus(String displayName, String hexColour) {
        this.displayName = displayName;
        this.hexColour = hexColour;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHexColour() {
        return hexColour;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
