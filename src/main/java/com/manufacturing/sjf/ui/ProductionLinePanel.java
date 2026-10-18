package com.manufacturing.sjf.ui;

import com.manufacturing.sjf.model.ManufacturingTask;
import com.manufacturing.sjf.model.TaskStatus;
import com.manufacturing.sjf.scheduler.ScheduleResult;
import com.manufacturing.sjf.scheduler.ScheduledTask;
import com.manufacturing.sjf.simulation.ProductionSimulator;
import com.manufacturing.sjf.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom-painted 4-zone manufacturing production line.
 *
 * <p>Zones (left → right):</p>
 * <ol>
 *   <li>Input Queue — tasks that have arrived but not yet processed</li>
 *   <li>SJF Ready Queue — tasks sorted for SJF selection</li>
 *   <li>Processing Machine — the currently executing task with a progress bar</li>
 *   <li>Completed Goods — finished tasks</li>
 * </ol>
 *
 * <p>Call {@link #update(ProductionSimulator)} after each simulation tick to
 * refresh the display.</p>
 */
public class ProductionLinePanel extends JPanel {

    // Zone proportions (fraction of total width)
    private static final double ZONE_INPUT  = 0.22;
    private static final double ZONE_QUEUE  = 0.25;
    private static final double ZONE_PROC   = 0.28;
    private static final double ZONE_DONE   = 0.25;

    // Task box dimensions
    private static final int BOX_W  = 80;
    private static final int BOX_H  = 36;
    private static final int BOX_GAP= 8;
    private static final int ARC    = 8;

    // Animation state
    private List<ManufacturingTask> inputTasks    = new ArrayList<>();
    private List<ManufacturingTask> readyQueue    = new ArrayList<>();
    private ScheduledTask            currentTask   = null;
    private double                   progress      = 0.0;
    private List<ScheduledTask>      completedList = new ArrayList<>();
    private int                      simTime       = 0;
    private ProductionSimulator.State simState     = ProductionSimulator.State.IDLE;
    private ScheduledTask            nextTask      = null; // highlighted "next"
    private ScheduleResult           scheduleResult;

    public ProductionLinePanel() {
        setBackground(Theme.BG_MAIN);
        setPreferredSize(new Dimension(900, 220));
        setBorder(new EmptyBorder(4, 4, 4, 4));
    }

    /** Refreshes all visual state from the current simulator snapshot. */
    public void update(ProductionSimulator sim) {
        this.simTime       = sim.getSimTime();
        this.simState      = sim.getState();
        this.currentTask   = sim.getCurrentTask();
        this.progress      = sim.getCurrentTaskProgress();
        this.completedList = new ArrayList<>(sim.getCompletedTasks());
        this.nextTask      = sim.getNextTask();
        this.scheduleResult= sim.getScheduleResult();

        // Classify tasks
        inputTasks.clear();
        readyQueue.clear();
        if (scheduleResult != null) {
            List<ScheduledTask> ordered = scheduleResult.getScheduledTasks();
            for (ManufacturingTask t : sim.getTasks()) {
                TaskStatus s = t.getStatus();
                if (s == TaskStatus.PENDING) {
                    inputTasks.add(t);
                } else if (s == TaskStatus.READY) {
                    readyQueue.add(t);
                }
                // PROCESSING and COMPLETED handled separately
            }
        }
        repaint();
    }

    /** Clears all displayed state (on reset). */
    public void clear() {
        inputTasks.clear();
        readyQueue.clear();
        currentTask    = null;
        progress       = 0;
        completedList.clear();
        simTime        = 0;
        simState       = ProductionSimulator.State.IDLE;
        nextTask       = null;
        scheduleResult = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int W = getWidth();
        int H = getHeight();

        drawBackground(g2, W, H);

        int x0 = 8;
        int zoneW_input = (int)(W * ZONE_INPUT);
        int zoneW_queue = (int)(W * ZONE_QUEUE);
        int zoneW_proc  = (int)(W * ZONE_PROC);
        int zoneW_done  = W - x0 - zoneW_input - zoneW_queue - zoneW_proc - 8;

        int x1 = x0 + zoneW_input;
        int x2 = x1 + zoneW_queue;
        int x3 = x2 + zoneW_proc;

        // Draw zone panels
        drawZone(g2, x0, 4, zoneW_input - 4, H - 8, "📥 INPUT", Theme.BG_PANEL);
        drawZone(g2, x1, 4, zoneW_queue - 4, H - 8, "⏳ SJF QUEUE", Theme.BG_PANEL);
        drawZone(g2, x2, 4, zoneW_proc  - 4, H - 8, "⚙  MACHINE", Theme.BG_SECONDARY);
        drawZone(g2, x3, 4, zoneW_done  - 4, H - 8, "✅ COMPLETED", Theme.BG_PANEL);

        // Draw arrows between zones
        drawArrow(g2, x1 - 4, H / 2);
        drawArrow(g2, x2 - 4, H / 2);
        drawArrow(g2, x3 - 4, H / 2);

        // Draw tasks in each zone
        int contentY  = 44;   // below zone title
        int centerY   = (H - contentY) / 2 + contentY;

        drawInputZone(g2,  x0 + 6,     contentY, zoneW_input  - 16, H - contentY - 4);
        drawQueueZone(g2,  x1 + 6,     contentY, zoneW_queue  - 16, H - contentY - 4);
        drawMachineZone(g2, x2 + 6,    contentY, zoneW_proc   - 16, H - contentY - 4);
        drawDoneZone(g2,   x3 + 6,     contentY, zoneW_done   - 16, H - contentY - 4);

        // Draw sim time and state overlay
        drawStatusBar(g2, W, H);

        g2.dispose();
    }

    // ─── Zone drawing ─────────────────────────────────────────────────────────

    private void drawBackground(Graphics2D g2, int W, int H) {
        g2.setColor(Theme.BG_MAIN);
        g2.fillRect(0, 0, W, H);
    }

    private void drawZone(Graphics2D g2, int x, int y, int w, int h, String title, Color bg) {
        // Zone background
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 12, 12));
        // Border
        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(1));
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, 12, 12));
        // Title
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, x + (w - fm.stringWidth(title)) / 2, y + 20);
        // Title underline
        g2.setColor(Theme.ACCENT_TEAL);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(x + 8, y + 24, x + w - 8, y + 24);
    }

    private void drawArrow(Graphics2D g2, int midX, int midY) {
        int ax = midX;
        g2.setColor(Theme.TEXT_DISABLED);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(ax, midY, ax + 8, midY);
        int[] xPts = {ax + 8, ax + 14, ax + 8};
        int[] yPts = {midY - 5, midY, midY + 5};
        g2.fillPolygon(xPts, yPts, 3);
    }

    private void drawInputZone(Graphics2D g2, int x, int y, int w, int h) {
        if (inputTasks.isEmpty()) {
            drawEmptyMsg(g2, x, y, w, h, "No pending tasks");
            return;
        }
        int col = Math.max(1, w / (BOX_W + BOX_GAP));
        drawTaskBoxes(g2, inputTasks.stream().map(t -> {
            String label = t.getTaskId();
            String sub   = "BT:" + t.getBurstTime();
            Color  bg    = Theme.BG_SECONDARY;
            Color  fg    = Theme.TEXT_SECONDARY;
            return new BoxInfo(label, sub, bg, fg, false);
        }).toList(), x, y, w, h, col);
    }

    private void drawQueueZone(Graphics2D g2, int x, int y, int w, int h) {
        if (readyQueue.isEmpty()) {
            drawEmptyMsg(g2, x, y, w, h, "Queue empty");
            return;
        }
        // Sort by burst time for SJF visual ordering
        List<ManufacturingTask> sorted = new ArrayList<>(readyQueue);
        sorted.sort(java.util.Comparator
            .comparingInt(ManufacturingTask::getBurstTime)
            .thenComparing(ManufacturingTask::getTaskId));

        List<BoxInfo> boxes = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            ManufacturingTask t = sorted.get(i);
            boolean isNext = nextTask != null && t.getTaskId().equals(nextTask.getTaskId());
            Color bg = isNext ? Theme.AMBER : Theme.BG_SECONDARY;
            Color fg = isNext ? Theme.BG_MAIN : Theme.TEXT_PRIMARY;
            boxes.add(new BoxInfo(t.getTaskId(), "BT:" + t.getBurstTime(), bg, fg, isNext));
        }
        int col = Math.max(1, w / (BOX_W + BOX_GAP));
        drawTaskBoxes(g2, boxes, x, y, w, h, col);
    }

    private void drawMachineZone(Graphics2D g2, int x, int y, int w, int h) {
        if (currentTask == null) {
            // Check if idle or truly empty
            String msg = (simState == ProductionSimulator.State.IDLE) ? "Idle" : "⏸ Idle";
            drawEmptyMsg(g2, x, y, w, h, msg);
            return;
        }
        // Machine gears decoration
        int cx = x + w / 2;
        int cy = y + 18;

        // Task block
        int bx = cx - BOX_W / 2 - 10;
        int by = y + 8;
        int bw = BOX_W + 20;
        int bh = BOX_H + 4;

        g2.setColor(Theme.ACCENT_TEAL);
        g2.fill(new RoundRectangle2D.Float(bx, by, bw, bh, ARC, ARC));

        g2.setFont(Theme.FONT_BTN);
        g2.setColor(Theme.BG_MAIN);
        FontMetrics fm = g2.getFontMetrics();
        String label = currentTask.getTaskId();
        g2.drawString(label, bx + (bw - fm.stringWidth(label)) / 2, by + 16);

        g2.setFont(Theme.FONT_SMALL);
        fm = g2.getFontMetrics();
        String sub = "BT:" + currentTask.getBurstTime();
        g2.drawString(sub, bx + (bw - fm.stringWidth(sub)) / 2, by + 30);

        // Progress bar background
        int pbY  = by + bh + 8;
        int pbW  = w - 4;
        int pbH  = 14;
        g2.setColor(Theme.BG_MAIN);
        g2.fill(new RoundRectangle2D.Float(x, pbY, pbW, pbH, 6, 6));

        // Progress fill
        int fillW = (int)(pbW * progress);
        if (fillW > 0) {
            g2.setColor(Theme.ACCENT_TEAL);
            g2.fill(new RoundRectangle2D.Float(x, pbY, fillW, pbH, 6, 6));
        }

        // Progress text
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_PRIMARY);
        String pct = String.format("%.0f%%", progress * 100);
        fm = g2.getFontMetrics();
        g2.drawString(pct, x + (pbW - fm.stringWidth(pct)) / 2, pbY + 10);

        // Time info
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        String timeInfo = String.format("ST:%d  CT:%d",
            currentTask.getStartTime(), currentTask.getCompletionTime());
        fm = g2.getFontMetrics();
        g2.drawString(timeInfo, x + (w - fm.stringWidth(timeInfo)) / 2, pbY + pbH + 14);
    }

    private void drawDoneZone(Graphics2D g2, int x, int y, int w, int h) {
        if (completedList.isEmpty()) {
            drawEmptyMsg(g2, x, y, w, h, "None yet");
            return;
        }
        List<BoxInfo> boxes = completedList.stream().map(st ->
            new BoxInfo(st.getTaskId(), "CT:" + st.getCompletionTime(),
                Theme.GREEN, Theme.BG_MAIN, false)
        ).toList();
        int col = Math.max(1, w / (BOX_W + BOX_GAP));
        drawTaskBoxes(g2, boxes, x, y, w, h, col);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private record BoxInfo(String label, String sub, Color bg, Color fg, boolean highlight) {}

    private void drawTaskBoxes(Graphics2D g2, List<BoxInfo> boxes,
                               int x, int y, int w, int h, int cols) {
        int row = 0, col = 0;
        for (BoxInfo b : boxes) {
            int bx = x + col * (BOX_W + BOX_GAP);
            int by = y + row * (BOX_H + BOX_GAP) + 4;

            if (bx + BOX_W > x + w) { // wrap
                col = 0; row++;
                bx = x;
                by = y + row * (BOX_H + BOX_GAP) + 4;
            }
            if (by + BOX_H > y + h) break; // out of space

            // Shadow
            g2.setColor(Theme.withAlpha(Color.BLACK, 60));
            g2.fill(new RoundRectangle2D.Float(bx + 2, by + 2, BOX_W, BOX_H, ARC, ARC));

            // Box
            g2.setColor(b.bg());
            g2.fill(new RoundRectangle2D.Float(bx, by, BOX_W, BOX_H, ARC, ARC));

            if (b.highlight()) {
                g2.setColor(Theme.BG_MAIN);
                g2.setStroke(new BasicStroke(2));
                g2.draw(new RoundRectangle2D.Float(bx, by, BOX_W, BOX_H, ARC, ARC));
            }

            // Label
            g2.setFont(Theme.FONT_BTN);
            g2.setColor(b.fg());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(b.label(), bx + (BOX_W - fm.stringWidth(b.label())) / 2, by + 15);

            // Sub-label
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(b.highlight() ? Theme.BG_SECONDARY : Theme.TEXT_SECONDARY);
            fm = g2.getFontMetrics();
            g2.drawString(b.sub(), bx + (BOX_W - fm.stringWidth(b.sub())) / 2, by + 27);

            col++;
            if (col >= cols) { col = 0; row++; }
        }
    }

    private void drawEmptyMsg(Graphics2D g2, int x, int y, int w, int h, String msg) {
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_DISABLED);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, x + (w - fm.stringWidth(msg)) / 2,
            y + (h - fm.getHeight()) / 2 + fm.getAscent());
    }

    private void drawStatusBar(Graphics2D g2, int W, int H) {
        // Bottom-right sim clock
        String stateStr = simState.name();
        String clockStr = "T = " + simTime + "  |  " + stateStr;
        g2.setFont(Theme.FONT_SMALL);
        Color stateColor = switch (simState) {
            case RUNNING   -> Theme.ACCENT_TEAL;
            case PAUSED    -> Theme.AMBER;
            case COMPLETED -> Theme.GREEN;
            default        -> Theme.TEXT_SECONDARY;
        };
        g2.setColor(stateColor);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(clockStr, W - fm.stringWidth(clockStr) - 12, H - 6);
    }

    // ─── Legend panel (returned for embedding) ────────────────────────────────

    public static JPanel createLegend() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        p.setBackground(Theme.BG_MAIN);
        addLegendItem(p, Theme.BG_SECONDARY, "Pending");
        addLegendItem(p, Theme.AMBER,        "Next/Selected");
        addLegendItem(p, Theme.ACCENT_TEAL,  "Processing");
        addLegendItem(p, Theme.GREEN,        "Completed");
        addLegendItem(p, Theme.RED,          "Error");
        return p;
    }

    private static void addLegendItem(JPanel p, Color color, String text) {
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 2, 12, 12);
            }
        };
        dot.setPreferredSize(new Dimension(14, 16));
        dot.setOpaque(false);
        p.add(dot);
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_SECONDARY);
        p.add(lbl);
    }
}
