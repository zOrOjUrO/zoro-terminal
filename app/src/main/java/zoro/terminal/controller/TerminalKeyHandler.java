public class TerminalKeyHandler extends KeyAdapter {
    private final TerminalBuffer buffer;

    public TerminalKeyHandler(TerminalBuffer buffer) {
        this.buffer = buffer;
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
            case KeyEvent.VK_DELETE:
                // TODO: implement delete key handling.
                break;
            case KeyEvent.VK_SPACE:
                buffer.write(" ");
                break;
            case KeyEvent.VK_ESCAPE:
                // TODO: implement escape key handling.
                break;
            default:
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Handled in keyPressed for better control over special keys.
        buffer.write(String.valueOf(e.getKeyChar()));
    }
}
