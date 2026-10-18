package com.manufacturing.sjf.simulation;

import com.manufacturing.sjf.model.ManufacturingTask;
import com.manufacturing.sjf.model.TaskStatus;
import com.manufacturing.sjf.scheduler.ScheduleResult;
import com.manufacturing.sjf.scheduler.ScheduledTask;

import javax.swing.Timer;
import java.util.*;
import java.util.function.Consumer;

/**
 * Drives the step-by-step visual animation of the SJF schedule.
 *
 * <p>The scheduler produces the complete {@link ScheduleResult} up front.
 * This simulator then reveals the execution time-line progressively,
 * advancing an internal simulation clock via a Swing Timer so that all
 * UI updates happen on the EDT.</p>
 *
 * <h3>State machine</h3>
 * <pre>
 *   IDLE ──run()──► RUNNING ──pause()──► PAUSED
 *                      ▲                    │
 *                      └────resume()─────────┘
 *   Any state ──reset()──► IDLE
 * </pre>
 */
public class ProductionSimulator {

    public enum State { IDLE, RUNNING, PAUSED, COMPLETED }

    // ─── Configuration ────────────────────────────────────────────────────────

    /** How many simulation-time units advance per timer tick. */
    private int timeUnitsPerTick = 1;
    /** Timer interval in milliseconds. */
    private int timerDelayMs     = 80;

    // ─── State ────────────────────────────────────────────────────────────────

    private State state = State.IDLE;

    /** Full schedule computed once by SJFScheduler. */
    private ScheduleResult scheduleResult;
    /** Working copies of the original tasks (mutated for status display). */
    private List<ManufacturingTask> tasks = new ArrayList<>();

    /** Current simulation clock value (in time units). */
    private int simTime = 0;
    /** Clock at the start of the simulation (= earliest arrival time). */
    private int startTime = 0;
    /** The task currently on the machine (null if idle or not started). */
    private ScheduledTask currentTask;
    /** Index of the next task to start (in execution-order list). */
    private int nextTaskIndex = 0;

    /** Completed scheduled tasks so far. */
    private final List<ScheduledTask> completedTasks = new ArrayList<>();

    private javax.swing.Timer swingTimer;

    // ─── Callbacks ────────────────────────────────────────────────────────────

    /** Called after each tick or step with the current simulator snapshot. */
    private Consumer<ProductionSimulator> onTick;
    /** Called when all tasks are finished. */
    private Consumer<ProductionSimulator> onComplete;

    // ─── Initialisation ───────────────────────────────────────────────────────

    public void init(List<ManufacturingTask> tasks, ScheduleResult result) {
        reset();
        this.scheduleResult = result;
        this.tasks = new ArrayList<>(tasks);
        if (!result.getScheduledTasks().isEmpty()) {
            // Clock starts at the first scheduled task's start time
            this.startTime = result.getScheduledTasks().get(0).getStartTime();
            this.simTime   = startTime;
        }
        this.state = State.IDLE;
    }

    // ─── Controls ─────────────────────────────────────────────────────────────

