package com.manufacturing.sjf.ui;

import com.manufacturing.sjf.scheduler.ScheduleResult;
import com.manufacturing.sjf.scheduler.ScheduledTask;
import com.manufacturing.sjf.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Custom horizontal bar chart rendered with Graphics2D showing each task's
 * burst time. The shortest eligible task is highlighted in amber.
 *
 * <p>Data is driven entirely from a {@link ScheduleResult}; no hardcoded values.</p>
 */
public class BurstTimeChart extends JPanel {

    private ScheduleResult scheduleResult;
    private boolean sortByBurstTime = true;

    // Chart margins / sizing
    private static final int MARGIN_LEFT  = 100;
    private static final int MARGIN_RIGHT  = 60;
    private static final int MARGIN_TOP    = 40;
    private static final int MARGIN_BOTTOM = 30;
    private static final int BAR_HEIGHT    = 28;
    private static final int BAR_GAP       = 10;

    public BurstTimeChart() {
        setBackground(Theme.BG_PANEL);
        setBorder(new EmptyBorder(4, 4, 4, 4));
    }

    public void setScheduleData(ScheduleResult result) {
        this.scheduleResult = result;
        // Recalculate preferred height
        if (result != null) {
            int n = result.getScheduledTasks().size();
            int prefH = MARGIN_TOP + n * (BAR_HEIGHT + BAR_GAP) + MARGIN_BOTTOM + 20;
            setPreferredSize(new Dimension(getWidth(), Math.max(200, prefH)));
        }
        revalidate();
        repaint();
    }

    public void setSortByBurstTime(boolean sort) {
        this.sortByBurstTime = sort;
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

        // Background
        g2.setColor(Theme.BG_PANEL);
        g2.fillRect(0, 0, W, H);

        if (scheduleResult == null || scheduleResult.getScheduledTasks().isEmpty()) {
            drawEmpty(g2, W, H);
            g2.dispose();
            return;
        }

        List<ScheduledTask> tasks = new java.util.ArrayList<>(scheduleResult.getScheduledTasks());
        if (sortByBurstTime) {
            tasks.sort(java.util.Comparator.comparingInt(ScheduledTask::getBurstTime));
        }

        int maxBurst = tasks.stream().mapToInt(ScheduledTask::getBurstTime).max().orElse(1);
        int minBurst = tasks.stream().mapToInt(ScheduledTask::getBurstTime).min().orElse(1);
        int chartW   = W - MARGIN_LEFT - MARGIN_RIGHT;
        int n        = tasks.size();

        // Chart title
        g2.setFont(Theme.FONT_HEADING);
        g2.setColor(Theme.ACCENT_TEAL);
        String titleStr = "Burst Time Comparison";
        g2.drawString(titleStr, MARGIN_LEFT, 22);

        // Sort toggle hint
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        String sortHint = sortByBurstTime ? "(sorted by BT)" : "(input order)";
        g2.drawString(sortHint, MARGIN_LEFT + 180, 22);

        // Time axis
        drawAxis(g2, W, H, chartW, maxBurst);

        // Bars
        for (int i = 0; i < n; i++) {
            ScheduledTask st  = tasks.get(i);
            int y             = MARGIN_TOP + i * (BAR_HEIGHT + BAR_GAP);
            boolean isShortest = st.getBurstTime() == minBurst;

            // Task ID label (left)
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_PRIMARY);
            String idLabel = st.getTaskId();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(idLabel, MARGIN_LEFT - fm.stringWidth(idLabel) - 8, y + BAR_HEIGHT / 2 + 4);

            // Bar
            int barW = (int)((double) st.getBurstTime() / maxBurst * chartW);
            barW     = Math.max(barW, 4);
            Color barCol = isShortest ? Theme.AMBER :
                           Theme.ACCENT_TEAL;

            // Bar shadow
            g2.setColor(Theme.withAlpha(Color.BLACK, 50));
            g2.fill(new RoundRectangle2D.Float(MARGIN_LEFT + 2, y + 2, barW, BAR_HEIGHT, 6, 6));

            // Bar fill
            GradientPaint gp = new GradientPaint(
                MARGIN_LEFT, y, barCol,
                MARGIN_LEFT + barW, y, Theme.withAlpha(barCol, 180));
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(MARGIN_LEFT, y, barW, BAR_HEIGHT, 6, 6));

