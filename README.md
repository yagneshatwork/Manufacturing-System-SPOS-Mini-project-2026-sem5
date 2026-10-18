# 🏭 Smart Manufacturing Production Scheduler

A fully functional desktop application demonstrating **Non-Preemptive Shortest Job First (SJF)** CPU scheduling applied to a manufacturing production-line context. Built with **Java 17 + Java Swing**, it features a complete industrial dark-theme dashboard, animated production line, Gantt chart, burst-time visualization, and analytics with FCFS vs SJF comparison.

---

## 📋 Table of Contents
1. [Features](#features)
2. [Project Structure](#project-structure)
3. [Prerequisites](#prerequisites)
4. [Build & Run](#build--run)
5. [Algorithm Details](#algorithm-details)
6. [Sample Dataset](#sample-dataset)
7. [UI Guide](#ui-guide)
8. [Testing](#testing)
9. [Design Decisions](#design-decisions)
10. [Known Limitations](#known-limitations)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| **Task Management** | Add, update, delete tasks with ID, name, arrival time, burst time |
| **SJF Scheduler** | Non-preemptive SJF with deterministic tie-breaking |
| **Production Line** | 4-zone animated factory: Input → SJF Queue → Machine → Completed |
| **Burst-Time Chart** | Horizontal bar chart (Graphics2D), highlights shortest task |
| **Gantt Chart** | Proportional execution timeline with idle blocks and time cursor |
| **Analytics** | Per-task metrics + FCFS vs SJF comparison table |
| **Simulation Controls** | Run / Pause / Resume / Step Forward / Reset / Speed slider |
| **Stat Cards** | Real-time: total tasks, pending, completed, avg WT, avg TAT, utilization |
| **Sample Data** | One-click load of 4 canonical test tasks |
| **Dark Theme** | Industrial control-room aesthetic with custom colour palette |
| **Unit Tests** | 20+ JUnit 5 tests covering all algorithm edge cases |

---

## 📁 Project Structure

```
manufacturing-sjf/
├── pom.xml
├── README.md
└── src/
    ├── main/java/com/manufacturing/sjf/
    │   ├── Main.java                         # Entry point
    │   ├── model/
    │   │   ├── ManufacturingTask.java         # Core task data model
    │   │   └── TaskStatus.java                # Lifecycle enum
    │   ├── scheduler/
    │   │   ├── SJFScheduler.java              # Non-preemptive SJF + FCFS
    │   │   ├── ScheduleResult.java            # Aggregate results + metrics
    │   │   └── ScheduledTask.java             # Per-task computed values
    │   ├── simulation/
    │   │   └── ProductionSimulator.java       # Swing Timer animation engine
    │   ├── ui/
    │   │   ├── MainFrame.java                 # Root JFrame + wiring
    │   │   ├── TaskTablePanel.java            # CRUD form + JTable
    │   │   ├── ProductionLinePanel.java       # 4-zone factory animation
    │   │   ├── BurstTimeChart.java            # Horizontal bar chart
    │   │   ├── GanttChartPanel.java           # Proportional Gantt chart
    │   │   └── AnalyticsPanel.java            # Metrics + comparison
    │   └── util/
    │       └── Theme.java                     # Design tokens + factories
    └── test/java/com/manufacturing/sjf/
        └── scheduler/
            └── SJFSchedulerTest.java          # JUnit 5 test suite
```

---

## ⚙ Prerequisites

| Requirement | Version |
|-------------|---------|
| Java JDK    | 17 or newer |
| Apache Maven | 3.8+ |

> **Windows tip**: If Maven is not on your PATH, download it from https://maven.apache.org/download.cgi  
> and run commands using the full path to `mvn.cmd`.

---

## 🚀 Build & Run

### Compile & test
```bash
mvn test
```

### Run the application
```bash
mvn exec:java
```

### Build runnable JAR
```bash
mvn package
java -jar target/manufacturing-sjf-runnable.jar
```

### Using downloaded Maven on Windows (if not on PATH)
```powershell
$env:PATH = "C:\tools\apache-maven-3.9.9\bin;$env:PATH"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.10.7-hotspot"
mvn exec:java
```

---

## 🧠 Algorithm Details

### Non-Preemptive SJF

At each scheduling decision point:

1. Collect all unfinished tasks with `arrivalTime ≤ currentTime`.
2. Select the task with the **smallest burst time**.
3. **Tie-break** (deterministic, documented order):
   - Smallest **arrival time** first.
   - If still tied: lexicographically smallest **task ID** (ascending).
4. Execute the selected task to completion (non-preemptive).
5. Advance `currentTime` by the task's burst time.
6. If no task has arrived yet, advance `currentTime` to the next arrival and record an **idle interval**.
7. Repeat until all tasks complete.

### Formulas

| Metric | Formula |
|--------|---------|
| Completion Time (CT) | `CT = ST + BT` |
| Turnaround Time (TAT) | `TAT = CT − AT` |
| Waiting Time (WT) | `WT = TAT − BT  (= ST − AT)` |
| Avg Waiting Time | `Σ WT / n` |
| Avg Turnaround Time | `Σ TAT / n` |
| Makespan | `last CT − first ST` |
| Machine Utilization | `(Σ BT / Makespan) × 100%` |

> Zero-makespan (empty schedule) → utilization reported as 0%.

---

## 📦 Sample Dataset

Click **"Load Sample"** to insert the canonical test dataset:

| Task ID | Task Name | Arrival Time | Burst Time |
|---------|-----------|--------------|------------|
| J1 | Component Assembly | 0 | 6 |
| J2 | Quality Inspection | 0 | 2 |
| J3 | Packaging | 0 | 8 |
| J4 | Label Printing | 0 | 3 |

### Expected SJF results

| Task | Execution Order | Start | Completion | Waiting | Turnaround |
|------|-----------------|-------|------------|---------|------------|
| J2 | 1 | 0 | 2 | 0 | 2 |
| J4 | 2 | 2 | 5 | 2 | 5 |
| J1 | 3 | 5 | 11 | 5 | 11 |
| J3 | 4 | 11 | 19 | 11 | 19 |

- **Avg Waiting Time:** 4.50 units  
- **Avg Turnaround Time:** 9.25 units  
- **Makespan:** 19 units  
- **Machine Utilization:** 100%

---

## 🖥 UI Guide

### Tabs

| Tab | Content |
|-----|---------|
| 📋 Tasks | Add/edit/delete tasks, view all scheduling metrics in the table |
| 🏭 Production Line | Animated 4-zone factory; run the simulation to see tasks move |
| 📈 Charts | Burst-time horizontal bar chart + proportional Gantt chart |
| 📊 Analytics | Detailed metrics cards, per-task table, FCFS vs SJF comparison |

### Simulation Controls

| Button | Function |
|--------|---------|
| ▶ Run | Compute schedule and start animation |
| ⏸ Pause | Freeze animation at current position |
| ▶ Resume | Continue from paused state |
| ⏭ Step | Advance one simulation tick (works while paused) |
| ↺ Reset | Return to initial state; re-enable task editing |
| Speed slider | 1 = slow (~500 ms/tick), 10 = fast (~20 ms/tick) |

### Colour Legend

| Colour | Meaning |
|--------|---------|
| 🟣 Grey | Pending / not yet arrived |
| 🟡 Amber | Ready / selected next |
| 🟦 Teal | Currently processing |
| 🟢 Green | Completed |
| 🔴 Red | Error / failed |

---

## 🧪 Testing

Run all tests:
```bash
mvn test
```

Test categories:
- Empty and null input
- Single task
- Multiple tasks at `t = 0`
- Tasks with different arrival times
- Idle time (before first task, between tasks)
- Equal burst time tie-breaking (by AT then task ID)
- Sample dataset validation (exact J2→J4→J1→J3 order + metrics)
- Average WT and TAT formulas
- Makespan and machine utilization
- Invalid inputs (negative AT, zero/negative BT, blank ID)
- Large burst time values

---

## 🔧 Design Decisions

1. **Scheduler is stateless** — `SJFScheduler.schedule()` takes a list and returns an immutable `ScheduleResult`. The UI and simulation only read this result.

2. **Animation is separate from scheduling** — `ProductionSimulator` reveals the pre-computed schedule tick by tick using a Swing Timer. No scheduling logic exists in the UI layer.

3. **FCFS comparison** — The `SJFScheduler.scheduleFCFS()` method runs FCFS on the same input tasks for the Analytics comparison. Clearly labelled as a simulation.

4. **EDT compliance** — All Swing updates happen via `SwingUtilities.invokeLater()`. No `Thread.sleep()` is used on the EDT.

5. **Tie-breaking is deterministic and documented** — Burst time → Arrival time → Task ID (lexicographic ascending).

---

## ⚠ Known Limitations

- Only single-machine scheduling is simulated (SJF for a single resource).
- Animation speed is limited by the Swing Timer resolution (~20 ms minimum).
- Large task counts (> 50) may make the Production Line zone crowded; scroll is supported.
- The FCFS comparison in Analytics is clearly a simulation, not a real-world performance guarantee.
