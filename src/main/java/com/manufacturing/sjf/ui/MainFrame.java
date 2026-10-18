package com.manufacturing.sjf.ui;

import com.manufacturing.sjf.model.ManufacturingTask;
import com.manufacturing.sjf.scheduler.ScheduleResult;
import com.manufacturing.sjf.scheduler.SJFScheduler;
import com.manufacturing.sjf.simulation.ProductionSimulator;
import com.manufacturing.sjf.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Root JFrame for the Smart Manufacturing System.
 *
 * <p>Layout (top-to-bottom):</p>
 * <ol>
 *   <li>Top navigation bar — title, clock, status, simulation controls</li>
 *   <li>Summary stat cards strip</li>
 *   <li>Main tabbed content area:
 *     <ul>
 *       <li>Tasks tab — form + table</li>
 *       <li>Production Line tab — 4-zone animation</li>
 *       <li>Charts tab — burst-time chart + Gantt chart</li>
 *       <li>Analytics tab — metrics + FCFS comparison</li>
 *     </ul>
 *   </li>
 * </ol>
 */
public class MainFrame extends JFrame {

    // ─── Core components ─────────────────────────────────────────────────────

    private final SJFScheduler        scheduler   = new SJFScheduler();
    private final ProductionSimulator simulator   = new ProductionSimulator();

    private final TaskTablePanel      taskPanel   = new TaskTablePanel();
    private final ProductionLinePanel prodLine    = new ProductionLinePanel();
    private final BurstTimeChart      burstChart  = new BurstTimeChart();
    private final GanttChartPanel     ganttChart  = new GanttChartPanel();
    private final AnalyticsPanel      analytics   = new AnalyticsPanel();

    // ─── Stat card labels ─────────────────────────────────────────────────────

    private final JLabel statTotal     = statValue("0");
    private final JLabel statPending   = statValue("0");
    private final JLabel statCompleted = statValue("0");
    private final JLabel statCurrent   = statValue("—");
    private final JLabel statAvgWt     = statValue("—");
    private final JLabel statAvgTat    = statValue("—");
    private final JLabel statMakespan  = statValue("—");
    private final JLabel statUtil      = statValue("—");

    // ─── Nav bar labels ───────────────────────────────────────────────────────

    private final JLabel clockLabel   = new JLabel("T = 0");
    private final JLabel statusLabel  = new JLabel("● READY");

    // ─── Sim control buttons ──────────────────────────────────────────────────

    private final JButton runBtn      = Theme.primaryButton("▶  Run");
    private final JButton pauseBtn    = Theme.secondaryButton("⏸  Pause");
    private final JButton resumeBtn   = Theme.primaryButton("▶  Resume");
    private final JButton stepBtn     = Theme.secondaryButton("⏭  Step");
    private final JButton resetBtn    = Theme.dangerButton("↺  Reset");
    private final JSlider speedSlider;

    // ─── State ────────────────────────────────────────────────────────────────

    private ScheduleResult currentSchedule;
    private List<ManufacturingTask> currentTasks;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public MainFrame() {
        super("Smart Manufacturing System — SJF Production Scheduler");

        speedSlider = buildSpeedSlider();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setBackground(Theme.BG_MAIN);

        getContentPane().setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(Theme.BG_MAIN);

        getContentPane().add(buildNavBar(),    BorderLayout.NORTH);
        getContentPane().add(buildBody(),      BorderLayout.CENTER);

        wireSimulator();
        wireTaskPanel();
        wireSimControls();
        updateButtonStates();
    }

    // ─── Navigation Bar ───────────────────────────────────────────────────────

