package zoro.terminal.buffer;

import java.util.ArrayDeque;
import java.util.Deque;

import zoro.terminal.model.Line;

public class TerminalBuffer {
    // Configuration
    private final int width;
    private final int height;
    private final int maxScrollback;

    // State
    private final Deque<Line> allLines;
    private int cursorX;
    private int cursorY;

    // Current pen attributes
    private int currentFg;
    private int currentBg;
    private boolean isBold;
    private boolean isItalic;
    private boolean isUnderline;

    public TerminalBuffer(int width, int height, int maxScrollback) {
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;
        this.allLines = new ArrayDeque<>();
        this.cursorX = 0;
        this.cursorY = 0;
        this.currentFg = 7;
        this.currentBg = 0;

        for (int i = 0; i < height; i++) {
            this.allLines.addLast(new Line(width, packAttributes()));
        }
    }

    public void write(String text) {
        // TODO: implement write logic with wrapping and wide-character handling.
    }

    public void insertLine() {
        // TODO: implement rolling window line insertion.
    }

    public void moveCursor(short direction) {
        // TODO: implement cursor movement relative to screen.
        switch (direction) {
            case 0: moveCursorUp(); break;
            case 1: moveCursorDown(); break;
            case 2: moveCursorLeft(); break;
            case 3: moveCursorRight(); break;
        }
    }

    private void moveCursorLeft(){
        // TODO: implement cursor movement left.
        if (cursorX > 0) {
            cursorX--;
        }
    }

    private void moveCursorRight(){
        // TODO: implement cursor movement right.
        if (cursorX < width - 1) {
            cursorX++;
        }
    }

    private void moveCursorUp(){
        // TODO: implement cursor movement up.
        if (cursorY > 0) {
            cursorY--;
        }
    }

    private void moveCursorDown(){
        // TODO: implement cursor movement down.
        if (cursorY < height - 1) {
            cursorY++;
        }
    }

    public void clearScreen() {
        // TODO: clear only visible screen lines.
    }

    public void clearAll() {
        // TODO: wipe full deque and recreate screen region.
    }

    public void toggleInsertMode() {
        // TODO: toggle insert mode.
    }

    public void handleBackspace() {
        // TODO: implement backspace handling.
        if (cursorX > 0) {
            cursorX--;
        } else if (cursorY > 0) {
            cursorY--;
            cursorX = width - 1;
        }
    }


    public short packAttributes() {
        // 4 bits fg, 4 bits bg, 3 bits style flags.
        int packed = (currentFg & 0xF)
                | ((currentBg & 0xF) << 4)
                | ((isBold ? 1 : 0) << 8)
                | ((isItalic ? 1 : 0) << 9)
                | ((isUnderline ? 1 : 0) << 10);
        return (short) packed;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getMaxScrollback() {
        return maxScrollback;
    }

    public Deque<Line> getAllLines() {
        return allLines;
    }

    public int getCursorX() {
        return cursorX;
    }

    public int getCursorY() {
        return cursorY;
    }

    public int getCurrentFg() {
        return currentFg;
    }

    public void setCurrentFg(int currentFg) {
        this.currentFg = currentFg;
    }

    public int getCurrentBg() {
        return currentBg;
    }

    public void setCurrentBg(int currentBg) {
        this.currentBg = currentBg;
    }

    public boolean isBold() {
        return isBold;
    }

    public void setBold(boolean bold) {
        isBold = bold;
    }

    public boolean isItalic() {
        return isItalic;
    }

    public void setItalic(boolean italic) {
        isItalic = italic;
    }

    public boolean isUnderline() {
        return isUnderline;
    }

    public void setUnderline(boolean underline) {
        isUnderline = underline;
    }
}
