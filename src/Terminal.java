import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

public final class Terminal {

    public Terminal() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception ignore) {
        }

        final JFrame frame = new JFrame("term");

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(screenSize.width / 2, screenSize.height / 2);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        final JTextPane screen = new JTextPane();
        screen.setFont(new Font("Monospaced", Font.PLAIN, 20));
        screen.setEditable(false);
        screen.setBackground(new Color(0, 0, 0));
        screen.setForeground(new Color(240, 240, 240));
        screen.setCaretColor(new Color(200, 200, 200));
        screen.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent evt) {
                 screen.getCaret().setVisible(true);
            }
        });
        screen.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent evt) {
                if ((evt.getKeyCode() == KeyEvent.VK_V) && ((evt.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK ) != 0)) {
                    pasteFromClipboard(screen);
                }

                switch (evt.getKeyCode()) {
                    case KeyEvent.VK_ENTER: {
                        insertString(screen, "\n", true);
                        final String[] lines = screen.getText().split("\n");
                        final int nLines = lines.length;
                        final String currentLine = lines[nLines - 1];
                        if (!currentLine.strip().isEmpty()) {
                            if (out != null) {
                                try {
                                    final String[] commandArr = currentLine.split(">");
                                    if (commandArr != null && commandArr.length > 1) {
                                        final String command = currentLine.split(">")[1];
                                        out.write(command.getBytes());
                                        out.write("\r\n".getBytes());
                                        out.flush();
                                    } else {
                                        out.write(currentLine.getBytes());
                                        out.write("\r\n".getBytes());
                                        out.flush();
                                    }
                                } catch (final Exception ex) {
                                    ex.printStackTrace(System.err); // TODO(nschultz): Temporary
                                }
                            }
                        }
                    } break;

                    case KeyEvent.VK_BACK_SPACE: {
                        if (!fetchCurrentChar(screen).equals(">")) {
                            deleteString(screen, screen.getDocument().getLength() - 1, 1);
                        }
                    } break;

                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_RIGHT: {
                        evt.consume();
                    } break;

                    default: {
                        final char c = evt.getKeyChar();
                        if (c >= 32 && c <= 126) {
                            insertString(screen, String.valueOf(evt.getKeyChar()), true);
                        }
                        screen.setCaretPosition(screen.getDocument().getLength());
                    } break;
                }
            }
        });
        frame.add(new JScrollPane(screen));
        frame.setIconImage(new ImageIcon("res\\icon.png").getImage());

        frame.setVisible(true);

        runCmd(screen);
    }

    private OutputStream out = null;

    private void runCmd(final JTextPane screen) {
        assert screen != null;

        final Thread thread = new Thread(() -> {
            try {
                final ProcessBuilder builder = new ProcessBuilder("cmd");
                builder.redirectErrorStream(true);

                final Process process = builder.start();
                final InputStream in = process.getInputStream();
                out = process.getOutputStream();
                while (true) {
                    final byte[] buffer = new byte[4096];
                    final int readBytes = in.read(buffer);
                    insertString(screen, new String(buffer, 0, readBytes), true);
                }
            } catch (final Exception ex) {
                ex.printStackTrace(System.err); // TODO(nschultz): Temporary
            }
        });
        thread.setName("cmd_thread");
        thread.setDaemon(true);
        thread.start();
    }

    private static String fetchCurrentChar(final JTextPane pane) {
        assert EventQueue.isDispatchThread();
        assert pane != null;

        try {
            return pane.getDocument().getText(pane.getDocument().getLength() - 1, 1);
        } catch (final BadLocationException ex) {
            assert false;
        }
        return null;
    }

    private static void deleteString(final JTextPane pane, final int offs, final int n) {
        assert EventQueue.isDispatchThread();
        assert pane != null;

        try {
            pane.getDocument().remove(offs, n);
        } catch (final BadLocationException ex) {
            assert false;
        }
    }

    private static void insertString(final JTextPane pane, final String str, final boolean block) {
        assert pane != null;
        assert str  != null;

        if (!EventQueue.isDispatchThread()) {
            if (block) {
                try {
                    EventQueue.invokeAndWait(() -> {
                        try {
                            final Document doc = pane.getDocument();
                            doc.insertString(doc.getLength(), str, null);
                        } catch(final BadLocationException ex) {
                            assert false;
                        }
                    });
                } catch (final InvocationTargetException | InterruptedException ex) {
                    assert false;
                }
            } else {
                EventQueue.invokeLater(() -> {
                    try {
                        final Document doc = pane.getDocument();
                        doc.insertString(doc.getLength(), str, null);
                    } catch(final BadLocationException ex) {
                        assert false;
                    }
                });
            }
        } else {
            try {
                final Document doc = pane.getDocument();
                doc.insertString(doc.getLength(), str, null);
            } catch(final BadLocationException ex) {
                assert false;
            }
        }
    }

    private void pasteFromClipboard(final JTextPane pane) {
        assert pane != null;

        try {
            final Clipboard c    = Toolkit.getDefaultToolkit().getSystemClipboard();
            final Transferable t = c.getContents(this);
            final String content = (String) t.getTransferData(DataFlavor.stringFlavor);

            insertString(pane, content, true);
        } catch (final Exception ex) {
            ex.printStackTrace(System.err); // TODO(nschultz): Temporary
        }
    }
}
