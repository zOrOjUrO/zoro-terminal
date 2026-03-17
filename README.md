# Zoro Terminal

A lightweight grid-based terminal emulator implemented in Java Swing.

## Overview

- Core buffer model: scrollback + screen + scroll-forward.
- Deque-based history for O(1) line shift operations.
- Fixed-width line grid for fast indexed access.
- Bit-packed cell attributes (fg, bg, bold, italic, underline).
- Resize reflow using wrapped-line metadata.

## Features

- Bounded scrollback and scroll-forward navigation
- 16-color foreground/background styling
- Bold, italic, underline
- Insert/overwrite mode
- Mouse selection + clipboard copy/paste
- Resize support with reflow

## Quick Start

Prerequisite: Java 21+ (the Gradle toolchain is configured for Java 21).

Windows:

```powershell
.\gradlew.bat run
.\gradlew.bat test
.\gradlew.bat clean build
```

macOS/Linux:

```bash
./gradlew run
./gradlew test
./gradlew clean build
```

## Keymap

| Keys | Action |
| --- | --- |
| Arrow Up | Move cursor up / scroll through history at top |
| Arrow Down | Move cursor down / scroll toward present at bottom |
| Arrow Left | Move cursor left |
| Arrow Right | Move cursor right |
| Insert | Toggle insert mode |
| Home | Clear visible screen |
| End | Clear screen and history |
| Backspace | Delete character before cursor |
| Delete | Delete character at cursor |
| Enter | Insert new line |
| Tab | Insert four spaces |
| F1 | Cycle foreground color (0-15) |
| F2 | Cycle background color (0-15) |
| Ctrl+B | Toggle bold |
| Ctrl+I | Toggle italic |
| Ctrl+U | Toggle underline |
| Ctrl+Shift+C | Copy selected text |
| Ctrl+Shift+V | Paste clipboard text |
| Esc | Exit application |
| Printable keys | Insert typed character |

## Mouse

- Mouse wheel scrolls history (three lines per notch).
- Left press + drag creates or extends selection.

## Limitations

- No PTY backend yet (buffer is model-driven)
- Swing event thread handles both input and rendering
- Wide-character behavior is not implemented yet

## Wide-Character Support Plan

- Add per-cell occupancy state: single, wide-leading, wide-trailing.
- Classify by code point width: combining=0, normal=1, fullwidth=2.
- Write width-2 glyphs atomically across two cells; wrap first if needed.
- Make cursor and delete/backspace glyph-aware (not just column-aware).
- Reflow by glyph clusters so wide glyphs are never split.
- Add tests for boundaries, wrap, delete/backspace, and resize.