    public void run() {
        if (state == State.IDLE || state == State.PAUSED) {
            state = State.RUNNING;
            startTimer();
        }
    }

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            stopTimer();
        }
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
            startTimer();
        }
    }

    public void stepForward() {
        if (state == State.IDLE || state == State.PAUSED) {
            // Ensure we are at least in paused-start
            if (state == State.IDLE) state = State.PAUSED;
            tick();
            notifyTick();
        }
    }

    public void reset() {
        stopTimer();
        state          = State.IDLE;
        simTime        = 0;
        startTime      = 0;
        currentTask    = null;
        nextTaskIndex  = 0;
        completedTasks.clear();
        if (tasks != null) {
            for (ManufacturingTask t : tasks) {
                t.setStatus(TaskStatus.PENDING);
            }
        }
    }

    // ─── Internal Timer ───────────────────────────────────────────────────────

    private void startTimer() {
        if (swingTimer != null && swingTimer.isRunning()) return;
        swingTimer = new Timer(timerDelayMs, e -> {
            tick();
            notifyTick();
            if (state == State.COMPLETED) {
                stopTimer();
                if (onComplete != null) onComplete.accept(this);
            }
        });
        swingTimer.start();
    }

    private void stopTimer() {
        if (swingTimer != null) {
            swingTimer.stop();
            swingTimer = null;
        }
    }

    /** Advances the simulation clock by one tick (timeUnitsPerTick). */
    private void tick() {
        if (scheduleResult == null) return;
        List<ScheduledTask> ordered = scheduleResult.getScheduledTasks();
        if (nextTaskIndex >= ordered.size()) {
            state = State.COMPLETED;
            return;
        }

        simTime += timeUnitsPerTick;

        // Complete any tasks whose completion time ≤ simTime
        while (currentTask != null && simTime >= currentTask.getCompletionTime()) {
            completedTasks.add(currentTask);
            setTaskStatus(currentTask.getTaskId(), TaskStatus.COMPLETED);
            currentTask = null;
            nextTaskIndex++;
        }

        // Start the next task if its start time ≤ simTime
        if (currentTask == null && nextTaskIndex < ordered.size()) {
            ScheduledTask next = ordered.get(nextTaskIndex);
            if (simTime >= next.getStartTime()) {
                currentTask = next;
                setTaskStatus(next.getTaskId(), TaskStatus.PROCESSING);
            }
        }

        // Mark READY tasks (arrived but not yet processing)
        for (ScheduledTask st : ordered) {
            ManufacturingTask mt = findTask(st.getTaskId());
            if (mt != null && mt.getStatus() == TaskStatus.PENDING
                    && st.getArrivalTime() <= simTime) {
                mt.setStatus(TaskStatus.READY);
            }
        }

        if (nextTaskIndex >= ordered.size() && currentTask == null) {
            state = State.COMPLETED;
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void setTaskStatus(String taskId, TaskStatus status) {
        ManufacturingTask t = findTask(taskId);
        if (t != null) t.setStatus(status);
    }

    private ManufacturingTask findTask(String taskId) {
        return tasks.stream()
            .filter(t -> t.getTaskId().equals(taskId))
            .findFirst().orElse(null);
    }

    private void notifyTick() {
        if (onTick != null) onTick.accept(this);
    }

    /** Returns processing progress [0.0, 1.0] of the current task. */
    public double getCurrentTaskProgress() {
        if (currentTask == null) return 0.0;
        int elapsed = simTime - currentTask.getStartTime();
        int total   = currentTask.getBurstTime();
        return Math.min(1.0, (double) elapsed / total);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public State         getState()           { return state; }
    public int           getSimTime()         { return simTime; }
    public ScheduledTask getCurrentTask()     { return currentTask; }
    public int           getNextTaskIndex()   { return nextTaskIndex; }
    public ScheduleResult getScheduleResult() { return scheduleResult; }
    public List<ScheduledTask> getCompletedTasks() {
        return Collections.unmodifiableList(completedTasks);
    }
    public List<ManufacturingTask> getTasks() { return Collections.unmodifiableList(tasks); }

    /** Returns the next task to be scheduled (peek, not consumed). */
    public ScheduledTask getNextTask() {
        if (scheduleResult == null) return null;
        List<ScheduledTask> ordered = scheduleResult.getScheduledTasks();
        if (nextTaskIndex < ordered.size() && currentTask == null) {
            return ordered.get(nextTaskIndex);
        }
        return null;
    }

    public boolean isInitialised() { return scheduleResult != null; }

    // ─── Configuration setters ────────────────────────────────────────────────

    public void setOnTick(Consumer<ProductionSimulator> onTick) {
        this.onTick = onTick;
    }

    public void setOnComplete(Consumer<ProductionSimulator> onComplete) {
        this.onComplete = onComplete;
    }

    public void setTimerDelayMs(int ms) {
        this.timerDelayMs = Math.max(20, ms);
        if (swingTimer != null && swingTimer.isRunning()) {
            swingTimer.setDelay(this.timerDelayMs);
        }
    }

    public int getTimerDelayMs() { return timerDelayMs; }
}
