import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.swing.*;

public class CalculatorApp extends JFrame {
    private final JTextField display = new JTextField("0");
    private BigDecimal current = BigDecimal.ZERO;
    private BigDecimal stored = null;
    private String pendingOp = null;
    private boolean startNewEntry = true;
    private boolean justEvaluated = false;
    private static final int SCALE = 12;

    public CalculatorApp() {
        super("Calculator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(360, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        display.setEditable(false);
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setFont(display.getFont().deriveFont(28f));
        display.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        display.setBackground(Color.WHITE);

        JPanel keys = buildKeypad();
        setLayout(new BorderLayout(0, 0));
        add(display, BorderLayout.NORTH);
        add(keys, BorderLayout.CENTER);
        installKeyBindings(keys);

        setVisible(true);
    }

    private JPanel buildKeypad() {
        String[][] layout = {
                {"CE", "C", "⌫", "/"},
                {"7", "8", "9", "*"},
                {"4", "5", "6", "-"},
                {"1", "2", "3", "+"},
                {"±", "0", ".", "="},
                {"%", "", "", ""}
        };
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.weightx = 1;
        gbc.weighty = 1;

        for (int r = 0; r < layout.length; r++) {
            for (int c = 0; c < layout[r].length; c++) {
                String label = layout[r][c];
                gbc.gridx = c;
                gbc.gridy = r;
                if (label.isEmpty()) {
                    panel.add(Box.createGlue(), gbc);
                    continue;
                }
                JButton btn = new JButton(label);
                btn.setFont(new Font("SansSerif", Font.BOLD, 20));
                btn.setFocusPainted(false);
                btn.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
                if (isOperator(label) || label.equals("=")) {
                    btn.setBackground(new Color(240, 243, 255));
                } else if (label.equals("C") || label.equals("CE") || label.equals("⌫")) {
                    btn.setBackground(new Color(252, 242, 242));
                } else {
                    btn.setBackground(new Color(246, 246, 246));
                }
                btn.addActionListener(e -> onButton(label));
                panel.add(btn, gbc);
            }
        }
        return panel;
    }

    private boolean isOperator(String s) {
        return "+-*/".contains(s);
    }

    private void onButton(String label) {
        switch (label) {
            case "0": case "1": case "2": case "3": case "4":
            case "5": case "6": case "7": case "8": case "9":
                appendDigit(label);
                break;
            case ".":
                appendDot();
                break;
            case "CE":
                clearEntry();
                break;
            case "C":
                clearAll();
                break;
            case "⌫":
                backspace();
                break;
            case "±":
                toggleSign();
                break;
            case "%":
                percent();
                break;
            case "+": case "-": case "*": case "/":
                setOperator(label);
                break;
            case "=":
                evaluate();
                break;
        }
    }

    private void appendDigit(String d) {
        if (startNewEntry || display.getText().equals("0") || justEvaluated) {
            display.setText(d);
            startNewEntry = false;
            justEvaluated = false;
        } else {
            display.setText(display.getText() + d);
        }
        current = parseDisplay();
    }

    private void appendDot() {
        if (startNewEntry || justEvaluated) {
            display.setText("0.");
            startNewEntry = false;
            justEvaluated = false;
        } else if (!display.getText().contains(".")) {
            display.setText(display.getText() + ".");
        }
        current = parseDisplay();
    }

    private void clearEntry() {
        display.setText("0");
        current = BigDecimal.ZERO;
        startNewEntry = true;
    }

    private void clearAll() {
        current = BigDecimal.ZERO;
        stored = null;
        pendingOp = null;
        startNewEntry = true;
        justEvaluated = false;
        display.setText("0");
    }

    private void backspace() {
        if (startNewEntry || justEvaluated) return;
        String t = display.getText();
        if (t.length() <= 1 || (t.length() == 2 && t.startsWith("-"))) {
            display.setText("0");
            startNewEntry = true;
            current = BigDecimal.ZERO;
        } else {
            display.setText(t.substring(0, t.length() - 1));
            if (display.getText().equals("-")) {
                display.setText("0");
                startNewEntry = true;
            }
            current = parseDisplay();
        }
    }

    private void toggleSign() {
        if (display.getText().equals("0")) return;
        if (display.getText().startsWith("-")) {
            display.setText(display.getText().substring(1));
        } else {
            display.setText("-" + display.getText());
        }
        current = parseDisplay();
    }

    private void percent() {
        BigDecimal result;
        if (stored != null && pendingOp != null) {
            result = stored.multiply(current)
                    .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
        } else {
            result = current.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
        }
        setDisplay(result);
        current = result;
        startNewEntry = true;
    }

    private void setOperator(String op) {
        if (pendingOp != null && !startNewEntry && !justEvaluated) {
            evaluate();
        }
        stored = parseDisplay();   // always capture current as left operand
        pendingOp = op;
        startNewEntry = true;
        justEvaluated = false;
    }

    private void evaluate() {
        if (pendingOp == null || stored == null) {
            setDisplay(current);
            justEvaluated = true;
            return;
        }
        BigDecimal rhs = parseDisplay();
        BigDecimal result;
        try {
            switch (pendingOp) {
                case "+": result = stored.add(rhs); break;
                case "-": result = stored.subtract(rhs); break;
                case "*": result = stored.multiply(rhs); break;
                case "/":
                    if (rhs.compareTo(BigDecimal.ZERO) == 0) {
                        showError("Cannot divide by zero.");
                        return;
                    }
                    result = stored.divide(rhs, SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
                    break;
                default:
                    return;
            }
        } catch (ArithmeticException ex) {
            showError("Math error: " + ex.getMessage());
            return;
        }
        setDisplay(result);
        current = result;
        stored = result;
        startNewEntry = true;
        justEvaluated = true;
    }

    private void setDisplay(BigDecimal value) {
        String s = value.stripTrailingZeros().toPlainString();
        display.setText(s);
    }

    private BigDecimal parseDisplay() {
        try {
            return new BigDecimal(display.getText());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /* -------- Key Bindings -------- */

    private void installKeyBindings(JComponent root) {
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        // digits (main row)
        for (char c = '0'; c <= '9'; c++) bindChar(im, am, c, String.valueOf(c));

        // operators and dot (main row)
        bindChar(im, am, '+', "+");
        bindChar(im, am, '-', "-");
        bindChar(im, am, '*', "*");
        bindChar(im, am, '/', "/");
        bindChar(im, am, '.', ".");

        // Enter/Return (some keyboards map to ACCEPT)
        bind(im, am, "ENTER", "=");
        bind(im, am, "ACCEPT", "=");

        // Backspace/Delete/Escape
        bind(im, am, "BACK_SPACE", "⌫");
        bind(im, am, "DELETE", "CE");
        bind(im, am, "ESCAPE", "C");

        // Numpad digits
        for (int i = 0; i <= 9; i++) {
            final int digit = i; // capture effectively final
            KeyStroke ks = KeyStroke.getKeyStroke("NUMPAD" + digit);
            if (ks != null) {
                String d = String.valueOf(digit);
                im.put(ks, d);
                am.put(d, new AbstractAction() {
                    @Override public void actionPerformed(ActionEvent e) {
                        onButton(d);
                    }
                });
            }
        }

        // Numpad ops
        bind(im, am, "ADD", "+");
        bind(im, am, "SUBTRACT", "-");
        bind(im, am, "MULTIPLY", "*");
        bind(im, am, "DIVIDE", "/");
        bind(im, am, "DECIMAL", ".");

        // Common calculator shortcut for sign toggle
        bind(im, am, "F9", "±");
    }

    private void bindChar(InputMap im, ActionMap am, char ch, String actionLabel) {
        KeyStroke ks = KeyStroke.getKeyStroke(ch);
        if (ks != null) {
            im.put(ks, actionLabel);
            am.put(actionLabel, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    onButton(actionLabel);
                }
            });
        }
    }

    private void bind(InputMap im, ActionMap am, String key, String actionLabel) {
        KeyStroke ks = KeyStroke.getKeyStroke(key);
        if (ks == null) return;
        im.put(ks, actionLabel);
        am.put(actionLabel, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                onButton(actionLabel);
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(CalculatorApp::new);
    }
}
