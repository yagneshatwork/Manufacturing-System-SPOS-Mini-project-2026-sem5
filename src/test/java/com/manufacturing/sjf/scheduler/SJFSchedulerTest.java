package com.manufacturing.sjf.scheduler;

import com.manufacturing.sjf.model.ManufacturingTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for {@link SJFScheduler}.
 *
 * <p>Tests cover: empty input, single task, multiple tasks arriving at t=0,
 * different arrival times, idle intervals, tie-breaking, the required sample
 * dataset, aggregate metrics, and edge cases.</p>
 */
@DisplayName("SJFScheduler Tests")
class SJFSchedulerTest {

    private SJFScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SJFScheduler();
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private static ManufacturingTask t(String id, int at, int bt) {
        return new ManufacturingTask(id, id, at, bt);
    }

    // ─── 1. Empty input ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Empty task list returns empty result")
    void testEmptyTaskList() {
        ScheduleResult result = scheduler.schedule(Collections.emptyList());
        assertTrue(result.getScheduledTasks().isEmpty());
        assertTrue(result.getIdleIntervals().isEmpty());
        assertEquals(0, result.getMakespan());
        assertEquals(0.0, result.getAverageWaitingTime(), 0.001);
        assertEquals(0.0, result.getAverageTurnaroundTime(), 0.001);
    }

    @Test
    @DisplayName("Null input returns empty result")
    void testNullInput() {
        ScheduleResult result = scheduler.schedule(null);
        assertTrue(result.getScheduledTasks().isEmpty());
    }

    // ─── 2. Single task ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Single task scheduled correctly")
    void testSingleTask() {
        ScheduleResult result = scheduler.schedule(List.of(t("T1", 0, 5)));
        assertEquals(1, result.getScheduledTasks().size());

        ScheduledTask st = result.getScheduledTasks().get(0);
        assertEquals("T1", st.getTaskId());
        assertEquals(0, st.getStartTime());
        assertEquals(5, st.getCompletionTime());
        assertEquals(0, st.getWaitingTime());
        assertEquals(5, st.getTurnaroundTime());
        assertEquals(5, result.getMakespan());
        assertEquals(100.0, result.getMachineUtilization(), 0.001);
    }

    @Test
    @DisplayName("Single task with non-zero arrival time")
    void testSingleTaskWithArrivalTime() {
        ScheduleResult result = scheduler.schedule(List.of(t("T1", 3, 4)));

        ScheduledTask st = result.getScheduledTasks().get(0);
        assertEquals(3, st.getStartTime());
        assertEquals(7, st.getCompletionTime());
        assertEquals(0, st.getWaitingTime());   // WT = ST - AT = 3 - 3 = 0
        assertEquals(4, st.getTurnaroundTime()); // TAT = CT - AT = 7 - 3 = 4
    }

    // ─── 3. Multiple tasks arriving at t=0 ────────────────────────────────────

    @Test
    @DisplayName("Multiple tasks at t=0 ordered by burst time")
    void testMultipleAtTimeZero() {
        List<ManufacturingTask> tasks = List.of(
            t("A", 0, 6),
            t("B", 0, 2),
            t("C", 0, 4)
        );
        ScheduleResult result = scheduler.schedule(tasks);

        List<ScheduledTask> scheduled = result.getScheduledTasks();
        assertEquals(3, scheduled.size());
        // SJF order: B(2) → C(4) → A(6)
        assertEquals("B", scheduled.get(0).getTaskId());
        assertEquals("C", scheduled.get(1).getTaskId());
        assertEquals("A", scheduled.get(2).getTaskId());
    }

    // ─── 4. Required sample dataset ───────────────────────────────────────────

