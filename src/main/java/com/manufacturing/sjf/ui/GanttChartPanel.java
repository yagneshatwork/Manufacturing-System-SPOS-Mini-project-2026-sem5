package com.manufacturing.sjf.ui;

import com.manufacturing.sjf.scheduler.ScheduleResult;
import com.manufacturing.sjf.scheduler.ScheduledTask;
import com.manufacturing.sjf.simulation.ProductionSimulator;
import com.manufacturing.sjf.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Proportional Gantt chart rendered with Graphics2D.
 *
 * <p>Each task block is sized proportionally to its burst time.
 * Idle gaps are shown as grey blocks. A time cursor marks the current
 * simulation time. The currently executing task is highlighted in teal.</p>
 */
public class GanttChartPanel extends JPanel {

    private ScheduleResult scheduleResult;
    private int            simTime    = 0;
    private String         currentId  = null;   // currently executing task ID

    private static final int MARGIN_LEFT   = 12;
    private static final int MARGIN_RIGHT  = 12;
    private static final int MARGIN_TOP    = 40;
    private static final int BAR_H         = 44;
    private static final int TICK_H        = 14;
    private static final int LABEL_H       = 16;

    public GanttChartPanel() {
        setBackground(Theme.BG_PANEL);
        setPreferredSize(new Dimension(800, MARGIN_TOP + BAR_H + TICK_H + LABEL_H + 20));
        setBorder(new EmptyBorder(4, 4, 4, 4));
    }

    /** Update from scheduler results (called after schedule is computed). */
    public void setScheduleData(ScheduleResult result) {
        this.scheduleResult = result;
        repaint();
    }

    /** Update simulation cursor and highlight from a running simulation. */
    public void updateSimState(int simTime, String currentTaskId) {
        this.simTime   = simTime;
        this.currentId = currentTaskId;
        repaint();
    }

