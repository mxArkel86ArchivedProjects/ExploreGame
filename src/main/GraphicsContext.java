package main;

import java.awt.*;

public class GraphicsContext {
    private Graphics2D g;

    public GraphicsContext(Graphics2D g) {
        this.g = g;
    }

    public Graphics2D getGraphics() {
        return g;
    }

    public void clear(int x, int y, int width, int height) {
        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(x, y, width, height);
    }
    
    public void drawArc(double x, double y, double width, double height, double startAngle, double arcAngle, Color c,
            int stroke) {
        g.setColor(c);
        g.setStroke(new BasicStroke(stroke));
        g.drawArc((int)x, (int)y, (int)width, (int)height, (int)startAngle, (int)arcAngle);
    }
    
    public void drawRect(double x, double y, double w, double h, Color c, float stroke) {
        g.setColor(c);
        g.setStroke(new java.awt.BasicStroke(stroke));
        g.drawRect((int) x, (int) y, (int) w, (int) h);
    }

    public void fillRect(double x, double y, double w, double h, Color c) {
        g.setColor(c);
        g.fillRect((int) x, (int) y, (int) w, (int) h);
    }

    public void drawImage(Image img, double x, double y, double w, double h) {
        g.drawImage(img, (int) x, (int) y, (int) w, (int) h, null);
    }
    
    public void drawCircle(double x, double y, double r, Color c, float stroke) {
        g.setColor(c);
        g.setStroke(new java.awt.BasicStroke(stroke));
        g.drawOval((int) (x-r), (int) (y-r), (int) r*2, (int) r*2);
    }

    public void fillCircle(double x, double y, double r, Color c) {
        g.setColor(c);
        g.fillOval((int) (x - r), (int) (y - r), (int) r*2, (int) r*2);
    }

    public void fillEllipse(double x, double y, double w, double h, Color c) {
        g.setColor(c);
        g.fillOval((int) x, (int) y, (int) w, (int) h);
    }
    
    public void drawEllipse(double x, double y, double w, double h, Color c, float stroke) {
        g.setColor(c);
        g.setStroke(new java.awt.BasicStroke(stroke));
        g.drawOval((int) x, (int) y, (int) w, (int) h);
    }

    public void drawLine(double x1, double y1, double x2, double y2, Color c, float stroke) {
        g.setColor(c);
        g.setStroke(new java.awt.BasicStroke(stroke));
        g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
    }

    public void drawString(String text, double x, double y, Color c, Font f) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(text, (int)x, (int)(y+g.getFontMetrics(f).getAscent()));
    }
}
