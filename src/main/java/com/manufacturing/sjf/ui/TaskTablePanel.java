package com.manufacturing.sjf.ui;

import com.manufacturing.sjf.model.ManufacturingTask;
import com.manufacturing.sjf.model.TaskStatus;
import com.manufacturing.sjf.scheduler.ScheduleResult;
import com.manufacturing.sjf.scheduler.ScheduledTask;
import com.manufacturing.sjf.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Task management panel containing:
 * <ul>
 *   <li>Form for adding/updating tasks</li>
 *   <li>JTable showing all tasks with computed scheduling metrics</li>
 *   <li>Action buttons: Add, Update, Delete, Clear, Load Sample</li>
 * </ul>
 *
 * <p>Call {@link #setScheduleResult(ScheduleResult)} after the scheduler runs
 * to populate the Start/Completion/WT/TAT/Status columns from real results.</p>
 */
public class TaskTablePanel extends JPanel {

    // ─── Table model columns ──────────────────────────────────────────────────

    private static final String[] COLUMNS = {
        "Task ID", "Task Name", "Arrival", "Burst",
        "Start", "Completion", "Waiting", "Turnaround", "Status"
    };

    private static final int COL_ID     = 0;
    private static final int COL_NAME   = 1;
    private static final int COL_AT     = 2;
    private static final int COL_BT     = 3;
    private static final int COL_ST     = 4;
    private static final int COL_CT     = 5;
    private static final int COL_WT     = 6;
    private static final int COL_TAT    = 7;
    private static final int COL_STATUS = 8;

    // ─── Data ─────────────────────────────────────────────────────────────────

    private final List<ManufacturingTask> tasks        = new ArrayList<>();
    private       ScheduleResult          scheduleResult;
    private boolean                       simRunning   = false;

    // ─── Callbacks ────────────────────────────────────────────────────────────

    /** Invoked whenever the task list changes (add/delete/update). */
    private Consumer<List<ManufacturingTask>> onTasksChanged;

    // ─── UI Components ────────────────────────────────────────────────────────

    private final DefaultTableModel tableModel;
    private final JTable            table;
    private JTextField              idField;
    private JTextField              nameField;
    private JSpinner                arrivalSpinner;
    private JSpinner                burstSpinner;
    private JButton                 addBtn;
    private JButton                 updateBtn;
    private JButton                 deleteBtn;
    private JButton                 clearBtn;
    private JButton                 sampleBtn;

    public TaskTablePanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(Theme.BG_PANEL);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        // ── Form ──────────────────────────────────────────────────────────────
        JPanel form = buildForm();

        // ── Table ─────────────────────────────────────────────────────────────
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        styleTable();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBackground(Theme.BG_PANEL);
        scrollPane.getViewport().setBackground(Theme.BG_PANEL);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));

        // Selection listener → populate form for editing
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateFormFromSelection();
        });

        add(form,       BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    // ─── Build form ───────────────────────────────────────────────────────────

    private JPanel buildForm() {
        idField        = Theme.textField();
        nameField      = Theme.textField();
        arrivalSpinner = Theme.intSpinner(0, 0, 9999);
        burstSpinner   = Theme.intSpinner(1, 1, 9999);

        JPanel row = new JPanel(new GridLayout(1, 4, 6, 0));
        row.setBackground(Theme.BG_PANEL);

        row.add(fieldGroup("Task ID *", idField));
        row.add(fieldGroup("Task Name", nameField));
        row.add(fieldGroup("Arrival Time", wrapSpinner(arrivalSpinner)));
        row.add(fieldGroup("Burst Time *", wrapSpinner(burstSpinner)));

        // Buttons inline with form
        addBtn    = Theme.primaryButton("Add Task");
        updateBtn = Theme.secondaryButton("Update");
        deleteBtn = Theme.dangerButton("Delete");
        clearBtn  = Theme.secondaryButton("Clear");
        sampleBtn = Theme.warningButton("Load Sample");

        addBtn.addActionListener(e -> handleAdd());
        updateBtn.addActionListener(e -> handleUpdate());
        deleteBtn.addActionListener(e -> handleDelete());
        clearBtn.addActionListener(e -> clearForm());
        sampleBtn.addActionListener(e -> loadSampleData());

        JPanel btnPanel = new JPanel(new GridLayout(2, 3, 4, 4));
        btnPanel.setBackground(Theme.BG_PANEL);
        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(clearBtn);
        btnPanel.add(sampleBtn);

        JPanel outer = new JPanel(new BorderLayout(8, 0));
        outer.setBackground(Theme.BG_PANEL);
        outer.add(row, BorderLayout.CENTER);
        outer.add(btnPanel, BorderLayout.EAST);
        outer.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BG_PANEL);
        wrapper.add(outer, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel fieldGroup(String label, Component comp) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setBackground(Theme.BG_PANEL);
        p.add(Theme.formLabel(label), BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JPanel wrapSpinner(JSpinner s) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_PANEL);
        p.add(s, BorderLayout.CENTER);
        return p;
    }

    // ─── Table styling ────────────────────────────────────────────────────────

    private void styleTable() {
        table.setBackground(Theme.BG_PANEL);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setFont(Theme.FONT_BODY);
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(Theme.ACCENT_TEAL);
        table.setSelectionForeground(Theme.BG_MAIN);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(Theme.BG_SECONDARY);
        table.getTableHeader().setForeground(Theme.TEXT_PRIMARY);
        table.getTableHeader().setFont(Theme.FONT_BTN);
        table.getTableHeader().setPreferredSize(new Dimension(-1, 32));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        // Status column renderer
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusRenderer());

        // Alternating row renderer
        table.setDefaultRenderer(Object.class, new AlternatingRowRenderer());

        // Column widths
        int[] widths = {70, 150, 60, 60, 60, 80, 60, 80, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // ─── CRUD Handlers ────────────────────────────────────────────────────────

    private void handleAdd() {
        if (simRunning) { warnSimActive(); return; }
        String id   = idField.getText().trim();
        String name = nameField.getText().trim();
        int    at   = (Integer) arrivalSpinner.getValue();
        int    bt   = (Integer) burstSpinner.getValue();

        if (id.isEmpty()) { error("Task ID is required."); return; }
        if (bt <= 0)      { error("Burst time must be > 0."); return; }
        if (at < 0)       { error("Arrival time must be ≥ 0."); return; }
        if (tasks.stream().anyMatch(t -> t.getTaskId().equalsIgnoreCase(id))) {
            error("Task ID '" + id + "' already exists."); return;
        }

        try {
            ManufacturingTask task = new ManufacturingTask(id, name.isEmpty() ? id : name, at, bt);
            tasks.add(task);
            refreshTable();
            clearForm();
            fireTasksChanged();
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        }
    }

    private void handleUpdate() {
        if (simRunning) { warnSimActive(); return; }
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) { error("Select a row to update."); return; }

        String id   = idField.getText().trim();
        String name = nameField.getText().trim();
        int    at   = (Integer) arrivalSpinner.getValue();
        int    bt   = (Integer) burstSpinner.getValue();

        if (id.isEmpty()) { error("Task ID is required."); return; }

        ManufacturingTask existing = tasks.get(selectedRow);
        // Allow same ID (self-update), reject duplicate with different task
        if (!existing.getTaskId().equalsIgnoreCase(id) &&
            tasks.stream().anyMatch(t -> t.getTaskId().equalsIgnoreCase(id))) {
            error("Task ID '" + id + "' is already used by another task.");
            return;
        }

        try {
            existing.setTaskId(id);
            existing.setTaskName(name.isEmpty() ? id : name);
            existing.setArrivalTime(at);
            existing.setBurstTime(bt);
            refreshTable();
            clearForm();
            fireTasksChanged();
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        }
    }

    private void handleDelete() {
        if (simRunning) { warnSimActive(); return; }
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) { error("Select a row to delete."); return; }
        tasks.remove(selectedRow);
        refreshTable();
        clearForm();
        fireTasksChanged();
    }

    // ─── Sample Data ──────────────────────────────────────────────────────────

    public void loadSampleData() {
        if (simRunning) { warnSimActive(); return; }
        tasks.clear();
        tasks.add(new ManufacturingTask("J1", "Component Assembly", 0, 6));
        tasks.add(new ManufacturingTask("J2", "Quality Inspection",  0, 2));
        tasks.add(new ManufacturingTask("J3", "Packaging",           0, 8));
        tasks.add(new ManufacturingTask("J4", "Label Printing",      0, 3));
        scheduleResult = null;
        refreshTable();
        fireTasksChanged();
    }

    // ─── Schedule Results ─────────────────────────────────────────────────────

    public void setScheduleResult(ScheduleResult result) {
        this.scheduleResult = result;
        refreshTable();
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (ManufacturingTask t : tasks) {
            ScheduledTask st = findScheduledTask(t.getTaskId());
            tableModel.addRow(new Object[]{
                t.getTaskId(),
                t.getTaskName(),
                t.getArrivalTime(),
                t.getBurstTime(),
                st != null ? st.getStartTime()      : "—",
                st != null ? st.getCompletionTime() : "—",
                st != null ? st.getWaitingTime()    : "—",
                st != null ? st.getTurnaroundTime() : "—",
                st != null ? TaskStatus.COMPLETED.getDisplayName() : t.getStatus().getDisplayName()
            });
        }
    }

    private ScheduledTask findScheduledTask(String taskId) {
        if (scheduleResult == null) return null;
        return scheduleResult.getScheduledTasks().stream()
            .filter(st -> st.getTaskId().equals(taskId))
            .findFirst().orElse(null);
    }

    // ─── Form Utilities ───────────────────────────────────────────────────────

    private void populateFormFromSelection() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= tasks.size()) return;
        ManufacturingTask t = tasks.get(row);
        idField.setText(t.getTaskId());
        nameField.setText(t.getTaskName());
        arrivalSpinner.setValue(t.getArrivalTime());
        burstSpinner.setValue(t.getBurstTime());
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        arrivalSpinner.setValue(0);
        burstSpinner.setValue(1);
        table.clearSelection();
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public List<ManufacturingTask> getTasks() { return new ArrayList<>(tasks); }

    public void setSimRunning(boolean running) {
        this.simRunning = running;
        if (addBtn != null) addBtn.setEnabled(!running);
        if (updateBtn != null) updateBtn.setEnabled(!running);
        if (deleteBtn != null) deleteBtn.setEnabled(!running);
        if (sampleBtn != null) sampleBtn.setEnabled(!running);
        if (clearBtn != null) clearBtn.setEnabled(!running);
    }

    public void setOnTasksChanged(Consumer<List<ManufacturingTask>> callback) {
        this.onTasksChanged = callback;
    }

    /** Resets all task statuses back to PENDING and clears schedule columns. */
    public void resetStatuses() {
        tasks.forEach(t -> t.setStatus(TaskStatus.PENDING));
        scheduleResult = null;
        refreshTable();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void fireTasksChanged() {
        if (onTasksChanged != null) onTasksChanged.accept(getTasks());
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    private void warnSimActive() {
        JOptionPane.showMessageDialog(this,
            "Please reset the simulation before modifying tasks.",
            "Simulation Active", JOptionPane.WARNING_MESSAGE);
    }

    // ─── Custom Renderers ─────────────────────────────────────────────────────

    private static class AlternatingRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Theme.BG_PANEL : Theme.BG_MAIN);
            }
            ((JLabel) c).setBorder(new EmptyBorder(0, 8, 0, 8));
            return c;
        }
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            lbl.setBorder(new EmptyBorder(0, 8, 0, 8));
            if (!isSelected && value instanceof String statusStr) {
                lbl.setForeground(statusColour(statusStr));
            }
            return lbl;
        }

        private Color statusColour(String status) {
            return switch (status) {
                case "Processing" -> Theme.ACCENT_TEAL;
                case "Completed"  -> Theme.GREEN;
                case "Ready"      -> Theme.AMBER;
                case "Failed"     -> Theme.RED;
                default           -> Theme.TEXT_SECONDARY;
            };
        }
    }
}
