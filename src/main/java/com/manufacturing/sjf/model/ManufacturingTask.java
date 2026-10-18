package com.manufacturing.sjf.model;

import java.util.Objects;

/**
 * Core data model representing a manufacturing task in the production system.
 *
 * <p>A task has a unique string ID, a human-readable name, an arrival time
 * (when the task enters the system), and a burst time (the machine time
 * required to complete it).</p>
 *
 * <p>Instances are mutable so that the task management panel can update them
 * before the simulation runs. Once the simulation begins, tasks should be
 * treated as read-only by the scheduler.</p>
 */
public class ManufacturingTask {

    private String taskId;
    private String taskName;
    private int arrivalTime;   // time units from simulation start
    private int burstTime;     // processing time required (must be > 0)
    private TaskStatus status;

    public ManufacturingTask(String taskId, String taskName, int arrivalTime, int burstTime) {
        validate(taskId, arrivalTime, burstTime);
        this.taskId = taskId.trim();
        this.taskName = (taskName == null || taskName.isBlank()) ? taskId.trim() : taskName.trim();
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.status = TaskStatus.PENDING;
    }

    // ─── Validation ──────────────────────────────────────────────────────────

    public static void validate(String taskId, int arrivalTime, int burstTime) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID must not be empty.");
        }
        if (arrivalTime < 0) {
            throw new IllegalArgumentException(
                "Arrival time must be ≥ 0. Got: " + arrivalTime);
        }
        if (burstTime <= 0) {
            throw new IllegalArgumentException(
                "Burst time must be > 0. Got: " + burstTime);
        }
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID must not be empty.");
        }
        this.taskId = taskId.trim();
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = (taskName == null || taskName.isBlank()) ? this.taskId : taskName.trim();
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("Arrival time must be ≥ 0.");
        }
        this.arrivalTime = arrivalTime;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public void setBurstTime(int burstTime) {
        if (burstTime <= 0) {
            throw new IllegalArgumentException("Burst time must be > 0.");
        }
        this.burstTime = burstTime;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    // ─── Object Methods ───────────────────────────────────────────────────────

    /** Creates a shallow copy of this task with PENDING status. */
    public ManufacturingTask copy() {
        return new ManufacturingTask(taskId, taskName, arrivalTime, burstTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ManufacturingTask other)) return false;
        return Objects.equals(taskId, other.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return String.format("Task{id='%s', name='%s', AT=%d, BT=%d, status=%s}",
            taskId, taskName, arrivalTime, burstTime, status);
    }
}
