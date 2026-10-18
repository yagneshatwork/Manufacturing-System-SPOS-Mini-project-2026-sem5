package com.manufacturing.sjf.scheduler;

import java.util.Collections;
import java.util.List;

/**
 * Immutable aggregate result returned by {@link SJFScheduler}.
 *
 * <p>Contains the ordered list of scheduled tasks, idle intervals recorded
 * during the simulation, and derived aggregate metrics.</p>
 */
public final class ScheduleResult {

    /**
     * An idle interval when the machine was waiting for the next task to arrive.
     */
    public record IdleInterval(int startTime, int endTime) {
        public int duration() { return endTime - startTime; }
    }

    // ─── Fields ───────────────────────────────────────────────────────────────

    private final List<ScheduledTask> scheduledTasks;   // in execution order
    private final List<IdleInterval>  idleIntervals;    // CPU idle gaps
    private final double averageWaitingTime;
    private final double averageTurnaroundTime;
    private final int    makespan;                      // last CT − first simulation time
    private final double machineUtilization;            // total busy / makespan × 100
    private final int    totalBusyTime;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public ScheduleResult(List<ScheduledTask> scheduledTasks,
                          List<IdleInterval>  idleIntervals) {
        this.scheduledTasks  = Collections.unmodifiableList(scheduledTasks);
        this.idleIntervals   = Collections.unmodifiableList(idleIntervals);

        if (scheduledTasks.isEmpty()) {
            this.averageWaitingTime    = 0;
            this.averageTurnaroundTime = 0;
            this.makespan              = 0;
            this.machineUtilization    = 0;
            this.totalBusyTime         = 0;
        } else {
            int sumWT  = scheduledTasks.stream().mapToInt(ScheduledTask::getWaitingTime).sum();
            int sumTAT = scheduledTasks.stream().mapToInt(ScheduledTask::getTurnaroundTime).sum();
            int n      = scheduledTasks.size();

            this.averageWaitingTime    = (double) sumWT  / n;
            this.averageTurnaroundTime = (double) sumTAT / n;

            int firstStart  = scheduledTasks.stream()
                .mapToInt(ScheduledTask::getStartTime).min().orElse(0);
            int lastFinish  = scheduledTasks.stream()
                .mapToInt(ScheduledTask::getCompletionTime).max().orElse(0);
            this.makespan   = lastFinish - firstStart;

            this.totalBusyTime = scheduledTasks.stream()
                .mapToInt(ScheduledTask::getBurstTime).sum();
            this.machineUtilization = (makespan == 0) ? 0.0
                : (double) totalBusyTime / makespan * 100.0;
        }
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public List<ScheduledTask> getScheduledTasks()  { return scheduledTasks; }
    public List<IdleInterval>  getIdleIntervals()   { return idleIntervals; }
    public double getAverageWaitingTime()            { return averageWaitingTime; }
    public double getAverageTurnaroundTime()         { return averageTurnaroundTime; }
    public int    getMakespan()                      { return makespan; }
    public double getMachineUtilization()            { return machineUtilization; }
    public int    getTotalBusyTime()                 { return totalBusyTime; }
    public int    getTotalIdleTime()                 {
        return idleIntervals.stream().mapToInt(IdleInterval::duration).sum();
    }

    public int getTaskCount() { return scheduledTasks.size(); }

    /** Returns the ScheduledTask for the given (1-based) execution order, or null. */
    public ScheduledTask getByOrder(int order) {
        return scheduledTasks.stream()
            .filter(t -> t.getExecutionOrder() == order)
            .findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return String.format(
            "ScheduleResult{tasks=%d, makespan=%d, avgWT=%.2f, avgTAT=%.2f, util=%.1f%%}",
            scheduledTasks.size(), makespan, averageWaitingTime,
            averageTurnaroundTime, machineUtilization);
    }
}
