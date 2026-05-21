package br.edu.ifsuldeminas.sd.chat.client;

import br.edu.ifsuldeminas.sd.chat.ChatException;
import br.edu.ifsuldeminas.sd.chat.ChatFactory;
import br.edu.ifsuldeminas.sd.chat.MessageContainer;
import br.edu.ifsuldeminas.sd.chat.Sender;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Interface gráfica Swing para o Chat UDP — tema hacker/terminal.
 * Implementa MessageContainer para exibir mensagens recebidas.
 */
public class SwingChatView extends JFrame implements MessageContainer {

    // ── Paleta hacker ─────────────────────────────────────────────────────────
    private static final Color BG_BLACK      = new Color(0, 0, 0);
    private static final Color BG_PANEL      = new Color(5, 10, 5);
    private static final Color BG_INPUT      = new Color(0, 15, 0);
    private static final Color GREEN_BRIGHT  = new Color(0, 255, 70);
    private static final Color GREEN_DIM     = new Color(0, 160, 40);
    private static final Color GREEN_DARK    = new Color(0, 80, 20);
    private static final Color GREEN_GHOST   = new Color(0, 50, 10);
    private static final Color AMBER         = new Color(255, 176, 0);
    private static final Color RED_ERR       = new Color(255, 50, 50);
    private static final Color BORDER_GREEN  = new Color(0, 100, 20);

    // ── Fontes mono ───────────────────────────────────────────────────────────
    private static final Font FONT_MONO_LG = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font FONT_MONO_SM = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font FONT_MONO_BD = new Font("Monospaced", Font.BOLD,  13);
    private static final Font FONT_TITLE   = new Font("Monospaced", Font.BOLD,  15);

    // ── Componentes de conexão ────────────────────────────────────────────────
    private JTextField fieldLocalPort;
    private JTextField fieldRemoteIP;
    private JTextField fieldRemotePort;
    private JTextField fieldUsername;
    private JButton    btnConnect;
    private JLabel     lblStatus;

    // ── Componentes de chat ───────────────────────────────────────────────────
    private JTextPane  chatPane;
    private StyledDocument chatDoc;
    private JTextField fieldMessage;
    private JButton    btnSend;

    // ── Estado ────────────────────────────────────────────────────────────────
    private Sender  sender;
    private String  username;
    private boolean connected = false;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // ─────────────────────────────────────────────────────────────────────────

