import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;

/**
 * Generates the Nexus application icon (nexus.ico) at 16, 32, 48 and 256 px.
 *
 * Run with:  java tools/GenerateIcon.java
 */
public class GenerateIcon {

    // ── Brand colours ──────────────────────────────────────────────────────────
    private static final Color BG_TOP    = new Color(0x16, 0x1b, 0x22);   // #161b22
    private static final Color BG_BOT    = new Color(0x0d, 0x11, 0x17);   // #0d1117
    private static final Color ACCENT    = new Color(0x2f, 0x81, 0xf7);   // #2f81f7
    private static final Color ACCENT_DIM= new Color(0x2f, 0x81, 0xf7, 55);
    private static final Color NODE_CLR  = new Color(0xff, 0xff, 0xff, 220);

    public static void main(String[] args) throws Exception {
        int[] sizes = {16, 32, 48, 256};

        // Collect PNG bytes for each size
        byte[][] pngs = new byte[sizes.length][];
        for (int i = 0; i < sizes.length; i++) {
            BufferedImage img = drawIcon(sizes[i]);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", buf);
            pngs[i] = buf.toByteArray();
        }

        // Write ICO (modern format: PNG payloads inside ICO container)
        Path out = Path.of("src/main/resources/nexus.ico");
        Files.createDirectories(out.getParent());
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(out))) {
            // ICONDIR header (6 bytes)
            writeU16(os, 0);              // reserved
            writeU16(os, 1);              // type = icon
            writeU16(os, sizes.length);   // image count

            // Directory entries (16 bytes each)
            int offset = 6 + sizes.length * 16;
            for (int i = 0; i < sizes.length; i++) {
                int dim = sizes[i] >= 256 ? 0 : sizes[i];  // 0 means 256
                os.write(dim);        // width
                os.write(dim);        // height
                os.write(0);          // colour count
                os.write(0);          // reserved
                writeU16(os, 1);      // planes
                writeU16(os, 32);     // bits-per-pixel
                writeU32(os, pngs[i].length);
                writeU32(os, offset);
                offset += pngs[i].length;
            }

            // PNG payloads
            for (byte[] png : pngs) os.write(png);
        }

        System.out.println("Icon written to " + out.toAbsolutePath());
    }

    // ── Drawing ────────────────────────────────────────────────────────────────

    static BufferedImage drawIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,      RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,       RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        float s  = size;
        float r  = s * 0.20f;   // corner radius

        // ── Background ────────────────────────────────────────────────────────
        Shape bg = new RoundRectangle2D.Float(0, 0, s, s, r, r);
        g.setPaint(new GradientPaint(0, 0, BG_TOP, 0, size, BG_BOT));
        g.fill(bg);

        // Subtle accent glow behind the letter (only visible at larger sizes)
        if (size >= 48) {
            float cx = s / 2f, cy = s / 2f;
            RadialGradientPaint glow = new RadialGradientPaint(
                cx, cy, s * 0.55f,
                new float[]{0f, 1f},
                new Color[]{new Color(0x2f, 0x81, 0xf7, 28), new Color(0, 0, 0, 0)}
            );
            g.setPaint(glow);
            g.fill(bg);
        }

        // ── Stylised "N" ──────────────────────────────────────────────────────
        float margin = s * 0.195f;
        float lx  = margin;            // left  x
        float rx  = s - margin;        // right x
        float ty  = s * 0.185f;        // top   y
        float by  = s - s * 0.185f;   // bottom y
        float sw  = s * 0.105f;        // stroke width
        float sw2 = s * 0.22f;        // glow width

        // Outer glow pass
        g.setStroke(new BasicStroke(sw2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(ACCENT_DIM);
        drawN(g, lx, rx, ty, by);

        // Main stroke
        g.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(ACCENT);
        drawN(g, lx, rx, ty, by);

        // ── Node dots at the four corners ────────────────────────────────────
        if (size >= 32) {
            float nr = sw * 0.65f;   // dot radius
            g.setColor(NODE_CLR);
            fillDot(g, lx, ty, nr);
            fillDot(g, lx, by, nr);
            fillDot(g, rx, ty, nr);
            fillDot(g, rx, by, nr);
        }

        g.dispose();
        return img;
    }

    private static void drawN(Graphics2D g, float lx, float rx, float ty, float by) {
        g.draw(new Line2D.Float(lx, ty, lx, by));   // left bar
        g.draw(new Line2D.Float(lx, ty, rx, by));   // diagonal
        g.draw(new Line2D.Float(rx, ty, rx, by));   // right bar
    }

    private static void fillDot(Graphics2D g, float cx, float cy, float r) {
        g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
    }

    // ── ICO binary helpers ────────────────────────────────────────────────────

    static void writeU16(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8) & 0xFF);
    }

    static void writeU32(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write((v >> 8)  & 0xFF);
        os.write((v >> 16) & 0xFF);
        os.write((v >> 24) & 0xFF);
    }
}