    @Test
    @DisplayName("Sample dataset: J2→J4→J1→J3 with correct metrics")
    void testSampleDataset() {
        List<ManufacturingTask> tasks = List.of(
            new ManufacturingTask("J1", "Component Assembly", 0, 6),
            new ManufacturingTask("J2", "Quality Inspection", 0, 2),
            new ManufacturingTask("J3", "Packaging",          0, 8),
            new ManufacturingTask("J4", "Label Printing",     0, 3)
        );

        ScheduleResult result = scheduler.schedule(tasks);
        List<ScheduledTask> scheduled = result.getScheduledTasks();

        // Execution order
        assertEquals("J2", scheduled.get(0).getTaskId());
        assertEquals("J4", scheduled.get(1).getTaskId());
        assertEquals("J1", scheduled.get(2).getTaskId());
        assertEquals("J3", scheduled.get(3).getTaskId());

        // J2: ST=0, CT=2, WT=0, TAT=2
        assertTask(scheduled.get(0), 0, 2, 0, 2);
        // J4: ST=2, CT=5, WT=2, TAT=5
        assertTask(scheduled.get(1), 2, 5, 2, 5);
        // J1: ST=5, CT=11, WT=5, TAT=11
        assertTask(scheduled.get(2), 5, 11, 5, 11);
        // J3: ST=11, CT=19, WT=11, TAT=19
        assertTask(scheduled.get(3), 11, 19, 11, 19);

        assertEquals(4.5,  result.getAverageWaitingTime(),    0.001);
        assertEquals(9.25, result.getAverageTurnaroundTime(), 0.001);
        assertEquals(19,   result.getMakespan());
        assertEquals(100.0, result.getMachineUtilization(),   0.001);
        assertTrue(result.getIdleIntervals().isEmpty());
    }

    // ─── 5. Different arrival times ───────────────────────────────────────────

    @Test
    @DisplayName("Tasks with different arrival times scheduled correctly")
    void testDifferentArrivalTimes() {
        List<ManufacturingTask> tasks = List.of(
            t("A", 0,  8),
            t("B", 1,  4),
            t("C", 2,  9),
            t("D", 3,  5)
        );
        // Non-preemptive SJF:
        //  t=0: only A eligible → run A (0→8)
        //  t=8: B(4), C(9), D(5) eligible → shortest is B → run B (8→12)
        //  t=12: C(9), D(5) → shortest is D → run D (12→17)
        //  t=17: C(9) → run C (17→26)
        ScheduleResult result = scheduler.schedule(tasks);
        List<ScheduledTask> scheduled = result.getScheduledTasks();

        assertEquals("A", scheduled.get(0).getTaskId());
        assertEquals("B", scheduled.get(1).getTaskId());
        assertEquals("D", scheduled.get(2).getTaskId());
        assertEquals("C", scheduled.get(3).getTaskId());

        assertEquals(0,  scheduled.get(0).getStartTime());
        assertEquals(8,  scheduled.get(1).getStartTime());
        assertEquals(12, scheduled.get(2).getStartTime());
        assertEquals(17, scheduled.get(3).getStartTime());
    }

    // ─── 6. Idle time before the first task arrives ───────────────────────────

    @Test
    @DisplayName("Idle time when first task arrives after t=0")
    void testIdleTimeBeforeFirstTask() {
        // Only one task, arrives at t=5
        ScheduleResult result = scheduler.schedule(List.of(t("T1", 5, 3)));

        List<ScheduleResult.IdleInterval> idle = result.getIdleIntervals();
        // No idle recorded — clock starts at first arrival time (5), not at 0
        // So no idle interval before the first task
        ScheduledTask st = result.getScheduledTasks().get(0);
        assertEquals(5, st.getStartTime());
        assertEquals(8, st.getCompletionTime());
    }

    @Test
    @DisplayName("Idle gap between two non-contiguous tasks")
    void testIdleGapBetweenTasks() {
        List<ManufacturingTask> tasks = List.of(
            t("A", 0, 3),   // finishes at 3
            t("B", 7, 2)    // arrives at 7 → idle 3→7
        );
        ScheduleResult result = scheduler.schedule(tasks);

        List<ScheduleResult.IdleInterval> idle = result.getIdleIntervals();
        assertEquals(1, idle.size());
        assertEquals(3, idle.get(0).startTime());
        assertEquals(7, idle.get(0).endTime());
        assertEquals(4, idle.get(0).duration());

        // Makespan = 9 - 0 = 9 (first start to last finish)
        assertEquals(9, result.getMakespan());
        // Utilization = 5 / 9 * 100
        assertEquals(5.0 / 9.0 * 100, result.getMachineUtilization(), 0.001);
    }

    // ─── 7. Equal burst times — tie-breaking ──────────────────────────────────

