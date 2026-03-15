package zoro.terminal.buffer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;

import zoro.terminal.model.Cell;
import zoro.terminal.model.CellKind;
import zoro.terminal.model.Line;

public class TerminalBuffer {
    // Configuration
    private final int width;
    private final int height;
    private final int maxScrollback;

    // State
    private final Deque<Line> scrollback;
    private final Deque<Line> scrollForward;
    private final List<Line> screen;
    private int cursorX;
    private int cursorY;
    private boolean insertMode;    
    private int viewOffset; // 0 = viewing active screen, > 0 = scrolling back into history
    
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
        this.scrollForward = new ArrayDeque<>();
        this.screen = new ArrayList<>(height);
        this.cursorX = 0;
        this.cursorY = 0;
        this.viewOffset = 0;
        this.currentFg = 7;
        this.currentBg = 0;

        for (int i = 0; i < height; i++) {
            this.screen.add(new Line(width, packAttributes()));
        }
    }

    public void write(String text) {
        this.viewOffset = 0; // Snap to bottom when typing
        List<Cell> line = screen.get(this.cursorY).getLine();
        short attr = packAttributes();
        
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            line.set(this.cursorX, new Cell(ch, attr, CellKind.NORMAL));
            if (this.cursorX < width - 1) {
                this.cursorX++;
            }
            else {
                this.cursorX = 0;
                insertLine();
                line = screen.get(this.cursorY).getLine();
            }
        }
    }

    public void insertLine() {
        this.viewOffset = 0; // Snap to bottom when pushing new lines
        if (this.cursorY == this.height - 1) {
            scrollback.addLast(screen.remove(0));
            if (scrollback.size() > maxScrollback) {
                scrollback.removeFirst();
            }
            if (!scrollForward.isEmpty()) {
                screen.add(scrollForward.removeFirst());
            } else {
                screen.add(new Line(width, packAttributes()));
            }
        } else {
            this.cursorY++;
        }
        this.cursorX = 0;
    }

    public void moveCursor(short direction) {
        switch (direction) {
            case 0: moveCursorUp(); break;
            case 1: moveCursorDown(); break;
            case 2: moveCursorLeft(); break;
            case 3: moveCursorRight(); break;
        }
    }

    private void moveCursorLeft(){
        if (this.cursorX > 0) {
            this.cursorX--;
        } else if (this.cursorY > 0) {
            this.cursorY--;
            this.cursorX = width - 1;
        }
    }

    private void moveCursorRight(){
        if (this.cursorX < width - 1) {
            this.cursorX++;
        } else if (this.cursorY < height - 1) {
            this.cursorY++;
            this.cursorX = 0;
        }
    }

    private void moveCursorUp(){
        if (this.cursorY > 0) {
            this.cursorY--;
        } else if (this.cursorY == 0 && !scrollback.isEmpty()) {
            // Scroll screen down into history.
            screen.remove(screen.size() - 1);
            screen.add(0, scrollback.removeLast());
        }
    }

    private void moveCursorDown(){
        if (this.cursorY < height - 1) {
           this.cursorY++;
        } else if (this.cursorY == height - 1) {
            // Scroll screen up, saving top line to history.
            scrollback.addLast(screen.remove(0));
            if (scrollback.size() > maxScrollback) {
                scrollback.removeFirst();
            }
            if (!scrollForward.isEmpty()) {
                screen.add(scrollForward.removeFirst());
            } else {
                screen.add(new Line(width, packAttributes()));
            }
        }
    }

    public void clearScreen() {
        for (Line line : screen) {
            line.clearLine(packAttributes());
        }
    }

    public void clearAll() {
        scrollback.clear();
        scrollForward.clear();
        screen.clear();
        for (int i = 0; i < height; i++) {
            this.screen.add(new Line(width, packAttributes()));
        }
        this.cursorX = 0;
        this.cursorY = 0;
        this.viewOffset = 0;
    }

    public void scrollUp() {
        if (this.viewOffset < scrollback.size()) {
            this.viewOffset++;
        }
    }

    public void scrollDown() {
        if (this.viewOffset > 0) {
            this.viewOffset--;
        }
    }

    public void pageUp() {
        this.viewOffset = Math.min(scrollback.size(), this.viewOffset + Math.max(1, height / 2));
    }

    public void pageDown() {
        this.viewOffset = Math.max(0, this.viewOffset - Math.max(1, height / 2));
    }

    public void toggleInsertMode() {
        if (this.insertMode) {
            // TODO: maybe change the cursor shape or color to indicate insert mode.
            this.cursorX = Math.max(this.cursorX - 1, 0);
        } else {
            // Revert cursor shape or color if needed.
            this.cursorX = Math.min(this.cursorX + 1, width - 1);
        }
        this.insertMode = !this.insertMode;
    }

    public void handleBackspace() {
        if (this.cursorX > 0) {
            this.cursorX--;
            screen.get(this.cursorY).getLine().set(this.cursorX, Cell.empty(packAttributes()));
        } else if (this.cursorY > 0) {
            this.cursorY--;
            this.cursorX = width - 1;
            screen.get(this.cursorY).getLine().set(this.cursorX, Cell.empty(packAttributes()));
        } else if (this.cursorY == 0 && !scrollback.isEmpty()) {
            // Pull previous line from scrollback
            scrollForward.addFirst(screen.remove(screen.size() - 1));
            screen.remove(screen.size() - 1);
            screen.add(0, scrollback.removeLast());
            this.cursorX = width - 1;
            screen.get(this.cursorY).getLine().set(this.cursorX, Cell.empty(packAttributes()));
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
        if (viewOffset == 0) {
            return screen;
        }
        
        List<Line> combined = getAllLines();
        // Calculate the slice visible based on viewOffset relative to the current active screen
        int currentScreenStart = scrollback.size();
        int start = Math.max(0, currentScreenStart - viewOffset);
        int end = Math.min(combined.size(), start + height);
        return combined.subList(start, end);
    }

    public int getViewOffset() {
        return viewOffset;
    }

    public List<Line> getAllLines() {
        List<Line> combined = new ArrayList<>(scrollback);
        combined.addAll(screen);
        combined.addAll(scrollForward);
        return combined;
    }

    public int getCursorX() {
        return this.cursorX;
    }

    public int getCursorY() {
        return this.cursorY;
    }

    public int getCurrentFg() {
        return this.currentFg;
    }

    public void setCurrentFg(int currentFg) {
        this.currentFg = currentFg;
    }

    public int getCurrentBg() {
        return this.currentBg;
    }

    public void setCurrentBg(int currentBg) {
        this.currentBg = currentBg;
    }

    public boolean isBold() {
        return this.isBold;
    }

    public void setBold(boolean bold) {
        this.isBold = bold;
    }

    public boolean isItalic() {
        return this.isItalic;
    }

    public void setItalic(boolean italic) {
        this.isItalic = italic;
    }

    public boolean isUnderline() {
        return this.isUnderline;
    }

    public void setUnderline(boolean underline) {
        this.isUnderline = underline;
    }
}