            // Bar border
            g2.setColor(Theme.withAlpha(barCol, 200));
            g2.setStroke(new BasicStroke(1));
            g2.draw(new RoundRectangle2D.Float(MARGIN_LEFT, y, barW, BAR_HEIGHT, 6, 6));

            // Burst time label (right of bar)
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_PRIMARY);
            String btLabel = st.getBurstTime() + " units";
            fm = g2.getFontMetrics();
            int lblX = MARGIN_LEFT + barW + 6;
            if (lblX + fm.stringWidth(btLabel) > W - 4) {
                lblX = MARGIN_LEFT + barW - fm.stringWidth(btLabel) - 6;
                g2.setColor(Theme.BG_MAIN);
            }
            g2.drawString(btLabel, lblX, y + BAR_HEIGHT / 2 + 4);

            // Shortest marker
            if (isShortest) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.setColor(Theme.AMBER);
                g2.drawString("★ SHORTEST", MARGIN_LEFT + barW + 6 + 60, y + BAR_HEIGHT / 2 + 4);
            }
        }

        // Legend
        drawLegend(g2, W, H);

        g2.dispose();
    }

    private void drawAxis(Graphics2D g2, int W, int H, int chartW, int maxBurst) {
        g2.setColor(Theme.BORDER);
        g2.setStroke(new BasicStroke(1));
        // Vertical axis line
        g2.drawLine(MARGIN_LEFT, MARGIN_TOP - 5,
            MARGIN_LEFT, H - MARGIN_BOTTOM);
        // Horizontal base line
        g2.drawLine(MARGIN_LEFT, H - MARGIN_BOTTOM,
            MARGIN_LEFT + chartW, H - MARGIN_BOTTOM);

        // Tick marks along horizontal axis
        int ticks = Math.min(maxBurst, 10);
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        for (int i = 0; i <= ticks; i++) {
            int val  = (int) Math.round((double) i / ticks * maxBurst);
            int tickX = MARGIN_LEFT + (int)((double) val / maxBurst * chartW);
            g2.setColor(Theme.BORDER);
            g2.drawLine(tickX, H - MARGIN_BOTTOM, tickX, H - MARGIN_BOTTOM + 4);
            g2.setColor(Theme.TEXT_SECONDARY);
            FontMetrics fm = g2.getFontMetrics();
            String lbl = String.valueOf(val);
            g2.drawString(lbl, tickX - fm.stringWidth(lbl) / 2, H - MARGIN_BOTTOM + 15);
        }

        // Axis label
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString("Burst Time (time units)", MARGIN_LEFT + chartW / 2 - 60, H - 4);
    }

    private void drawLegend(Graphics2D g2, int W, int H) {
        int lx = W - 180;
        int ly = 8;
        g2.setFont(Theme.FONT_SMALL);

        drawLegendBox(g2, lx, ly, Theme.AMBER, "Shortest (next)");
        drawLegendBox(g2, lx, ly + 16, Theme.ACCENT_TEAL, "Other tasks");
    }

    private void drawLegendBox(Graphics2D g2, int x, int y, Color c, String label) {
        g2.setColor(c);
        g2.fillRoundRect(x, y, 12, 10, 3, 3);
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString(label, x + 16, y + 10);
    }

    private void drawEmpty(Graphics2D g2, int W, int H) {
        g2.setFont(Theme.FONT_BODY);
        g2.setColor(Theme.TEXT_DISABLED);
        String msg = "Add tasks to see burst time chart";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2);
    }
}
