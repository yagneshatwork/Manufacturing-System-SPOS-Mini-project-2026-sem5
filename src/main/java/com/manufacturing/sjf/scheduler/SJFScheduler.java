package com.manufacturing.sjf.scheduler;

import com.manufacturing.sjf.model.ManufacturingTask;

import java.util.*;

/**
 * Non-preemptive Shortest Job First (SJF) scheduler.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>At the current simulation clock, collect every unfinished task whose
 *       {@code arrivalTime ≤ currentTime}.</li>
 *   <li>Select the task with the smallest burst time.</li>
 *   <li>Tie-break: smallest arrival time → then lexicographically smallest
 *       task ID (ascending).</li>
 *   <li>Execute the chosen task (non-preemptive) until completion.</li>
 *   <li>If no task has arrived yet, advance the clock to the next arrival time
 *       and record an idle interval.</li>
 *   <li>Repeat until all tasks are completed.</li>
 * </ol>
 *
 * <h3>Formulas</h3>
 * <ul>
 *   <li>CT  = ST + BT</li>
 *   <li>TAT = CT − AT</li>
 *   <li>WT  = TAT − BT  (= ST − AT)</li>
 * </ul>
 */
public class SJFScheduler {

    /**
     * Schedules the given tasks using non-preemptive SJF.
     *
     * @param tasks list of manufacturing tasks (not mutated)
     * @return a {@link ScheduleResult} containing the ordered schedule and metrics
     * @throws IllegalArgumentException if any task has invalid data
     */
    public ScheduleResult schedule(List<ManufacturingTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return new ScheduleResult(Collections.emptyList(), Collections.emptyList());
        }

        // Validate all tasks first
        for (ManufacturingTask t : tasks) {
            ManufacturingTask.validate(t.getTaskId(), t.getArrivalTime(), t.getBurstTime());
        }

        // Working copy sorted by arrival time for easy look-ahead
        List<ManufacturingTask> remaining = new ArrayList<>(tasks);
        remaining.sort(Comparator.comparingInt(ManufacturingTask::getArrivalTime));

        List<ScheduledTask> result        = new ArrayList<>();
        List<ScheduleResult.IdleInterval> idle = new ArrayList<>();

        int currentTime   = 0;
        int executionOrder = 1;

        // Start clock at the earliest arrival time
        currentTime = remaining.get(0).getArrivalTime();

        while (!remaining.isEmpty()) {
            // Collect eligible tasks (arrived at or before currentTime)
            final int ct = currentTime;
            List<ManufacturingTask> eligible = remaining.stream()
                .filter(t -> t.getArrivalTime() <= ct)
                .toList();

            if (eligible.isEmpty()) {
                // CPU is idle — advance to the next arrival
                int nextArrival = remaining.stream()
                    .mapToInt(ManufacturingTask::getArrivalTime)
                    .min()
                    .orElse(currentTime);
                idle.add(new ScheduleResult.IdleInterval(currentTime, nextArrival));
                currentTime = nextArrival;
                continue;
            }

            // Select the shortest job with deterministic tie-breaking
            ManufacturingTask chosen = eligible.stream()
                .min(Comparator
                    .comparingInt(ManufacturingTask::getBurstTime)
                    .thenComparingInt(ManufacturingTask::getArrivalTime)
                    .thenComparing(ManufacturingTask::getTaskId))
                .orElseThrow();

            // Schedule the chosen task
            ScheduledTask scheduled = new ScheduledTask(chosen, currentTime, executionOrder++);
            result.add(scheduled);
            currentTime = scheduled.getCompletionTime();

            remaining.remove(chosen);
        }

        return new ScheduleResult(result, idle);
    }

    /**
     * Schedules the given tasks using First-Come-First-Served (FCFS).
     * Used for the analytics comparison panel.
     *
     * @param tasks list of manufacturing tasks (not mutated)
     * @return a {@link ScheduleResult} for FCFS ordering
     */
    public ScheduleResult scheduleFCFS(List<ManufacturingTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return new ScheduleResult(Collections.emptyList(), Collections.emptyList());
        }

        List<ManufacturingTask> ordered = new ArrayList<>(tasks);
        // FCFS: sort by arrival time; tie-break by task ID
        ordered.sort(Comparator
            .comparingInt(ManufacturingTask::getArrivalTime)
            .thenComparing(ManufacturingTask::getTaskId));

        List<ScheduledTask> result        = new ArrayList<>();
        List<ScheduleResult.IdleInterval> idle = new ArrayList<>();

        int currentTime    = ordered.get(0).getArrivalTime();
        int executionOrder = 1;

        for (ManufacturingTask t : ordered) {
            if (t.getArrivalTime() > currentTime) {
                idle.add(new ScheduleResult.IdleInterval(currentTime, t.getArrivalTime()));
                currentTime = t.getArrivalTime();
            }
            ScheduledTask scheduled = new ScheduledTask(t, currentTime, executionOrder++);
            result.add(scheduled);
            currentTime = scheduled.getCompletionTime();
        }

        return new ScheduleResult(result, idle);
    }
}
