package zoro.terminal.buffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zoro.terminal.model.Cell;
import zoro.terminal.model.Line;

import static org.junit.jupiter.api.Assertions.*;

class TerminalBufferTest {

    private TerminalBuffer buffer;
    private final int width = 10;
    private final int height = 5;
    private final int maxScrollback = 10;

    @BeforeEach
    void setUp() {
        buffer = new TerminalBuffer(width, height, maxScrollback);
    }

    @Test
    void testInitialState() {
        assertEquals(0, buffer.getCursorX());
        assertEquals(0, buffer.getCursorY());
        assertEquals(width, buffer.getWidth());
        assertEquals(height, buffer.getHeight());
        assertEquals(height, buffer.getScreen().size());
        
        for (Line line : buffer.getScreen()) {
            assertEquals(width, line.getWidth());
        }
    }

    @Test
    void testBasicWrite() {
        buffer.write("Hello");
        assertEquals(5, buffer.getCursorX());
        assertEquals(0, buffer.getCursorY());
        
        Line firstLine = buffer.getScreen().get(0);
        assertEquals('H', firstLine.getLine().get(0).getCharacter());
        assertEquals('e', firstLine.getLine().get(1).getCharacter());
        assertEquals('l', firstLine.getLine().get(2).getCharacter());
        assertEquals('l', firstLine.getLine().get(3).getCharacter());
        assertEquals('o', firstLine.getLine().get(4).getCharacter());
    }

    @Test
    void testWriteWrap_LineInsertion() {
        buffer.write("HelloWorld1"); // 11 chars on width 10 
        assertEquals(1, buffer.getCursorX()); // Wrapped around to index 1
        assertEquals(1, buffer.getCursorY()); // Moved down one line
        
        Line line0 = buffer.getScreen().get(0);
        Line line1 = buffer.getScreen().get(1);
        
        assertEquals('o', line0.getLine().get(4).getCharacter());
        assertEquals('d', line0.getLine().get(9).getCharacter()); // 10th char
        assertEquals('1', line1.getLine().get(0).getCharacter()); // 11th char wrapped
    }

    @Test
    void testInsertLine_ScrollbackTrigger() {
        for (int i = 0; i < height + 2; i++) {
            buffer.insertLine();
        }
        
        assertEquals(0, buffer.getCursorX());
        assertEquals(height - 1, buffer.getCursorY());
        
        // Moves cursorY initially from 0 to 4 (4 calls), then 3 calls create new lines. Total lines = 5 + 3 = 8
        assertEquals(height + 3, buffer.getAllLines().size());
    }

    @Test
    void testBackspace() {
        buffer.write("Abc");
        buffer.handleBackspace(); // delets 'c' and moves left
        
        assertEquals(2, buffer.getCursorX());
        Line firstLine = buffer.getScreen().get(0);
        assertEquals('A', firstLine.getLine().get(0).getCharacter());
        assertEquals('b', firstLine.getLine().get(1).getCharacter());
        assertEquals(' ', firstLine.getLine().get(2).getCharacter()); // Cleared
    }

    @Test
    void testCursorBounds() {
        // Test Top Bound
        buffer.moveCursor((short) 0); // Up
        assertEquals(0, buffer.getCursorY());
        
        // Test Left Bound
        buffer.moveCursor((short) 2); // Left
        assertEquals(0, buffer.getCursorX());
    }

    @Test
    void testScrolling_ScrollForward() {
        // Fill up to make one scrollback item
        for (int i = 0; i < height; i++) {
            buffer.write("Line" + i);
            if (i < height - 1) buffer.insertLine();
        }
        buffer.insertLine();
        buffer.write("OffScreen");
        
        assertEquals(height - 1, buffer.getCursorY());
        
        // Move to Top
        for (int i = 0; i < height - 1; i++) {
            buffer.moveCursor((short) 0);
        }
        assertEquals(0, buffer.getCursorY());
        
        // One more Up pushes us into history
        buffer.moveCursor((short) 0);
        
        // The bottom line should have moved into scrollForward
        assertEquals(height, buffer.getScreen().size());
        
        // Move back down multiple times until we hit bottom and unroll the history
        for (int i = 0; i < height; i++) {
            buffer.moveCursor((short) 1); 
        }
        assertEquals(height - 1, buffer.getCursorY());
        
        // At this point scrollForward should be empty and screen restored
        assertEquals(height, buffer.getScreen().size());
    }

    @Test
    void testInsertMode() {
        buffer.write("acd");
        buffer.moveCursor((short) 2);
        buffer.moveCursor((short) 2);
        // cursor is now at 'c' (index 1)
        
        buffer.toggleInsertMode();
        assertTrue(buffer.isInsertMode());
        
        buffer.write("b");
        
        Line firstLine = buffer.getScreen().get(0);
        assertEquals('a', firstLine.getLine().get(0).getCharacter());
        assertEquals('b', firstLine.getLine().get(1).getCharacter());
        assertEquals('c', firstLine.getLine().get(2).getCharacter());
        assertEquals('d', firstLine.getLine().get(3).getCharacter());
    }

    @Test
    void testTextAttributes() {
        buffer.setBold(true);
        buffer.setUnderline(true);
        buffer.setItalic(false);
        buffer.write("B");

        Cell cell = buffer.getScreen().get(0).getLine().get(0);
        int style = cell.getStyle();
        assertTrue((style & (1 << 8)) != 0, "Bold bit should be set");
        assertFalse((style & (1 << 9)) != 0, "Italic bit should not be set");
        assertTrue((style & (1 << 10)) != 0, "Underline bit should be set");
    }
}
