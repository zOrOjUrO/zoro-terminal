package zoro.terminal.controller;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import zoro.terminal.buffer.TerminalBuffer;
import zoro.terminal.ui.TerminalView;

public class TerminalMouseHandler implements MouseWheelListener {
    private final TerminalBuffer buffer;
    private final TerminalView view;

    public TerminalMouseHandler(TerminalBuffer buffer, TerminalView view) {
        this.buffer = buffer;
        this.view = view;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // e.getWheelRotation() is negative for scrolling up, positive for down
        int rotation = e.getWheelRotation();
        if (rotation < 0) {
            // Scrolled up
            for (int i = 0; i < -rotation * 3; i++) {
                buffer.scrollUp();
            }
        } else if (rotation > 0) {
            // Scrolled down
            for (int i = 0; i < rotation * 3; i++) {
                buffer.scrollDown();
            }
        }
        
        view.repaint(); // Redraw UI after scroll
    }
}