    public SwingChatView() {
        super("[ UDP-CHAT // IFSULDEMINAS // SECURE TERMINAL ]");
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 640);
        setMinimumSize(new Dimension(660, 480));
        setLocationRelativeTo(null);
        // Ícone verde no título
        setVisible(true);
    }

    // ── Construção da UI ──────────────────────────────────────────────────────

    private void buildUI() {
        getContentPane().setBackground(BG_BLACK);
        setLayout(new BorderLayout(0, 0));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildChatArea(),  BorderLayout.CENTER);
        add(buildInputBar(),  BorderLayout.SOUTH);
    }

    /** Barra superior com ASCII art + campos de conexão */
    private JPanel buildTopBar() {
        JPanel outer = new JPanel(new BorderLayout(0, 0));
        outer.setBackground(BG_BLACK);
        outer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GREEN_DARK));

        // ── ASCII header ──
        JLabel ascii = new JLabel(
            "<html><pre style='color:#00ff46;font-family:Monospaced;font-size:11px;'>" +
            " ██╗   ██╗██████╗ ██████╗      ██████╗██╗  ██╗ █████╗ ████████╗\n" +
            " ██║   ██║██╔══██╗██╔══██╗    ██╔════╝██║  ██║██╔══██╗╚══██╔══╝\n" +
            " ██║   ██║██║  ██║██████╔╝    ██║     ███████║███████║   ██║   \n" +
            " ██║   ██║██║  ██║██╔═══╝     ██║     ██╔══██║██╔══██║   ██║   \n" +
            " ╚██████╔╝██████╔╝██║         ╚██████╗██║  ██║██║  ██║   ██║   \n" +
            "  ╚═════╝ ╚═════╝ ╚═╝          ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝  \n" +
            "</pre></html>"
        );
        ascii.setBorder(new EmptyBorder(6, 10, 0, 10));

        // ── Status line ──
        lblStatus = new JLabel("[ STATUS: OFFLINE ]");
        lblStatus.setFont(FONT_MONO_SM);
        lblStatus.setForeground(RED_ERR);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        statusBar.setOpaque(false);
        statusBar.add(lblStatus);

        // ── Formulário de conexão ──
        JPanel form = buildConnectionForm();

        JPanel topContent = new JPanel(new BorderLayout(0, 0));
        topContent.setBackground(BG_BLACK);
        topContent.add(ascii,     BorderLayout.NORTH);
        topContent.add(statusBar, BorderLayout.CENTER);
        topContent.add(form,      BorderLayout.SOUTH);

        outer.add(topContent, BorderLayout.CENTER);
        return outer;
    }

    /** Campos de conexão estilo terminal */
    private JPanel buildConnectionForm() {
        JPanel form = new JPanel(new GridLayout(2, 4, 6, 4));
        form.setBackground(BG_PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, GREEN_DARK),
                new EmptyBorder(8, 12, 8, 12)));

        fieldUsername   = hackerField("IDENTIDADE");
        fieldLocalPort  = hackerField("PORTA_LOCAL");
        fieldRemoteIP   = hackerField("IP_ALVO");
        fieldRemotePort = hackerField("PORTA_ALVO");

        btnConnect = new JButton(">> CONECTAR");
        btnConnect.setBackground(GREEN_DARK);
        btnConnect.setForeground(GREEN_BRIGHT);
        btnConnect.setFont(FONT_MONO_BD);
        btnConnect.setFocusPainted(false);
        btnConnect.setBorderPainted(true);
        btnConnect.setBorder(BorderFactory.createLineBorder(GREEN_DIM, 1));
        btnConnect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConnect.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnConnect.setBackground(new Color(0,120,30)); }
            public void mouseExited(MouseEvent e)  { btnConnect.setBackground(GREEN_DARK); }
        });
        btnConnect.addActionListener(e -> handleConnect());

        form.add(hackerLabel("> IDENTIDADE:")); form.add(fieldUsername);
        form.add(hackerLabel("> PORTA_LOCAL:")); form.add(fieldLocalPort);
        form.add(hackerLabel("> IP_ALVO:"));    form.add(fieldRemoteIP);
        form.add(hackerLabel("> PORTA_ALVO:")); form.add(fieldRemotePort);

        JPanel wrapper = new JPanel(new BorderLayout(6, 0));
        wrapper.setBackground(BG_PANEL);
        wrapper.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBackground(BG_PANEL);
        btnPanel.setBorder(new EmptyBorder(8, 0, 8, 12));
        btnPanel.add(btnConnect, BorderLayout.CENTER);
        wrapper.add(btnPanel, BorderLayout.EAST);

        return wrapper;
    }

    /** Área de chat estilo terminal com scrollbar verde */
    private JScrollPane buildChatArea() {
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(BG_BLACK);
        chatPane.setForeground(GREEN_BRIGHT);
        chatPane.setFont(FONT_MONO_LG);
        chatPane.setBorder(new EmptyBorder(10, 14, 10, 14));
        chatDoc = chatPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(chatPane);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GREEN_DARK));
        scroll.setBackground(BG_BLACK);
        scroll.getViewport().setBackground(BG_BLACK);

        // Scrollbar estilizada
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected void configureScrollBarColors() {
                thumbColor    = GREEN_DARK;
                trackColor    = BG_BLACK;
            }
            protected JButton createDecreaseButton(int o) { return zeroButton(); }
            protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });

        appendSystemMessage("SISTEMA INICIALIZADO. AGUARDANDO CONEXÃO...");
        appendSystemMessage("Preencha os campos acima e clique em >> CONECTAR");

        return scroll;
    }

    /** Barra de input estilo prompt de terminal */
    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(6, 0));
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, GREEN_DARK),
                new EmptyBorder(8, 12, 8, 12)));

        JLabel prompt = new JLabel("root@udpchat:~$ ");
        prompt.setFont(FONT_MONO_BD);
        prompt.setForeground(GREEN_BRIGHT);

        fieldMessage = new JTextField();
        fieldMessage.setBackground(BG_INPUT);
        fieldMessage.setForeground(GREEN_BRIGHT);
        fieldMessage.setCaretColor(GREEN_BRIGHT);
        fieldMessage.setFont(FONT_MONO_LG);
        fieldMessage.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GREEN_DIM));
        fieldMessage.setEnabled(false);
        fieldMessage.addActionListener(e -> handleSend());

        btnSend = new JButton("[ENTER]");
        btnSend.setBackground(BG_BLACK);
        btnSend.setForeground(GREEN_DIM);
        btnSend.setFont(FONT_MONO_SM);
        btnSend.setFocusPainted(false);
        btnSend.setBorder(BorderFactory.createLineBorder(GREEN_DARK, 1));
        btnSend.setEnabled(false);
        btnSend.addActionListener(e -> handleSend());
        btnSend.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (btnSend.isEnabled()) btnSend.setForeground(GREEN_BRIGHT); }
            public void mouseExited(MouseEvent e)  { btnSend.setForeground(GREEN_DIM); }
        });

        bar.add(prompt,       BorderLayout.WEST);
        bar.add(fieldMessage, BorderLayout.CENTER);
        bar.add(btnSend,      BorderLayout.EAST);

        return bar;
    }

    // ── Lógica de conexão ─────────────────────────────────────────────────────

    private void handleConnect() {
        if (connected) return;

        String name      = getFieldValue(fieldUsername,   "IDENTIDADE");
        String remoteIP  = getFieldValue(fieldRemoteIP,   "IP_ALVO");
        String localTxt  = getFieldValue(fieldLocalPort,  "PORTA_LOCAL");
        String remoteTxt = getFieldValue(fieldRemotePort, "PORTA_ALVO");

        if (name.isEmpty() || remoteIP.isEmpty() || localTxt.isEmpty() || remoteTxt.isEmpty()) {
            appendErrorMessage("ERRO: Todos os campos devem ser preenchidos.");
            return;
        }

        int localPort, remotePort;
        try {
            localPort  = Integer.parseInt(localTxt);
            remotePort = Integer.parseInt(remoteTxt);
        } catch (NumberFormatException ex) {
            appendErrorMessage("ERRO: As portas precisam ser números inteiros.");
            return;
        }

        username = name;
        appendSystemMessage("Iniciando conexão com " + remoteIP + ":" + remoteTxt + "...");

        try {
            sender = ChatFactory.build(remoteIP, remotePort, localPort, this);
            connected = true;

            SwingUtilities.invokeLater(() -> {
                lblStatus.setText("[ STATUS: ONLINE // USER: " + username.toUpperCase() + " ]");
                lblStatus.setForeground(GREEN_BRIGHT);
                btnConnect.setText(">> CONECTADO");
                btnConnect.setEnabled(false);
                fieldLocalPort.setEnabled(false);
                fieldRemoteIP.setEnabled(false);
                fieldRemotePort.setEnabled(false);
                fieldUsername.setEnabled(false);
                fieldMessage.setEnabled(true);
                btnSend.setEnabled(true);
                fieldMessage.requestFocus();
                appendSystemMessage("CONEXÃO ESTABELECIDA. CANAL SEGURO ATIVO.");
            });

        } catch (ChatException ex) {
            appendErrorMessage("FALHA NA CONEXÃO: " + ex.getCause().getMessage());
        }
    }

    // ── Lógica de envio ───────────────────────────────────────────────────────

    private void handleSend() {
        if (!connected || sender == null) return;
        String text = fieldMessage.getText().trim();
        if (text.isEmpty()) return;

        String fullMessage = text + MessageContainer.FROM + username;
        try {
            sender.send(fullMessage);
            appendSentMessage(username, text);
            fieldMessage.setText("");
        } catch (ChatException ex) {
            appendErrorMessage("FALHA NO ENVIO: " + ex.getMessage());
        }
    }

    // ── MessageContainer ──────────────────────────────────────────────────────

    @Override
    public void newMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(MessageContainer.FROM, 2);
            if (parts.length == 2) {
                String text   = parts[0].trim();
                String sender = parts[1].trim();
                if (!text.isEmpty())
                    appendReceivedMessage(sender, text);
            }
        });
    }

    // ── Renderização de mensagens ─────────────────────────────────────────────

    private void appendSentMessage(String from, String text) {
        try {
            String time = LocalTime.now().format(TIME_FMT);
            String line = "[" + time + "] <" + from.toUpperCase() + "> " + text;

            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, GREEN_BRIGHT);
            StyleConstants.setFontFamily(style, "Monospaced");
            StyleConstants.setFontSize(style, 13);

            chatDoc.insertString(chatDoc.getLength(), line + "\n", style);
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException e) { e.printStackTrace(); }
    }

    private void appendReceivedMessage(String from, String text) {
        try {
            String time = LocalTime.now().format(TIME_FMT);
            String line = "[" + time + "] <" + from.toUpperCase() + "> " + text;

            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, AMBER);
            StyleConstants.setFontFamily(style, "Monospaced");
            StyleConstants.setFontSize(style, 13);

            chatDoc.insertString(chatDoc.getLength(), line + "\n", style);
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException e) { e.printStackTrace(); }
    }

    private void appendSystemMessage(String text) {
        try {
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, GREEN_DARK);
            StyleConstants.setFontFamily(style, "Monospaced");
            StyleConstants.setFontSize(style, 12);

            chatDoc.insertString(chatDoc.getLength(), "// " + text + "\n", style);
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException e) { e.printStackTrace(); }
    }

    private void appendErrorMessage(String text) {
        try {
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, RED_ERR);
            StyleConstants.setFontFamily(style, "Monospaced");
            StyleConstants.setFontSize(style, 12);
            StyleConstants.setBold(style, true);

            chatDoc.insertString(chatDoc.getLength(), "[!] " + text + "\n", style);
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException e) { e.printStackTrace(); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JTextField hackerField(String placeholder) {
        JTextField f = new JTextField(10);
        f.setBackground(BG_INPUT);
        f.setForeground(GREEN_BRIGHT);
        f.setCaretColor(GREEN_BRIGHT);
        f.setFont(FONT_MONO_LG);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN_DARK, 1),
                new EmptyBorder(3, 6, 3, 6)));
        f.setText(placeholder);
        f.setForeground(GREEN_DARK);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) {
                    f.setText("");
                    f.setForeground(GREEN_BRIGHT);
                }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) {
                    f.setText(placeholder);
                    f.setForeground(GREEN_DARK);
                }
            }
        });
        return f;
    }

    private JLabel hackerLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_MONO_SM);
        l.setForeground(GREEN_DIM);
        return l;
    }

    /** Retorna o valor do campo ignorando o placeholder */
    private String getFieldValue(JTextField field, String placeholder) {
        String val = field.getText().trim();
        return val.equals(placeholder) ? "" : val;
    }

    // ── Main ──────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(SwingChatView::new);
    }
}
