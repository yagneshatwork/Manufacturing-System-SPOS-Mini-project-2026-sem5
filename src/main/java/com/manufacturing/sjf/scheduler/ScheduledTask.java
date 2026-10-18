package com.manufacturing.sjf.scheduler;

import com.manufacturing.sjf.model.ManufacturingTask;

/**
 * Immutable value object that holds the computed scheduling results for a
 * single {@link ManufacturingTask} after the SJF scheduler has run.
 *
 * <p>Formula summary:</p>
 * <ul>
 *   <li>CT  = ST + BT</li>
 *   <li>TAT = CT − AT</li>
 *   <li>WT  = TAT − BT</li>
 * </ul>
 */
public final class ScheduledTask {

    private final ManufacturingTask task;   // original task (not mutated)
    private final int startTime;            // when the machine begins processing
    private final int completionTime;       // when the machine finishes processing
    private final int waitingTime;          // WT = TAT - BT
    private final int turnaroundTime;       // TAT = CT - AT
    private final int executionOrder;       // 1-based position in the schedule

    public ScheduledTask(ManufacturingTask task, int startTime, int executionOrder) {
        this.task           = task;
        this.startTime      = startTime;
        this.completionTime = startTime + task.getBurstTime();
        this.turnaroundTime = this.completionTime - task.getArrivalTime();
        this.waitingTime    = this.turnaroundTime - task.getBurstTime();
        this.executionOrder = executionOrder;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public ManufacturingTask getTask() { return task; }

    public String getTaskId()   { return task.getTaskId(); }
    public String getTaskName() { return task.getTaskName(); }
    public int getArrivalTime() { return task.getArrivalTime(); }
    public int getBurstTime()   { return task.getBurstTime(); }

    public int getStartTime()       { return startTime; }
    public int getCompletionTime()  { return completionTime; }
    public int getWaitingTime()     { return waitingTime; }
    public int getTurnaroundTime()  { return turnaroundTime; }
    public int getExecutionOrder()  { return executionOrder; }

    @Override
    public String toString() {
        return String.format(
            "ScheduledTask{id='%s', order=%d, ST=%d, CT=%d, WT=%d, TAT=%d}",
            getTaskId(), executionOrder, startTime, completionTime,
            waitingTime, turnaroundTime);
    }
}
