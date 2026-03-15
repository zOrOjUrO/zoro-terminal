package zoro.terminal.model;


public final class Cell {
    private int character;
    private short attributes; // 0-3: foreground color, 4-7: bg color, 8 - bold, 9 - italic, 10 - underline
    private CellKind kind;

    public Cell(int character, short attributes, CellKind kind) {
        this.character = character;
        this.attributes = attributes;
        this.kind = kind;
    }

    public static Cell empty(short attributes) {
        return new Cell(' ', attributes, CellKind.EMPTY);
    }

    public static Cell continuation(short attributes) {
        return new Cell(' ', attributes, CellKind.CONTINUATION);
    }

    public static Cell normal(short attributes) {return new Cell(' ', attributes, CellKind.NORMAL);}

    public int getCharacter() {
        return this.character;
    }

    public void setCharacter(int character) {
        this.character = character;
    }

    public short getAttributes() {
        return this.attributes;
    }

    public void setAttributes(short attributes) {
        this.attributes = attributes;
    }

    public int getForegroundColor() {
        return this.attributes & 15;
    }

    public int getBackgroundColor() {
        return (this.attributes & 240)>>>4;
    }

    public int getStyle() {
        return this.attributes & (0xF << 10); // B | I | U
    }

    public void setStyle(boolean isBold, boolean isItalic, boolean isUnderline){
        int style = ((isBold ? 1 : 0) << 8)
                | ((isItalic ? 1 : 0) << 9)
                | ((isUnderline ? 1 : 0) << 10);
        this.attributes |= (short)style;
    }

    public CellKind getKind() {
        return this.kind;
    }

    public void setKind(CellKind kind) {
        this.kind = kind;
    }
}