    @Test
    @DisplayName("Equal burst times: tie-break by arrival time")
    void testEqualBurstTimesTieBreakByArrival() {
        List<ManufacturingTask> tasks = List.of(
            t("A", 2, 5),
            t("B", 0, 5),
            t("C", 1, 5)
        );
        ScheduleResult result = scheduler.schedule(tasks);
        List<ScheduledTask> scheduled = result.getScheduledTasks();

        // All burst times equal; at t=0 only B is available
        assertEquals("B", scheduled.get(0).getTaskId()); // earliest arrival
        // At t=5: A(AT=2) and C(AT=1) eligible — equal burst, C has earlier arrival
        assertEquals("C", scheduled.get(1).getTaskId());
        assertEquals("A", scheduled.get(2).getTaskId());
    }

    @Test
    @DisplayName("Equal burst and arrival times: tie-break by task ID (lexicographic)")
    void testEqualBurstAndArrivalTieBreakById() {
        List<ManufacturingTask> tasks = List.of(
            t("C", 0, 4),
            t("A", 0, 4),
            t("B", 0, 4)
        );
        ScheduleResult result = scheduler.schedule(tasks);
        List<ScheduledTask> scheduled = result.getScheduledTasks();

        assertEquals("A", scheduled.get(0).getTaskId()); // lexicographically smallest
        assertEquals("B", scheduled.get(1).getTaskId());
        assertEquals("C", scheduled.get(2).getTaskId());
    }

    // ─── 8. Correct completion / waiting / turnaround ─────────────────────────

    @Test
    @DisplayName("Waiting time equals start time minus arrival time")
    void testWaitingTimeFormula() {
        List<ManufacturingTask> tasks = List.of(
            t("X", 0, 10),
            t("Y", 0, 3)
        );
        ScheduleResult result = scheduler.schedule(tasks);
        // Y runs first (shorter)
        ScheduledTask y = result.getScheduledTasks().get(0);
        ScheduledTask x = result.getScheduledTasks().get(1);

        assertEquals("Y", y.getTaskId());
        assertEquals(0,  y.getWaitingTime()); // ST(0) - AT(0)
        assertEquals(3,  x.getWaitingTime()); // ST(3) - AT(0)
    }

    // ─── 9. Aggregate metrics ─────────────────────────────────────────────────

    @Test
    @DisplayName("Average waiting time computed correctly")
    void testAverageWaitingTime() {
        // Two tasks at t=0: BT=4, BT=6  → order: 4(0-4), 6(4-10)
        // WT: 0, 4  → avg = 2.0
        List<ManufacturingTask> tasks = List.of(t("A", 0, 4), t("B", 0, 6));
        ScheduleResult result = scheduler.schedule(tasks);
        assertEquals(2.0, result.getAverageWaitingTime(), 0.001);
    }

    @Test
    @DisplayName("Average turnaround time computed correctly")
    void testAverageTurnaroundTime() {
        // tasks: A(BT=4), B(BT=6) at t=0
        // TAT: A=4, B=10 → avg = 7.0
        List<ManufacturingTask> tasks = List.of(t("A", 0, 4), t("B", 0, 6));
        ScheduleResult result = scheduler.schedule(tasks);
        assertEquals(7.0, result.getAverageTurnaroundTime(), 0.001);
    }

    @Test
    @DisplayName("Makespan equals last completion minus first start")
    void testMakespan() {
        List<ManufacturingTask> tasks = List.of(t("A", 0, 5), t("B", 0, 3));
        ScheduleResult result = scheduler.schedule(tasks);
        assertEquals(8, result.getMakespan()); // first start=0, last finish=8
    }

    @Test
    @DisplayName("Machine utilization is 100 percent when no idle time")
    void testMachineUtilization100() {
        List<ManufacturingTask> tasks = List.of(t("A", 0, 3), t("B", 0, 4));
        ScheduleResult result = scheduler.schedule(tasks);
        assertEquals(100.0, result.getMachineUtilization(), 0.001);
    }

    @Test
    @DisplayName("Machine utilization less than 100 percent when idle time exists")
    void testMachineUtilizationWithIdle() {
        List<ManufacturingTask> tasks = List.of(
            t("A", 0, 2),   // finishes at 2
            t("B", 6, 2)    // arrives at 6 → idle 2→6
        );
        ScheduleResult result = scheduler.schedule(tasks);
        // busy=4, makespan=8 → 50%
        assertEquals(50.0, result.getMachineUtilization(), 0.001);
    }

