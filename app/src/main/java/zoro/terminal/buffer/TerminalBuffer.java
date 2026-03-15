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
    private int width;
    private int height;
    private final int maxScrollback;

    // State
    private final Deque<Line> scrollback;
    private final Deque<Line> scrollForward;
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

    // Selection State
    private int selStartX = -1;
    private int selStartY = -1;
    private int selEndX = -1;
    private int selEndY = -1;

    public TerminalBuffer(int width, int height, int maxScrollback) {
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;
        this.scrollback = new ArrayDeque<>();
        this.scrollForward = new ArrayDeque<>();
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
        // Snap back to present if we are scrolled up
        while (!scrollForward.isEmpty()) {
            scrollback.addLast(screen.remove(0));
            screen.add(scrollForward.removeFirst());
            if (scrollback.size() > maxScrollback) scrollback.removeFirst();
        }
        
        List<Cell> line = screen.get(this.cursorY).getLine();
        short attr = packAttributes();
        
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            
            if (this.insertMode) {
                // Shift remainder of the line to the right
                for (int j = width - 1; j > this.cursorX; j--) {
                    line.set(j, line.get(j - 1));
                }
            }
            
            line.set(this.cursorX, new Cell(ch, attr, CellKind.NORMAL));
            
            if (this.cursorX < width - 1) {
                this.cursorX++;
            }
            else {
                screen.get(this.cursorY).setWrapped(true);
                this.cursorX = 0;
                insertLine();
                line = screen.get(this.cursorY).getLine();
            }
        }
    }

    public void insertLine() {
        // Snap back to present if we are scrolled up
        while (!scrollForward.isEmpty()) {
            scrollback.addLast(screen.remove(0));
            screen.add(scrollForward.removeFirst());
            if (scrollback.size() > maxScrollback) scrollback.removeFirst();
        }

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
            // Save the line that falls off the bottom so we can come back to it.
            scrollForward.addFirst(screen.remove(screen.size() - 1));
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
    }

    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;
        if (this.width == newWidth && this.height == newHeight) return;

        List<Line> allLines = getAllLines();
        int targetPhysLine = scrollback.size() + this.cursorY;
        int logicalLineOfCursor = -1;
        int cellIndexOfCursor = -1;

        List<List<Cell>> logicalLines = new ArrayList<>();
        List<Cell> currentLogical = new ArrayList<>();

        for (int row = 0; row < allLines.size(); row++) {
            Line l = allLines.get(row);
            boolean hasCursor = (row == targetPhysLine);
            
            int end = l.isWrapped() ? l.getWidth() - 1 : getLastNonEmpty(l);
            if (hasCursor && this.cursorX > end) {
                end = this.cursorX;
            }
            
            if (hasCursor) {
                logicalLineOfCursor = logicalLines.size();
                cellIndexOfCursor = currentLogical.size() + this.cursorX;
            }

            for(int i = 0; i <= end; i++) {
                currentLogical.add(l.getLine().get(i));
            }
            
            if (!l.isWrapped()) {
                logicalLines.add(currentLogical);
                currentLogical = new ArrayList<>();
            }
        }
        if (!currentLogical.isEmpty()) {
            logicalLines.add(currentLogical);
        }

        List<Line> newAllLines = new ArrayList<>();
        int newCursorRow = -1;
        int newCursorCol = -1;
        short attr = packAttributes();

        for (int i = 0; i < logicalLines.size(); i++) {
            List<Cell> logical = logicalLines.get(i);
            if (logical.isEmpty()) {
                if (i == logicalLineOfCursor) {
                    newCursorRow = newAllLines.size();
                    newCursorCol = 0;
                }
                newAllLines.add(new Line(newWidth, attr));
                continue;
            }
            
            int startOfPhys = 0;
            while (startOfPhys < logical.size()) {
                Line phys = new Line(newWidth, attr);
                int copyLen = Math.min(newWidth, logical.size() - startOfPhys);
                for (int j = 0; j < copyLen; j++) {
                    phys.getLine().set(j, logical.get(startOfPhys + j));
                }
                
                if (i == logicalLineOfCursor) {
                    if (cellIndexOfCursor >= startOfPhys && cellIndexOfCursor < startOfPhys + newWidth) {
                        newCursorRow = newAllLines.size();
                        newCursorCol = cellIndexOfCursor - startOfPhys;
                    } else if (cellIndexOfCursor == logical.size() && startOfPhys + copyLen == logical.size()) {
                        if (copyLen < newWidth) {
                            newCursorRow = newAllLines.size();
                            newCursorCol = copyLen;
                        }
                    }
                }
                
                startOfPhys += copyLen;
                if (startOfPhys < logical.size()) {
                    phys.setWrapped(true);
                }
                newAllLines.add(phys);
            }
            if (i == logicalLineOfCursor && newCursorRow == -1) {
                newCursorRow = newAllLines.size();
                newCursorCol = 0;
                newAllLines.add(new Line(newWidth, attr));
            }
        }

        if (newAllLines.isEmpty()) newAllLines.add(new Line(newWidth, attr));
        if (newCursorRow == -1) { newCursorRow = 0; newCursorCol = 0; }

        int cursorScreenOffset = Math.min(newHeight - 1, this.cursorY);
        int screenStart = Math.max(0, newCursorRow - cursorScreenOffset);
        if (screenStart + newHeight > newAllLines.size()) {
            screenStart = Math.max(0, newAllLines.size() - newHeight);
        }

        this.scrollback.clear();
        this.screen.clear();
        this.scrollForward.clear();
        
        for (int i = 0; i < newAllLines.size(); i++) {
            if (i < screenStart) {
                scrollback.add(newAllLines.get(i));
                if (scrollback.size() > maxScrollback) scrollback.removeFirst();
            } else if (i < screenStart + newHeight) {
                screen.add(newAllLines.get(i));
            } else {
                scrollForward.add(newAllLines.get(i));
            }
        }
        
        while (screen.size() < newHeight) {
            screen.add(new Line(newWidth, attr));
        }

        this.width = newWidth;
        this.height = newHeight;
        this.cursorX = newCursorCol;
        this.cursorY = newCursorRow - screenStart;
        if (this.cursorY < 0) this.cursorY = 0;
        
        this.clearSelection();
    }

    private int getLastNonEmpty(Line l) {
        List<Cell> cells = l.getLine();
        for (int i = cells.size() - 1; i >= 0; i--) {
            Cell c = cells.get(i);
            if (c.getCharacter() != ' ' || c.getBackgroundColor() != 0 || c.getStyle() != 0 || c.getForegroundColor() != 7) {
                return i;
            }
        }
        return -1;
    }

    public void scrollUp() {
        if (!scrollback.isEmpty()) {
            scrollForward.addFirst(screen.remove(screen.size() - 1));
            screen.add(0, scrollback.removeLast());
        }
    }

    public void scrollDown() {
        if (!scrollForward.isEmpty()) {
            scrollback.addLast(screen.remove(0));
            screen.add(scrollForward.removeFirst());
        }
    }

    public void toggleInsertMode() {
        this.insertMode = !this.insertMode;
    }

    public boolean isInsertMode() {
        return this.insertMode;
    }

    public void setSelectionStart(int x, int y) {
        selStartX = Math.max(0, Math.min(width - 1, x));
        selStartY = Math.max(0, Math.min(getAllLines().size() - 1, scrollback.size() + y));
        selEndX = selStartX;
        selEndY = selStartY;
    }

    public void setSelectionEnd(int x, int y) {
        selEndX = Math.max(0, Math.min(width - 1, x));
        selEndY = Math.max(0, Math.min(getAllLines().size() - 1, scrollback.size() + y));
    }

    public void clearSelection() {
        selStartX = -1;
        selStartY = -1;
        selEndX = -1;
        selEndY = -1;
    }

    public boolean hasSelection() {
        return selStartX != -1;
    }

    public boolean isSelected(int screenX, int screenY) {
        if (!hasSelection()) return false;
        int absY = scrollback.size() + screenY;
        
        int startY = Math.min(selStartY, selEndY);
        int endY = Math.max(selStartY, selEndY);
        int startX = selStartY < selEndY ? selStartX : (selStartY > selEndY ? selEndX : Math.min(selStartX, selEndX));
        int endX = selStartY < selEndY ? selEndX : (selStartY > selEndY ? selStartX : Math.max(selStartX, selEndX));

        if (absY < startY || absY > endY) return false;
        if (absY == startY && absY == endY) {
            return screenX >= startX && screenX <= endX;
        }
        if (absY == startY) return screenX >= startX;
        if (absY == endY) return screenX <= endX;
        return true;
    }

    public String getSelectedText() {
        if (!hasSelection()) return "";
        int startY = Math.min(selStartY, selEndY);
        int endY = Math.max(selStartY, selEndY);
        int startX = selStartY < selEndY ? selStartX : (selStartY > selEndY ? selEndX : Math.min(selStartX, selEndX));
        int endX = selStartY < selEndY ? selEndX : (selStartY > selEndY ? selStartX : Math.max(selStartX, selEndX));

        List<Line> lines = getAllLines();
        StringBuilder sb = new StringBuilder();

        for (int y = startY; y <= endY; y++) {
            if (y >= lines.size()) break;
            Line line = lines.get(y);
            int x1 = (y == startY) ? startX : 0;
            int x2 = (y == endY) ? endX : line.getWidth() - 1;
            
            StringBuilder lineStr = new StringBuilder();
            for (int x = x1; x <= x2; x++) {
                int ch = line.getLine().get(x).getCharacter();
                lineStr.append(ch == 0 ? ' ' : (char)ch);
            }
            
            // Trim right side of lines that aren't the last selected line
            String lStr = lineStr.toString();
            if (y < endY) {
                int lastChar = lStr.length() - 1;
                while(lastChar >= 0 && lStr.charAt(lastChar) == ' ') lastChar--;
                sb.append(lStr.substring(0, lastChar + 1)).append("\n");
            } else {
                sb.append(lStr);
            }
        }
        return sb.toString();
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

    public void handleDelete() {
        if (this.cursorX < width - 1) {
            screen.get(this.cursorY).getLine().set(this.cursorX, Cell.empty(packAttributes()));
            // Shift the rest of the line left
            for (int i = this.cursorX; i < width - 1; i++) {
                Cell nextCell = screen.get(this.cursorY).getLine().get(i + 1);
                screen.get(this.cursorY).getLine().set(i, nextCell);
            }
            screen.get(this.cursorY).getLine().set(width - 1, Cell.empty(packAttributes()));
        } else if (this.cursorY < height - 1) {
            // Shift lines up
            for (int i = this.cursorY; i < height - 1; i++) {
                screen.set(i, screen.get(i + 1));
            }
            screen.set(height - 1, new Line(width, packAttributes()));
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
