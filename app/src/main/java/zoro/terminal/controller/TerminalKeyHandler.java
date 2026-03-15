package zoro.terminal.controller;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.OutputStream;
import java.io.PrintStream;

import zoro.terminal.ui.TerminalView;
import zoro.terminal.buffer.TerminalBuffer;

public class TerminalKeyHandler extends KeyAdapter {
    private final TerminalBuffer buffer;
    private final TerminalView view;

    public TerminalKeyHandler(TerminalBuffer buffer, TerminalView view) {
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
                this.buffer.toggleInsertMode();
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
            case KeyEvent.VK_F1:
                buffer.setCurrentFg((buffer.getCurrentFg() + 1) % 16);
                break;
            case KeyEvent.VK_F2:
                buffer.setCurrentBg((buffer.getCurrentBg() + 1) % 16);
                break;
            case KeyEvent.VK_B:
                if (e.isControlDown()) buffer.setBold(!buffer.isBold());
                break;
            case KeyEvent.VK_I:
                if (e.isControlDown()) buffer.setItalic(!buffer.isItalic());
                break;
            case KeyEvent.VK_U:
                if (e.isControlDown()) buffer.setUnderline(!buffer.isUnderline());
                break;
            case KeyEvent.VK_V:
                if (e.isControlDown() && e.isShiftDown()) handlePaste();
                break;
            case KeyEvent.VK_C:
                if (e.isControlDown() && e.isShiftDown()) handleCopy();
                break;
            case KeyEvent.VK_DELETE:
                buffer.handleDelete();
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
        if (Character.isISOControl(e.getKeyChar()) || e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
            return;
        }
        buffer.clearSelection();
        buffer.write(String.valueOf(e.getKeyChar()));
        view.repaint(); // UI redraw after a char is typed
    }

    private void handleCopy() {
        if (buffer.hasSelection()) {
            String selected = buffer.getSelectedText();
            StringSelection selection = new StringSelection(selected);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            buffer.clearSelection();
            view.repaint();
        }
    }

    private void handlePaste() {
        PrintStream oldErr = System.err;
        try {
            // Suppressing the ClassNotFoundException printed internally by Java AWT 
            System.setErr(new PrintStream(new OutputStream() {
                public void write(int b) {}
            }));

            String text = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (text != null) {
                // Handle newlines in pasted text by splitting and inserting lines
                String[] lines = text.split("\r\n|\n|\r");
                for (int i = 0; i < lines.length; i++) {
                    buffer.write(lines[i]);
                    if (i < lines.length - 1) {
                        buffer.insertLine();
                    }
                }
            }
        } catch (Exception ex) {
            // Ignore paste errors
        } finally {
            System.setErr(oldErr);
        }
    }
}
