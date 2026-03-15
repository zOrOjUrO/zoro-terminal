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

    @Test
    void testResizeReflow_ShrinkWidth() {
        // Write long line on 10 char width (14 chars total)
        buffer.write("12345678901234");
        
        // Assert initial layout
        assertEquals(4, buffer.getCursorX());
        assertEquals(1, buffer.getCursorY());
        assertTrue(buffer.getScreen().get(0).isWrapped());
        assertFalse(buffer.getScreen().get(1).isWrapped());
        
        // Resize down to 5 width
        buffer.resize(5, 5);
        assertEquals(5, buffer.getWidth());
        
        // Logical "12345678901234" spread across 5 char chunks ->
        // [0]: "12345" wrapped
        // [1]: "67890" wrapped
        // [2]: "1234 " not wrapped
        // Resize keeps the cursor on the same screen row, so row [0] may move
        // into scrollback while still remaining available via getAllLines().
        assertEquals(4, buffer.getCursorX());
        assertEquals(1, buffer.getCursorY());

        // Visible rows after resize are [1] and [2].
        assertEquals('6', buffer.getScreen().get(0).getLine().get(0).getCharacter());
        assertEquals('0', buffer.getScreen().get(0).getLine().get(4).getCharacter());
        assertEquals('1', buffer.getScreen().get(1).getLine().get(0).getCharacter());
        assertEquals('4', buffer.getScreen().get(1).getLine().get(3).getCharacter());

        // No text is lost: [0] remains in history/all-lines.
        assertEquals('1', buffer.getAllLines().get(0).getLine().get(0).getCharacter());
        assertEquals('5', buffer.getAllLines().get(0).getLine().get(4).getCharacter());
    }

    @Test
    void testResizeReflow_GrowWidth() {
        buffer.write("12345678901234"); // 14 chars on 10-char width
        
        // Resize up to 20 width
        buffer.resize(20, 5);
        assertEquals(20, buffer.getWidth());
        
        // Everything should now fit on one line!
        // [0]: "12345678901234      " not wrapped
        assertEquals(14, buffer.getCursorX());
        assertEquals(0, buffer.getCursorY());
        
        assertFalse(buffer.getScreen().get(0).isWrapped());
        assertEquals('1', buffer.getScreen().get(0).getLine().get(0).getCharacter());
        assertEquals('0', buffer.getScreen().get(0).getLine().get(9).getCharacter());
        assertEquals('4', buffer.getScreen().get(0).getLine().get(13).getCharacter());
        assertEquals(' ', buffer.getScreen().get(0).getLine().get(14).getCharacter());
    }

    @Test
    void testResize_HeightChanges() {
        // Write 8 lines on 5 height buffer (pushes 3 lines to scrollback)
        for (int i = 0; i < 8; i++) {
            buffer.write("L" + i);
            if (i < 7) buffer.insertLine();
        }
        
        assertEquals(height - 1, buffer.getCursorY()); // 4 (bottom of screen)
        assertEquals(8, buffer.getAllLines().size());
        
        // Expand height to 10. Scrollback should pull down.
        buffer.resize(10, 10);
        assertEquals(10, buffer.getHeight());
        
        // All 8 lines should now fit perfectly on the 10-height screen with 0 scrollback
        assertEquals(7, buffer.getCursorY());
        // logical lines created, but getAllLines() returns scrollback + screen + scrollForward
        // Since screen is height 10, getAllLines() size is 10.
        // The first 8 are our input, the last 2 are empty padded lines.
        assertEquals(10, buffer.getAllLines().size()); 
        
        // Shrink height back to 3
        buffer.resize(10, 3);
        assertEquals(3, buffer.getHeight());
        
        // Cursor should cap at bottom of the new smaller screen (height 3 -> bottom is y=2)
        assertEquals(2, buffer.getCursorY());
        // Empty lines at the bottom created when expanding height will be pushed to scrollForward. Total lines is 10.
        assertEquals(10, buffer.getAllLines().size());
    }
}