    /** Clear to blank state. */
    public void clear() {
        this.scheduleResult = null;
        this.simTime   = 0;
        this.currentId = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int W = getWidth();
        int H = getHeight();

        g2.setColor(Theme.BG_PANEL);
        g2.fillRect(0, 0, W, H);

        if (scheduleResult == null || scheduleResult.getScheduledTasks().isEmpty()) {
            drawEmpty(g2, W, H);
            g2.dispose();
            return;
        }

        // Determine timeline extent
        List<ScheduledTask> tasks = scheduleResult.getScheduledTasks();
        List<ScheduleResult.IdleInterval> idles = scheduleResult.getIdleIntervals();

        int timeStart = tasks.get(0).getStartTime();
        int timeEnd   = tasks.stream().mapToInt(ScheduledTask::getCompletionTime).max().orElse(1);
        // also account for idle intervals that may start before timeStart
        for (ScheduleResult.IdleInterval idle : idles) {
            timeStart = Math.min(timeStart, idle.startTime());
        }
        int totalSpan = timeEnd - timeStart;
        if (totalSpan <= 0) totalSpan = 1;

        int chartW   = W - MARGIN_LEFT - MARGIN_RIGHT;
        int barY     = MARGIN_TOP;

        // Chart title
        g2.setFont(Theme.FONT_HEADING);
        g2.setColor(Theme.ACCENT_TEAL);
        g2.drawString("Gantt Chart  —  Execution Timeline", MARGIN_LEFT + 4, 22);

        // Draw idle blocks first (behind task blocks)
        for (ScheduleResult.IdleInterval idle : idles) {
            int x  = MARGIN_LEFT + (int)((double)(idle.startTime() - timeStart) / totalSpan * chartW);
            int w  = (int)((double) idle.duration() / totalSpan * chartW);
            w = Math.max(w, 3);

            g2.setColor(Theme.BG_SECONDARY);
            g2.fill(new RoundRectangle2D.Float(x, barY, w, BAR_H, 4, 4));
            g2.setColor(Theme.BORDER);
            g2.setStroke(new BasicStroke(1));
            g2.draw(new RoundRectangle2D.Float(x, barY, w, BAR_H, 4, 4));

            // "IDLE" label
            if (w > 30) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 10));
                g2.setColor(Theme.TEXT_DISABLED);
                FontMetrics fm = g2.getFontMetrics();
                String lbl = "IDLE";
                g2.drawString(lbl, x + (w - fm.stringWidth(lbl)) / 2, barY + BAR_H / 2 + 4);
            }
        }

        // Draw task blocks
        for (ScheduledTask st : tasks) {
            boolean isCurrent = st.getTaskId().equals(currentId);

            int x = MARGIN_LEFT + (int)((double)(st.getStartTime()  - timeStart) / totalSpan * chartW);
            int w = (int)((double)(st.getCompletionTime() - st.getStartTime()) / totalSpan * chartW);
            w = Math.max(w, 8);

            Color blockColor = isCurrent ? Theme.ACCENT_TEAL
                             : (st.getCompletionTime() <= simTime ? Theme.GREEN
                             : Theme.BG_SECONDARY);

            // Shadow
            g2.setColor(Theme.withAlpha(Color.BLACK, 60));
            g2.fill(new RoundRectangle2D.Float(x + 2, barY + 2, w, BAR_H, 6, 6));

            // Fill
            GradientPaint gp = new GradientPaint(
                x, barY, blockColor,
                x, barY + BAR_H, Theme.withAlpha(blockColor, 160));
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(x, barY, w, BAR_H, 6, 6));

            // Border
            g2.setColor(isCurrent ? Theme.ACCENT_TEAL_L : Theme.BORDER);
            g2.setStroke(new BasicStroke(isCurrent ? 2 : 1));
            g2.draw(new RoundRectangle2D.Float(x, barY, w, BAR_H, 6, 6));

            // Task ID inside block
            if (w > 24) {
                g2.setFont(Theme.FONT_BTN);
                g2.setColor(Theme.BG_MAIN);
                FontMetrics fm = g2.getFontMetrics();
                String lbl = st.getTaskId();
                if (fm.stringWidth(lbl) < w - 4) {
                    g2.drawString(lbl, x + (w - fm.stringWidth(lbl)) / 2, barY + BAR_H / 2 + 5);
                } else {
                    // Clip label
                    lbl = lbl.length() > 3 ? lbl.substring(0, 3) : lbl;
                    g2.drawString(lbl, x + 3, barY + BAR_H / 2 + 5);
                }
            }
        }

        // Draw time axis
        drawTimeAxis(g2, timeStart, timeEnd, totalSpan, chartW, barY);

        // Draw simulation time cursor
        if (simTime >= timeStart && simTime <= timeEnd) {
            int cx = MARGIN_LEFT + (int)((double)(simTime - timeStart) / totalSpan * chartW);
            g2.setColor(Theme.AMBER);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{4, 3}, 0));
            g2.drawLine(cx, barY - 6, cx, barY + BAR_H + TICK_H);
            g2.setStroke(new BasicStroke(1));

            // Cursor label
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.AMBER);
            String cursorLbl = "T=" + simTime;
            FontMetrics fm = g2.getFontMetrics();
            int lblX = Math.min(cx - fm.stringWidth(cursorLbl) / 2,
                W - MARGIN_RIGHT - fm.stringWidth(cursorLbl));
            lblX = Math.max(lblX, MARGIN_LEFT);
            g2.drawString(cursorLbl, lblX, barY - 8);
        }

        g2.dispose();
    }

    private void drawTimeAxis(Graphics2D g2, int timeStart, int timeEnd,
                              int totalSpan, int chartW, int barY) {
        int axisY = barY + BAR_H;
        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(1));
        g2.drawLine(MARGIN_LEFT, axisY, MARGIN_LEFT + chartW, axisY);

        // Draw start/end time markers and intermediate ticks
        int maxTicks = Math.min(totalSpan, 20);
        g2.setFont(Theme.FONT_SMALL);

        for (int i = 0; i <= maxTicks; i++) {
            int t    = timeStart + (int) Math.round((double) i / maxTicks * totalSpan);
            int tickX = MARGIN_LEFT + (int)((double)(t - timeStart) / totalSpan * chartW);

            g2.setColor(Theme.BORDER);
            g2.drawLine(tickX, axisY, tickX, axisY + 5);

            g2.setColor(Theme.TEXT_SECONDARY);
            FontMetrics fm = g2.getFontMetrics();
            String lbl = String.valueOf(t);
            g2.drawString(lbl, tickX - fm.stringWidth(lbl) / 2, axisY + TICK_H);
        }
    }

    private void drawEmpty(Graphics2D g2, int W, int H) {
        g2.setFont(Theme.FONT_BODY);
        g2.setColor(Theme.TEXT_DISABLED);
        String msg = "Run the scheduler to see the Gantt chart";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2);
    }
}
