package zoro.terminal.controller;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import zoro.terminal.buffer.TerminalBuffer;

public class TerminalKeyHandler extends KeyAdapter {
    private final TerminalBuffer buffer;
    private final JComponent view;

    public TerminalKeyHandler(TerminalBuffer buffer, JComponent view) {
        this.buffer = buffer;
        this.view = view;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                buffer.moveCursor((short) 0);
                break;
            case KeyEvent.VK_DOWN:
                buffer.moveCursor((short) 1);
                break;
            case KeyEvent.VK_LEFT:
                buffer.moveCursor((short) 2);
                break;
            case KeyEvent.VK_RIGHT:
                buffer.moveCursor((short) 3);
                break;
            case KeyEvent.VK_INSERT:
                buffer.toggleInsertMode();
                break;
            case KeyEvent.VK_HOME:
                buffer.clearScreen();
                break;
            case KeyEvent.VK_END:
                buffer.clearAll();
                break;
            case KeyEvent.VK_BACK_SPACE:
                buffer.handleBackspace();
                break;
            case KeyEvent.VK_ENTER:
                buffer.insertLine();
                break;
            case KeyEvent.VK_TAB:
                buffer.write("    ");
                break;
            case KeyEvent.VK_PAGE_UP:
                buffer.pageUp();
                break;
            case KeyEvent.VK_PAGE_DOWN:
                buffer.pageDown();
                break;
            case KeyEvent.VK_DELETE:
                // TODO: implement delete key handling.
                break;
            case KeyEvent.VK_SPACE:
                buffer.write(" ");
                break;
            case KeyEvent.VK_ESCAPE:
                System.exit(0);
                break;
            default:
                break;
        }
        view.repaint(); // UI redraw after key press
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Ignore control characters, let keyPressed handle them
        if (Character.isISOControl(e.getKeyChar())) {
            return;
        }
        buffer.write(String.valueOf(e.getKeyChar()));
        view.repaint(); // UI redraw after a char is typed
    }
}
