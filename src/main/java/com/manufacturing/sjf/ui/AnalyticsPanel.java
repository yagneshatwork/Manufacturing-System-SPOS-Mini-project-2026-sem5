package com.manufacturing.sjf.ui;

import com.manufacturing.sjf.scheduler.ScheduleResult;
import com.manufacturing.sjf.scheduler.ScheduledTask;
import com.manufacturing.sjf.scheduler.SJFScheduler;
import com.manufacturing.sjf.model.ManufacturingTask;
import com.manufacturing.sjf.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Analytics panel showing:
 * <ul>
 *   <li>Individual task metrics table (WT, TAT)</li>
 *   <li>Aggregate metrics (avg WT, avg TAT, makespan, utilization)</li>
 *   <li>SJF scheduling order</li>
 *   <li>FCFS vs SJF comparison (optional)</li>
 * </ul>
 *
 * <p>All data is driven from a {@link ScheduleResult}; nothing is hardcoded.</p>
 */
public class AnalyticsPanel extends JPanel {

    // ─── Metric cards ────────────────────────────────────────────────────────

    private final JLabel avgWtLabel     = metricValue("—");
    private final JLabel avgTatLabel    = metricValue("—");
    private final JLabel makespanLabel  = metricValue("—");
    private final JLabel utilLabel      = metricValue("—");
    private final JLabel idleLabel      = metricValue("—");
    private final JLabel orderLabel     = metricValue("—");

    // ─── Tables ───────────────────────────────────────────────────────────────

    private final DefaultTableModel taskMetricsModel;
    private final DefaultTableModel comparisonModel;

    private final SJFScheduler scheduler = new SJFScheduler();

    public AnalyticsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBackground(Theme.BG_MAIN);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        // Top: metric cards
        add(buildMetricCards(), BorderLayout.NORTH);