    // ─── 10. Invalid input ────────────────────────────────────────────────────

    @Test
    @DisplayName("Negative arrival time throws IllegalArgumentException")
    void testNegativeArrivalTime() {
        assertThrows(IllegalArgumentException.class,
            () -> new ManufacturingTask("T1", "Task", -1, 5));
    }

    @Test
    @DisplayName("Zero burst time throws IllegalArgumentException")
    void testZeroBurstTime() {
        assertThrows(IllegalArgumentException.class,
            () -> new ManufacturingTask("T1", "Task", 0, 0));
    }

    @Test
    @DisplayName("Negative burst time throws IllegalArgumentException")
    void testNegativeBurstTime() {
        assertThrows(IllegalArgumentException.class,
            () -> new ManufacturingTask("T1", "Task", 0, -3));
    }

    @Test
    @DisplayName("Blank task ID throws IllegalArgumentException")
    void testBlankTaskId() {
        assertThrows(IllegalArgumentException.class,
            () -> new ManufacturingTask("   ", "Task", 0, 5));
    }

    @Test
    @DisplayName("Null task ID throws IllegalArgumentException")
    void testNullTaskId() {
        assertThrows(IllegalArgumentException.class,
            () -> new ManufacturingTask(null, "Task", 0, 5));
    }

    // ─── 11. Large burst times ────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 99999, Integer.MAX_VALUE / 2})
    @DisplayName("Large burst times handled without overflow")
    void testLargeBurstTimes(int burstTime) {
        ScheduleResult result = scheduler.schedule(List.of(t("BIG", 0, burstTime)));
        assertEquals(1, result.getScheduledTasks().size());
        assertEquals(burstTime, result.getScheduledTasks().get(0).getBurstTime());
        assertEquals(burstTime, result.getMakespan());
    }

    // ─── 12. Scheduling order ─────────────────────────────────────────────────

    @Test
    @DisplayName("Execution order is correctly numbered starting from 1")
    void testExecutionOrderNumbering() {
        List<ManufacturingTask> tasks = List.of(t("A", 0, 3), t("B", 0, 1), t("C", 0, 2));
        ScheduleResult result = scheduler.schedule(tasks);
        List<ScheduledTask> scheduled = result.getScheduledTasks();

        for (int i = 0; i < scheduled.size(); i++) {
            assertEquals(i + 1, scheduled.get(i).getExecutionOrder());
        }
    }

    // ─── 13. Three tasks mixed arrivals and burst times ───────────────────────

    @Test
    @DisplayName("Mixed arrival and burst times produce correct order")
    void testMixedArrivalAndBurst() {
        // A: AT=0, BT=6; B: AT=2, BT=4; C: AT=4, BT=2
        // t=0: only A → run A (0→6)
        // t=6: B(4) and C(2) eligible → C shortest → run C (6→8)
        // t=8: B → run B (8→12)
        List<ManufacturingTask> tasks = List.of(
            t("A", 0, 6), t("B", 2, 4), t("C", 4, 2)
        );
        ScheduleResult result = scheduler.schedule(tasks);
        List<ScheduledTask> scheduled = result.getScheduledTasks();

        assertEquals("A", scheduled.get(0).getTaskId());
        assertEquals("C", scheduled.get(1).getTaskId());
        assertEquals("B", scheduled.get(2).getTaskId());

        assertEquals(6,  scheduled.get(1).getStartTime());
        assertEquals(8,  scheduled.get(1).getCompletionTime());
        assertEquals(8,  scheduled.get(2).getStartTime());
        assertEquals(12, scheduled.get(2).getCompletionTime());
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private void assertTask(ScheduledTask st, int startTime, int completionTime,
                            int waitingTime, int turnaroundTime) {
        assertEquals(startTime,      st.getStartTime(),      "ST mismatch for " + st.getTaskId());
        assertEquals(completionTime, st.getCompletionTime(), "CT mismatch for " + st.getTaskId());
        assertEquals(waitingTime,    st.getWaitingTime(),    "WT mismatch for " + st.getTaskId());
        assertEquals(turnaroundTime, st.getTurnaroundTime(), "TAT mismatch for " + st.getTaskId());
    }
}
