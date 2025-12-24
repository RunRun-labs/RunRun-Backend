package com.multi.runrunbackend.domain.course.util.thumbnail;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LocalDrawThumbnailFallback {

    public byte[] draw(List<double[]> coords,
        int width,
        int height,
        int padding,
        int strokeWidth,
        int markerRadius) {

        if (coords == null || coords.size() < 2) {
            return null;
        }

        int glowWidth = Math.max(strokeWidth + 6, strokeWidth * 2);

        int safePadding = Math.max(padding, 20 + glowWidth + markerRadius);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            drawStylishBackground(g2d, width, height);
            drawSubtleGrid(g2d, width, height);

            // bounds
            double minLng = Double.MAX_VALUE, maxLng = -Double.MAX_VALUE;
            double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;

            for (double[] c : coords) {
                minLng = Math.min(minLng, c[0]);
                maxLng = Math.max(maxLng, c[0]);
                minLat = Math.min(minLat, c[1]);
                maxLat = Math.max(maxLat, c[1]);
            }

            double dx = Math.max(maxLng - minLng, 1e-9);
            double dy = Math.max(maxLat - minLat, 1e-9);

            // ✅ 비율 유지 + 중앙 정렬
            double scaleX = (width - safePadding * 2.0) / dx;
            double scaleY = (height - safePadding * 2.0) / dy;
            double scale = Math.min(scaleX, scaleY);

            double usedW = dx * scale;
            double usedH = dy * scale;
            double offsetX = (width - usedW) / 2.0;
            double offsetY = (height - usedH) / 2.0;

            final double fMinLng = minLng;
            final double fMaxLat = maxLat;

            Function<double[], Point2D> toPoint = coord -> {
                double lng = coord[0];
                double lat = coord[1];
                double x = offsetX + (lng - fMinLng) * scale;
                double y = offsetY + (fMaxLat - lat) * scale;
                return new Point2D.Double(x, y);
            };

            Path2D path = new Path2D.Double();
            Point2D first = toPoint.apply(coords.get(0));
            path.moveTo(first.getX(), first.getY());
            for (int i = 1; i < coords.size(); i++) {
                Point2D p = toPoint.apply(coords.get(i));
                path.lineTo(p.getX(), p.getY());
            }

            // ✅ 두 겹(그림자/글로우 + 메인)
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.setStroke(
                new BasicStroke(glowWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(path);

            g2d.setColor(new Color(0xFF3D00));
            g2d.setStroke(
                new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(path);

            Point2D start = toPoint.apply(coords.get(0));
            Point2D end = toPoint.apply(coords.get(coords.size() - 1));

            boolean isLoop = start.distance(end) < 6.0;
            if (isLoop) {
                drawRingMarker(g2d, start, markerRadius + 2, new Color(255, 255, 255, 220),
                    new Color(0, 0, 0, 160));
            } else {
                drawFilledMarker(g2d, start, markerRadius + 1, new Color(0, 0, 0, 160));
                drawFilledMarker(g2d, start, markerRadius, new Color(0x00C853));

                drawFilledMarker(g2d, end, markerRadius + 1, new Color(0, 0, 0, 160));
                drawFilledMarker(g2d, end, markerRadius, new Color(0xD50000));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.warn("[LocalDrawThumbnailFallback] draw failed: {}", e.getMessage());
            return null;
        } finally {
            g2d.dispose();
        }
    }

    private void drawStylishBackground(Graphics2D g2d, int w, int h) {
        GradientPaint gp = new GradientPaint(
            0, 0, new Color(18, 18, 22),
            w, h, new Color(30, 20, 18)
        );
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, w, h);

        // 약한 비네팅
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillRect(0, 0, w, 12);
        g2d.fillRect(0, h - 12, w, 12);
    }

    private void drawSubtleGrid(Graphics2D g2d, int w, int h) {
        g2d.setColor(new Color(255, 255, 255, 18));
        g2d.setStroke(new BasicStroke(1f));

        int step = 24;
        for (int x = 0; x <= w; x += step) {
            g2d.drawLine(x, 0, x, h);
        }
        for (int y = 0; y <= h; y += step) {
            g2d.drawLine(0, y, w, y);
        }
    }

    private void drawFilledMarker(Graphics2D g2d, Point2D p, int r, Color color) {
        g2d.setColor(color);
        int x = (int) Math.round(p.getX() - r);
        int y = (int) Math.round(p.getY() - r);
        g2d.fillOval(x, y, r * 2, r * 2);
    }

    private void drawRingMarker(Graphics2D g2d, Point2D p, int r, Color ring, Color inner) {
        drawFilledMarker(g2d, p, r, ring);
        drawFilledMarker(g2d, p, Math.max(1, r - 3), inner);
    }
}