    private JPanel buildNavBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(Theme.BG_PANEL);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
            new EmptyBorder(10, 16, 10, 16)));

        // Left: title block
        JPanel titleBlock = new JPanel(new BorderLayout());
        titleBlock.setBackground(Theme.BG_PANEL);

        JLabel title = new JLabel("🏭  SMART MANUFACTURING SYSTEM");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.ACCENT_TEAL);

        JLabel subtitle = new JLabel("SJF PRODUCTION SCHEDULER  |  Non-Preemptive Shortest Job First");
        subtitle.setFont(Theme.FONT_SUBTITLE);
        subtitle.setForeground(Theme.TEXT_SECONDARY);

        titleBlock.add(title,    BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.SOUTH);

        // Centre: simulation controls
        JPanel controls = buildControlPanel();

        // Right: clock and status
        JPanel rightPanel = new JPanel(new BorderLayout(0, 4));
        rightPanel.setBackground(Theme.BG_PANEL);

        clockLabel.setFont(Theme.FONT_MONO);
        clockLabel.setForeground(Theme.TEXT_PRIMARY);
        clockLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.ACCENT_TEAL);
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        rightPanel.add(clockLabel,  BorderLayout.NORTH);
        rightPanel.add(statusLabel, BorderLayout.SOUTH);

        bar.add(titleBlock, BorderLayout.WEST);
        bar.add(controls,   BorderLayout.CENTER);
        bar.add(rightPanel, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildControlPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        p.setBackground(Theme.BG_PANEL);

        pauseBtn.setVisible(false);
        resumeBtn.setVisible(false);

        p.add(runBtn);
        p.add(pauseBtn);
        p.add(resumeBtn);
        p.add(stepBtn);
        p.add(resetBtn);

        // Speed slider
        JLabel speedLbl = Theme.formLabel("Speed:");
        speedLbl.setForeground(Theme.TEXT_SECONDARY);
        p.add(Box.createHorizontalStrut(8));
        p.add(speedLbl);
        p.add(speedSlider);

        return p;
    }

    private JSlider buildSpeedSlider() {
        JSlider s = new JSlider(JSlider.HORIZONTAL, 1, 10, 5);
        s.setBackground(Theme.BG_PANEL);
        s.setForeground(Theme.TEXT_SECONDARY);
        s.setPreferredSize(new Dimension(120, 24));
        s.setMajorTickSpacing(3);
        s.setPaintTicks(false);
        s.setToolTipText("Simulation speed (1=slow, 10=fast)");
        s.addChangeListener(e -> {
            // Map 1–10 → 500ms–20ms (inverse: faster = smaller delay)
            int spd = s.getValue();
            int delay = (int)(500 - (spd - 1) * (480.0 / 9));
            simulator.setTimerDelayMs(delay);
        });
        return s;
    }

    // ─── Body ─────────────────────────────────────────────────────────────────

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBackground(Theme.BG_MAIN);
        body.setBorder(new EmptyBorder(8, 10, 8, 10));

        body.add(buildStatCards(), BorderLayout.NORTH);
        body.add(buildTabbedPane(), BorderLayout.CENTER);

        return body;
    }

    // ─── Stat cards strip ─────────────────────────────────────────────────────

    private JPanel buildStatCards() {
        JPanel p = new JPanel(new GridLayout(1, 8, 8, 0));
        p.setBackground(Theme.BG_MAIN);
        p.setPreferredSize(new Dimension(-1, 72));

        p.add(statCard("Total Tasks",      statTotal,     Theme.ACCENT_TEAL));
        p.add(statCard("Pending",          statPending,   Theme.TEXT_SECONDARY));
        p.add(statCard("Completed",        statCompleted, Theme.GREEN));
        p.add(statCard("Processing",       statCurrent,   Theme.ACCENT_TEAL));
        p.add(statCard("Avg Wait (units)", statAvgWt,     Theme.AMBER));
        p.add(statCard("Avg TAT (units)",  statAvgTat,    Theme.BLUE));
        p.add(statCard("Makespan",         statMakespan,  Theme.TEXT_PRIMARY));
        p.add(statCard("Utilization",      statUtil,      Theme.GREEN));

        return p;
    }

    private JPanel statCard(String title, JLabel valueLabel, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1, true),
            new EmptyBorder(6, 10, 6, 10)));

        JLabel lbl = new JLabel(title);
        lbl.setFont(Theme.FONT_CARD_LBL);
        lbl.setForeground(Theme.TEXT_SECONDARY);

        valueLabel.setFont(Theme.FONT_CARD_VAL);
        valueLabel.setForeground(accent);

        // Teal accent bar at top
        JPanel accentBar = new JPanel();
        accentBar.setBackground(accent);
        accentBar.setPreferredSize(new Dimension(-1, 3));

        p.add(accentBar,   BorderLayout.NORTH);
        p.add(lbl,         BorderLayout.CENTER);
        p.add(valueLabel,  BorderLayout.SOUTH);

        return p;
    }

    // ─── Tabbed pane ──────────────────────────────────────────────────────────

    private JTabbedPane buildTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_PANEL);
        tabs.setForeground(Theme.TEXT_PRIMARY);
        tabs.setFont(Theme.FONT_BTN);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // Tab 1: Task Management
        tabs.addTab("📋  Tasks", wrapTab(taskPanel));

        // Tab 2: Production Line
        JPanel prodTab = new JPanel(new BorderLayout(0, 4));
        prodTab.setBackground(Theme.BG_MAIN);
        prodTab.setBorder(new EmptyBorder(8, 8, 8, 8));
        prodTab.add(prodLine, BorderLayout.CENTER);
        prodTab.add(ProductionLinePanel.createLegend(), BorderLayout.SOUTH);
        tabs.addTab("🏭  Production Line", prodTab);

        // Tab 3: Charts
        JPanel chartsTab = new JPanel(new GridLayout(2, 1, 0, 8));
        chartsTab.setBackground(Theme.BG_MAIN);
        chartsTab.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Burst chart with sort toggle
        JPanel burstWrapper = new JPanel(new BorderLayout(0, 4));
        burstWrapper.setBackground(Theme.BG_PANEL);
        burstWrapper.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        JPanel burstHeaderRow = new JPanel(new BorderLayout());
        burstHeaderRow.setBackground(Theme.BG_PANEL);
        JToggleButton sortToggle = new JToggleButton("Sort by BT", true);
        sortToggle.setFont(Theme.FONT_SMALL);
        sortToggle.setBackground(Theme.BG_SECONDARY);
        sortToggle.setForeground(Theme.TEXT_PRIMARY);
        sortToggle.setBorderPainted(false);
        sortToggle.addActionListener(e -> burstChart.setSortByBurstTime(sortToggle.isSelected()));
        burstHeaderRow.add(sortToggle, BorderLayout.EAST);
        burstWrapper.add(burstHeaderRow, BorderLayout.NORTH);

        JScrollPane burstScroll = new JScrollPane(burstChart);
        burstScroll.setBackground(Theme.BG_PANEL);
        burstScroll.getViewport().setBackground(Theme.BG_PANEL);
        burstScroll.setBorder(null);
        burstWrapper.add(burstScroll, BorderLayout.CENTER);

        // Gantt chart
        JScrollPane ganttScroll = new JScrollPane(ganttChart);
        ganttScroll.setBackground(Theme.BG_PANEL);
        ganttScroll.getViewport().setBackground(Theme.BG_PANEL);
        ganttScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        chartsTab.add(burstWrapper);
        chartsTab.add(ganttScroll);
        tabs.addTab("📈  Charts", wrapTab(chartsTab));

        // Tab 4: Analytics
        tabs.addTab("📊  Analytics", wrapTab(analytics));

        // Style tab headers
        UIManager.put("TabbedPane.selected",     Theme.BG_SECONDARY);
        UIManager.put("TabbedPane.background",   Theme.BG_PANEL);
        UIManager.put("TabbedPane.foreground",   Theme.TEXT_PRIMARY);
        UIManager.put("TabbedPane.borderColor",  Theme.BORDER);

        return tabs;
    }

    private JPanel wrapTab(JComponent comp) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_MAIN);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    // ─── Wire: task panel → charts/analytics ─────────────────────────────────

    private void wireTaskPanel() {
        taskPanel.setOnTasksChanged(tasks -> {
            currentTasks = tasks;
            if (!tasks.isEmpty()) {
                // Re-compute schedule eagerly on each task change (for charts)
                try {
                    currentSchedule = scheduler.schedule(tasks);
                    burstChart.setScheduleData(currentSchedule);
                    ganttChart.setScheduleData(currentSchedule);
                    analytics.setScheduleData(currentSchedule, tasks);
                } catch (Exception ex) {
                    currentSchedule = null;
                    burstChart.setScheduleData(null);
                    ganttChart.setScheduleData(null);
                    analytics.setScheduleData(null, null);
                }
            } else {
                currentSchedule = null;
                burstChart.setScheduleData(null);
                ganttChart.setScheduleData(null);
                analytics.setScheduleData(null, null);
            }
            updateStatCards();
        });
    }

    // ─── Wire: simulation controls ────────────────────────────────────────────

    private void wireSimControls() {
        runBtn.addActionListener(e -> {
            List<ManufacturingTask> tasks = taskPanel.getTasks();
            if (tasks.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Add at least one task before running.", "No Tasks", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Recalculate schedule
            try {
                currentSchedule = scheduler.schedule(tasks);
                currentTasks    = tasks;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Scheduler error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            taskPanel.setScheduleResult(currentSchedule);
            taskPanel.setSimRunning(true);
            burstChart.setScheduleData(currentSchedule);
            ganttChart.setScheduleData(currentSchedule);
            analytics.setScheduleData(currentSchedule, currentTasks);

            simulator.init(tasks, currentSchedule);
            simulator.run();

            updateButtonStates();
        });

        pauseBtn.addActionListener(e -> {
            simulator.pause();
            updateButtonStates();
        });

        resumeBtn.addActionListener(e -> {
            simulator.resume();
            updateButtonStates();
        });

        stepBtn.addActionListener(e -> {
            if (!simulator.isInitialised()) {
                // Auto-init
                runBtn.doClick();
                simulator.pause();
                return;
            }
            simulator.stepForward();
            updateButtonStates();
        });

        resetBtn.addActionListener(e -> {
            simulator.reset();
            taskPanel.setSimRunning(false);
            taskPanel.resetStatuses();
            prodLine.clear();
            ganttChart.clear();
            clockLabel.setText("T = 0");
            setStatus("READY", Theme.ACCENT_TEAL);
            updateButtonStates();
            updateStatCards();
        });
    }

    // ─── Wire: simulator callbacks ────────────────────────────────────────────

    private void wireSimulator() {
        simulator.setOnTick(sim -> SwingUtilities.invokeLater(() -> {
            prodLine.update(sim);
            ganttChart.updateSimState(sim.getSimTime(),
                sim.getCurrentTask() != null ? sim.getCurrentTask().getTaskId() : null);
            clockLabel.setText("T = " + sim.getSimTime());
            setStatus("RUNNING", Theme.ACCENT_TEAL);
            updateStatCardsFromSim(sim);
            updateButtonStates();
        }));

        simulator.setOnComplete(sim -> SwingUtilities.invokeLater(() -> {
            prodLine.update(sim);
            taskPanel.setScheduleResult(currentSchedule);
            setStatus("COMPLETED", Theme.GREEN);
            updateButtonStates();
            updateStatCards();
        }));
    }

    // ─── Status bar helpers ───────────────────────────────────────────────────

    private void setStatus(String text, Color color) {
        statusLabel.setText("● " + text);
        statusLabel.setForeground(color);
    }

    private void updateButtonStates() {
        ProductionSimulator.State state = simulator.getState();
        boolean idle      = state == ProductionSimulator.State.IDLE;
        boolean running   = state == ProductionSimulator.State.RUNNING;
        boolean paused    = state == ProductionSimulator.State.PAUSED;
        boolean completed = state == ProductionSimulator.State.COMPLETED;

        runBtn.setVisible(idle || completed);
        runBtn.setEnabled(idle || completed);
        pauseBtn.setVisible(running);
        pauseBtn.setEnabled(running);
        resumeBtn.setVisible(paused);
        resumeBtn.setEnabled(paused);
        stepBtn.setEnabled(!running && !completed);
        resetBtn.setEnabled(!idle);

        if (paused) setStatus("PAUSED", Theme.AMBER);
        else if (running) setStatus("RUNNING", Theme.ACCENT_TEAL);
        else if (completed) setStatus("COMPLETED", Theme.GREEN);
        else setStatus("READY", Theme.TEXT_SECONDARY);
    }

    // ─── Stat card updates ────────────────────────────────────────────────────

    private void updateStatCards() {
        List<ManufacturingTask> tasks = (currentTasks != null) ? currentTasks : taskPanel.getTasks();
        statTotal.setText(String.valueOf(tasks.size()));
        long pending   = tasks.stream().filter(t -> t.getStatus().name().equals("PENDING")).count();
        long completed = tasks.stream().filter(t -> t.getStatus().name().equals("COMPLETED")).count();
        statPending.setText(String.valueOf(pending));
        statCompleted.setText(String.valueOf(completed));
        statCurrent.setText("—");

        if (currentSchedule != null) {
            statAvgWt.setText(String.format("%.2f", currentSchedule.getAverageWaitingTime()));
            statAvgTat.setText(String.format("%.2f", currentSchedule.getAverageTurnaroundTime()));
            statMakespan.setText(String.valueOf(currentSchedule.getMakespan()));
            statUtil.setText(String.format("%.1f%%", currentSchedule.getMachineUtilization()));
        }
    }

    private void updateStatCardsFromSim(ProductionSimulator sim) {
        List<ManufacturingTask> tasks = sim.getTasks();
        statTotal.setText(String.valueOf(tasks.size()));
        long pending   = tasks.stream().filter(t -> t.getStatus().name().equals("PENDING")
                                                   || t.getStatus().name().equals("READY")).count();
        long completed = sim.getCompletedTasks().size();
        statPending.setText(String.valueOf(pending));
        statCompleted.setText(String.valueOf(completed));
        statCurrent.setText(sim.getCurrentTask() != null ? sim.getCurrentTask().getTaskId() : "—");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static JLabel statValue(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_CARD_VAL);
        l.setForeground(Theme.TEXT_PRIMARY);
        return l;
    }
}
