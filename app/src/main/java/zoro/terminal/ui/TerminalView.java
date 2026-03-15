package zoro.terminal.ui;

import zoro.terminal.buffer.TerminalBuffer;
import zoro.terminal.model.Cell;
import zoro.terminal.model.Line;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TerminalView extends JPanel {
    private final TerminalBuffer buffer;
    
    // Config : rendering properties
    private final int charWidth = 10;
    private final int charHeight = 18;
    private final Font font = new Font("Monospaced", Font.PLAIN, 14);

    // Standard 16-color ANSI palette
    private final Color[] colors = {
        Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW,
        Color.BLUE, Color.MAGENTA, Color.CYAN, Color.LIGHT_GRAY,
        Color.DARK_GRAY, new Color(255, 85, 85), new Color(85, 255, 85), new Color(255, 255, 85),
        new Color(85, 85, 255), new Color(255, 85, 255), new Color(85, 255, 255), Color.WHITE
    };

    public TerminalView(TerminalBuffer buffer) {
        this.buffer = buffer;
        this.setBackground(Color.BLACK);
        // Calculate size based on buffer dimensions
        int prefWidth = buffer.getWidth() * charWidth;
        int prefHeight = buffer.getHeight() * charHeight;
        this.setPreferredSize(new Dimension(prefWidth, prefHeight));
        this.setFocusable(true);
    }

    public int getCharWidth() {
        return charWidth;
    }

    public int getCharHeight() {
        return charHeight;
    }

    public void setPaletteColor(int index, Color color) {
        if (index >= 0 && index < colors.length) {
            colors[index] = color;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Optional: anti-aliasing for text
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setFont(font);
        
        FontMetrics fm = g2d.getFontMetrics();
        int fontAscent = fm.getAscent();

        List<Line> screenLines = buffer.getScreen();

        // Backgrounds & Characters
        for (int y = 0; y < screenLines.size(); y++) {
            Line line = screenLines.get(y);
            List<Cell> cells = line.getLine();
            
            for (int x = 0; x < cells.size(); x++) {
                Cell cell = cells.get(x);
                
                int drawX = x * charWidth;
                int drawY = y * charHeight;
                
                // Draw Background
                Color bgColor = colors[cell.getBackgroundColor() % 16];
                if (buffer.isSelected(x, y)) {
                    g2d.setColor(Color.WHITE); // Selection background color
                    g2d.fillRect(drawX, drawY, charWidth, charHeight);
                } else if (!bgColor.equals(Color.BLACK)) { // Avoid repaint black over black
                    g2d.setColor(bgColor);
                    g2d.fillRect(drawX, drawY, charWidth, charHeight);
                }

                // Draw Foreground Text
                int cp = cell.getCharacter();
                if (cp > 0) {
                    if (buffer.isSelected(x, y)) {
                        g2d.setColor(Color.BLACK); // Selection text color
                    } else {
                        g2d.setColor(colors[cell.getForegroundColor() % 16]);
                    }
                    
                    // Handle Styles (Bold, Italic)
                    int style = cell.getStyle();
                    boolean isBold = (style & (1 << 8)) != 0;
                    boolean isItalic = (style & (1 << 9)) != 0;
                    
                    int fontStyle = Font.PLAIN;
                    if (isBold) fontStyle |= Font.BOLD;
                    if (isItalic) fontStyle |= Font.ITALIC;
                    
                    if (fontStyle != Font.PLAIN) {
                        g2d.setFont(font.deriveFont(fontStyle));
                    } else {
                        g2d.setFont(font); // Reset if changed
                    }

                    // Draw actual character
                    g2d.drawString(String.valueOf((char) cp), drawX, drawY + fontAscent);
                    
                    // Handle Underline
                    boolean isUnderline = (style & (1 << 10)) != 0;
                    if (isUnderline) {
                        int underlineY = drawY + charHeight - 2;
                        g2d.drawLine(drawX, underlineY, drawX + charWidth, underlineY);
                    }
                }
            }
        }

        // Cursor
        int cx = buffer.getCursorX() * charWidth;
        int cy = buffer.getCursorY() * charHeight; 

        g2d.setColor(Color.WHITE); // Cursor color
        
        if (buffer.isInsertMode()) {
            // In insert mode, draw a vertical bar cursor
            g2d.fillRect(cx, cy, 2, charHeight);
        } else {
            // In overwrite mode, draw a block cursor
            g2d.fillRect(cx, cy, charWidth, charHeight);
            
            // Draw the character under the block cursor in invert (black on white)
            if (buffer.getCursorY() < screenLines.size()) {
                Line line = screenLines.get(buffer.getCursorY());
                if (buffer.getCursorX() < line.getWidth()) {
                    Cell cell = line.getLine().get(buffer.getCursorX());
                    int cp = cell.getCharacter();
                    if (cp > 0 && cp != ' ') {
                        g2d.setColor(Color.BLACK);
                        g2d.setFont(font);
                        g2d.drawString(String.valueOf((char) cp), cx, cy + fontAscent);
                    }
                }
            }
        }
    }
}
