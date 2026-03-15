package zoro.terminal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Line {
    private final List<Cell> cells;
    private boolean wrapped = false;

    public Line(int width, short initialAttributes) {
        this.cells = new ArrayList<>(width);
        for (int i = 0; i < width; i++) {
            this.cells.add(Cell.empty(initialAttributes));
        }
    }

    public List<Cell> getLine() {
        return cells;
    }

    public int getWidth() {
        return cells.size();
    }

    public boolean isWrapped() {
        return wrapped;
    }

    public void setWrapped(boolean wrapped) {
        this.wrapped = wrapped;
    }

    public void clearLine(short attributes) {
        for (int i = 0; i < cells.size(); i++) {
            cells.set(i, Cell.empty(attributes));
        }
    }

    public List<Cell> snapshot() {
        return Collections.unmodifiableList(cells);
    }
}
