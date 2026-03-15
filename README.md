# Zoro Terminal
A lightweight grid-based terminal emulator built with Java Swing.

## Architecture 
- **MVC split:** `TerminalBuffer` is the headless state engine. `TerminalView` only renders. Input is routed through dedicated handlers.
- **Compact cells:** `Cell` packs foreground, background, and style bits into a 16-bit `short` to reduce per-cell memory overhead.
- **Reflow-aware lines:** `Line.isWrapped` differentiates soft-wrap continuation from logical line breaks, enabling resize reflow without dropping content.

## Features
- Scrollback + scroll-forward history using `ArrayDeque`.
- Styled text (bold, italic, underline) and 16-color foreground/background cycling.
- Mouse selection with clipboard copy/paste support.
- Insert/overwrite mode toggle.
- Width/height resize support with cursor-preserving reflow.

## Quick Start
Prerequisite: Java 21+ installed.

Windows:

	.\gradlew.bat run

	.\gradlew.bat test

macOS/Linux:

	./gradlew run

	./gradlew test

Full build:

	.\gradlew.bat clean build

## Keymap

| Keys | Action |
| --- | --- |
| `Arrow Up` | Move cursor up / scroll through history at top |
| `Arrow Down` | Move cursor down / scroll toward present at bottom |
| `Arrow Left` | Move cursor left |
| `Arrow Right` | Move cursor right |
| `Insert` | Toggle insert mode |
| `Home` | Clear visible screen |
| `End` | Clear all (screen + history) |
| `Backspace` | Delete character before cursor |
| `Delete` | Delete character at cursor |
| `Enter` | Insert new line |
| `Tab` | Insert 4 spaces |
| `F1` | Cycle foreground color (0-15) |
| `F2` | Cycle background color (0-15) |
| `Ctrl+B` | Toggle bold |
| `Ctrl+I` | Toggle italic |
| `Ctrl+U` | Toggle underline |
| `Ctrl+Shift+C` | Copy selected text to clipboard |
| `Ctrl+Shift+V` | Paste clipboard text |
| `Esc` | Exit application |
| Printable keys | Insert typed character |

## Mouse
- Mouse wheel: scroll history (3 lines per wheel notch).
- Left press + drag: create/extend selection.

## Shortcomings
- Rendering and input handling are both on the Swing event thread; bursts of input can affect responsiveness.
- No PTY backend yet; input currently writes into an internal buffer model rather than a real shell process.
- Font metrics are calculated from Swing at runtime but not centrally cached/tuned for advanced typography scenarios.