        // Centre: split between task metrics and FCFS comparison
        taskMetricsModel = new DefaultTableModel(
            new String[]{"Task ID", "Task Name", "AT", "BT", "ST", "CT", "WT", "TAT", "Order"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable taskMetricsTable = buildStyledTable(taskMetricsModel);

        comparisonModel = new DefaultTableModel(
            new String[]{"Metric", "FCFS Value", "SJF Value", "Winner"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable compTable = buildStyledTable(comparisonModel);
        compTable.getColumnModel().getColumn(3).setCellRenderer(new WinnerRenderer());

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            wrapInScroll("📊 Task Metrics (SJF)", taskMetricsTable),
            wrapInScroll("⚖ FCFS vs SJF Comparison", compTable));
        split.setDividerLocation(200);
        split.setBackground(Theme.BG_MAIN);
        split.setBorder(null);
        split.setDividerSize(5);

        add(split, BorderLayout.CENTER);
    }

    // ─── Update from schedule result ─────────────────────────────────────────

    public void setScheduleData(ScheduleResult sjfResult, List<ManufacturingTask> tasks) {
        update(sjfResult, tasks);
    }

    public void update(ScheduleResult sjfResult, List<ManufacturingTask> tasks) {
        if (sjfResult == null || sjfResult.getScheduledTasks().isEmpty()) {
            clearAll();
            return;
        }

        // Metric cards
        avgWtLabel.setText(String.format("%.2f", sjfResult.getAverageWaitingTime()));
        avgTatLabel.setText(String.format("%.2f", sjfResult.getAverageTurnaroundTime()));
        makespanLabel.setText(String.valueOf(sjfResult.getMakespan()));
        utilLabel.setText(String.format("%.1f%%", sjfResult.getMachineUtilization()));
        idleLabel.setText(String.valueOf(sjfResult.getTotalIdleTime()));

        // Scheduling order
        StringBuilder order = new StringBuilder();
        List<ScheduledTask> ordered = sjfResult.getScheduledTasks();
        for (int i = 0; i < ordered.size(); i++) {
            order.append(ordered.get(i).getTaskId());
            if (i < ordered.size() - 1) order.append(" → ");
        }
        orderLabel.setText("<html><body>" + order + "</body></html>");

        // Task metrics table
        taskMetricsModel.setRowCount(0);
        for (ScheduledTask st : ordered) {
            taskMetricsModel.addRow(new Object[]{
                st.getTaskId(),
                st.getTaskName(),
                st.getArrivalTime(),
                st.getBurstTime(),
                st.getStartTime(),
                st.getCompletionTime(),
                st.getWaitingTime(),
                st.getTurnaroundTime(),
                st.getExecutionOrder()
            });
        }

        // FCFS comparison
        if (tasks != null && !tasks.isEmpty()) {
            ScheduleResult fcfsResult = scheduler.scheduleFCFS(tasks);
            buildComparison(fcfsResult, sjfResult);
        }
    }

    private void buildComparison(ScheduleResult fcfs, ScheduleResult sjf) {
        comparisonModel.setRowCount(0);
        addCompRow("Avg Waiting Time",
            fmt(fcfs.getAverageWaitingTime()), fmt(sjf.getAverageWaitingTime()),
            fcfs.getAverageWaitingTime() >= sjf.getAverageWaitingTime() ? "SJF ✓" : "FCFS ✓");
        addCompRow("Avg Turnaround Time",
            fmt(fcfs.getAverageTurnaroundTime()), fmt(sjf.getAverageTurnaroundTime()),
            fcfs.getAverageTurnaroundTime() >= sjf.getAverageTurnaroundTime() ? "SJF ✓" : "FCFS ✓");
        addCompRow("Makespan",
            String.valueOf(fcfs.getMakespan()), String.valueOf(sjf.getMakespan()),
            fcfs.getMakespan() > sjf.getMakespan() ? "SJF ✓" : (fcfs.getMakespan() == sjf.getMakespan() ? "Tie" : "FCFS ✓"));
        addCompRow("Total Idle Time",
            String.valueOf(fcfs.getTotalIdleTime()), String.valueOf(sjf.getTotalIdleTime()),
            fcfs.getTotalIdleTime() > sjf.getTotalIdleTime() ? "SJF ✓" :
            fcfs.getTotalIdleTime() == sjf.getTotalIdleTime() ? "Tie" : "FCFS ✓");
        addCompRow("Machine Utilization",
            fmt1(fcfs.getMachineUtilization()) + "%", fmt1(sjf.getMachineUtilization()) + "%",
            fcfs.getMachineUtilization() < sjf.getMachineUtilization() ? "SJF ✓" :
            fcfs.getMachineUtilization() == sjf.getMachineUtilization() ? "Tie" : "FCFS ✓");
    }

    private void addCompRow(String metric, String fcfs, String sjf, String winner) {
        comparisonModel.addRow(new Object[]{metric, fcfs, sjf, winner});
    }

    private void clearAll() {
        avgWtLabel.setText("—");
        avgTatLabel.setText("—");
        makespanLabel.setText("—");
        utilLabel.setText("—");
        idleLabel.setText("—");
        orderLabel.setText("—");
        taskMetricsModel.setRowCount(0);
        comparisonModel.setRowCount(0);
    }

    // ─── UI Builders ──────────────────────────────────────────────────────────

    private JPanel buildMetricCards() {
        JPanel p = new JPanel(new GridLayout(2, 3, 8, 8));
        p.setBackground(Theme.BG_MAIN);

        p.add(card("Avg Waiting Time",     avgWtLabel,    Theme.AMBER));
        p.add(card("Avg Turnaround Time",  avgTatLabel,   Theme.ACCENT_TEAL));
        p.add(card("Makespan",             makespanLabel, Theme.BLUE));
        p.add(card("Machine Utilization",  utilLabel,     Theme.GREEN));
        p.add(card("Total Idle Time",      idleLabel,     Theme.RED));
        p.add(card("Execution Order",      orderLabel,    Theme.TEXT_SECONDARY));

        return p;
    }

    private JPanel card(String title, JLabel valueLabel, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 1, true),
            new EmptyBorder(10, 12, 10, 12)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_CARD_LBL);
        titleLbl.setForeground(Theme.TEXT_SECONDARY);

        valueLabel.setFont(Theme.FONT_HEADING);
        valueLabel.setForeground(accent);

        p.add(titleLbl,   BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    private JScrollPane wrapInScroll(String title, JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1),
            title, 0, 0, Theme.FONT_HEADING, Theme.ACCENT_TEAL));
        sp.setBackground(Theme.BG_PANEL);
        sp.getViewport().setBackground(Theme.BG_PANEL);
        return sp;
    }

    private JTable buildStyledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(Theme.BG_PANEL);
        t.setForeground(Theme.TEXT_PRIMARY);
        t.setFont(Theme.FONT_SMALL);
        t.setRowHeight(26);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(Theme.ACCENT_TEAL);
        t.setSelectionForeground(Theme.BG_MAIN);
        t.getTableHeader().setBackground(Theme.BG_SECONDARY);
        t.getTableHeader().setForeground(Theme.TEXT_PRIMARY);
        t.getTableHeader().setFont(Theme.FONT_BTN);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < model.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(center);
        }
        return t;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static JLabel metricValue(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_CARD_VAL);
        l.setForeground(Theme.TEXT_PRIMARY);
        return l;
    }

    private static String fmt(double v)  { return String.format("%.2f", v); }
    private static String fmt1(double v) { return String.format("%.1f", v); }

    // ─── Winner column renderer ───────────────────────────────────────────────

    private static class WinnerRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean hasFocus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                table, value, selected, hasFocus, row, col);
            lbl.setHorizontalAlignment(CENTER);
            if (!selected && value instanceof String s) {
                lbl.setForeground(s.startsWith("SJF") ? Theme.ACCENT_TEAL
                    : s.startsWith("FCFS") ? Theme.AMBER : Theme.TEXT_SECONDARY);
            }
            return lbl;
        }
    }
}
