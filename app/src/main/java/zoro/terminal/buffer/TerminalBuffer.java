package zoro.terminal.buffer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;

import zoro.terminal.model.Line;

public class TerminalBuffer {
    // Configuration
    private final int width;
    private final int height;
    private final int maxScrollback;

    // State
    private final Deque<Line> scrollback;
    private final List<Line> screen;
    private int cursorX;
    private int cursorY;
    private boolean insertMode;

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
        this.scrollback = new ArrayDeque<>();
        this.screen = new ArrayList<>(height);
        this.cursorX = 0;
        this.cursorY = 0;
        this.currentFg = 7;
        this.currentBg = 0;

        for (int i = 0; i < height; i++) {
            this.screen.add(new Line(width, packAttributes()));
        }
    }

    public void write(String text) {
        // TODO: implement write logic with wrapping and wide-character handling.
        List<Cell> cells = screen.get(cursorY).getLine();
        short attr = packAttributes();
        
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            cells.set(cursorX, new Cell(ch, attr, CellKind.NORMAL));
            cursorX = Math.min(cursorX + 1, width - 1);
        }
    }

    public void insertLine() {
        // TODO: implement rolling window line insertion.
        if (cursorY == height - 1) {
            // Pushing down creates a new line and pushes top to history
            scrollback.addLast(screen.remove(0));
            if (scrollback.size() > maxScrollback) {
                scrollback.removeFirst();
            }
            screen.add(new Line(width, packAttributes()));
        } else {
            cursorY++;
        }
        cursorX = 0;
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
        } else if (cursorY > 0) {
            cursorY--;
            cursorX = width - 1;
        }
    }

    private void moveCursorRight(){
        // TODO: implement cursor movement right.
        if (cursorX < width - 1) {
            cursorX++;
        } else if (cursorY < height - 1) {
            cursorY++;
            cursorX = 0;
        }
    }

    private void moveCursorUp(){
        // TODO: implement cursor movement up.
        if (cursorY > 0) {
            cursorY--;
        } else if (cursorY == 0 && !scrollback.isEmpty()) {
            // Scroll screen down into history.
            screen.remove(screen.size() - 1);
            screen.add(0, scrollback.removeLast());
        }
    }

    private void moveCursorDown(){
        // TODO: implement cursor movement down.
        if (cursorY < height - 1) {
           cursorY++;
        } else if (cursorY == height - 1) {
            // Scroll screen up, saving top line to history.
            scrollback.addLast(screen.remove(0));
            if (scrollback.size() > maxScrollback) {
                scrollback.removeFirst();
            }
            screen.add(new Line(width, packAttributes()));
        }
    }

    public void clearScreen() {
        for (Line line : screen) {
            line.clearLine(packAttributes());
        }
    }

    public void clearAll() {
        scrollback.clear();
        screen.clear();
        for (int i = 0; i < height; i++) {
            this.screen.add(new Line(width, packAttributes()));
        }
        cursorX = 0;
        cursorY = 0;
    }

    public void toggleInsertMode() {
        if (this.insertMode) {
            // TODO: maybe change the cursor shape or color to indicate insert mode.
            this.cursorX = Math.max(cursorX - 1, 0);
        } else {
            // Revert cursor shape or color if needed.
            this.cursorX = Math.min(cursorX + 1, width - 1);
        }
        this.insertMode = !this.insertMode;
    }

    public void handleBackspace() {
        // TODO: implement backspace handling.
        if (cursorX > 0) {
            cursorX--;
            screen.get(cursorY).getLine().set(cursorX, Cell.empty(packAttributes()));
        } else if (cursorY > 0) {
            cursorY--;
            cursorX = width - 1;
            screen.get(cursorY).getLine().set(cursorX, Cell.empty(packAttributes()));
        } else if (cursorY == 0 && !scrollback.isEmpty()) {
            // Pull previous line from scrollback
            screen.remove(screen.size() - 1);
            screen.add(0, scrollback.removeLast());
            cursorX = width - 1;
            screen.get(cursorY).getLine().set(cursorX, Cell.empty(packAttributes()));
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

    public List<Line> getScreen() {
        return screen;
    }


    public List<Line> getAllLines() {
        List<Line> combined = new ArrayList<>(scrollback);
        combined.addAll(screen);
        return combined;
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
