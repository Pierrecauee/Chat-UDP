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
 * Interface gráfica Swing para o Chat UDP.
 * Implementa MessageContainer para exibir mensagens recebidas.
 */
public class SwingChatView extends JFrame implements MessageContainer {

    // ── Cores do tema escuro ──────────────────────────────────────────────────
    private static final Color BG_DARK       = new Color(18, 18, 28);
    private static final Color BG_PANEL      = new Color(28, 28, 42);
    private static final Color BG_INPUT      = new Color(38, 38, 55);
    private static final Color ACCENT        = new Color(99, 102, 241);   // índigo
    private static final Color ACCENT_HOVER  = new Color(129, 132, 255);
    private static final Color TEXT_PRIMARY  = new Color(230, 230, 255);
    private static final Color TEXT_MUTED    = new Color(120, 120, 160);
    private static final Color BUBBLE_SENT   = new Color(99, 102, 241);   // índigo
    private static final Color BUBBLE_RECV   = new Color(45, 45, 65);
    private static final Color BORDER_COLOR  = new Color(55, 55, 80);
    private static final Color GREEN_ON      = new Color(72, 199, 116);
    private static final Color RED_OFF       = new Color(220, 80, 80);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_MONO  = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font FONT_LABEL = new Font("SansSerif", Font.BOLD, 11);
    private static final Font FONT_FIELD = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FONT_BTN   = new Font("SansSerif", Font.BOLD, 13);

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
            DateTimeFormatter.ofPattern("HH:mm");

    // ─────────────────────────────────────────────────────────────────────────

    public SwingChatView() {
        super("💬  UDP Chat — IFSULDEMINAS");
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(780, 620);
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── Construção da UI ──────────────────────────────────────────────────────

    private void buildUI() {
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildChatArea(),  BorderLayout.CENTER);
        add(buildInputBar(),  BorderLayout.SOUTH);
    }

    /** Barra superior: título + painel de conexão */
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(12, 16, 12, 16)));

        // Título
        JLabel title = new JLabel("UDP Chat");
        title.setFont(FONT_TITLE);
        title.setForeground(ACCENT_HOVER);

        // Status indicator
        lblStatus = new JLabel("● desconectado");
        lblStatus.setFont(FONT_LABEL);
        lblStatus.setForeground(RED_OFF);

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(title);
        leftGroup.add(lblStatus);

        bar.add(leftGroup,            BorderLayout.WEST);
        bar.add(buildConnectionForm(), BorderLayout.CENTER);

        return bar;
    }

    /** Formulário de conexão inline */
    private JPanel buildConnectionForm() {
        JPanel form = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        form.setOpaque(false);

        fieldUsername   = styledField("Seu nome", 8);
        fieldLocalPort  = styledField("Porta local", 6);
        fieldRemoteIP   = styledField("IP remoto", 10);
        fieldRemotePort = styledField("Porta remota", 6);

        btnConnect = new JButton("Conectar");
        styleButton(btnConnect, ACCENT);

        form.add(label("Usuário:"));   form.add(fieldUsername);
        form.add(label("P.Local:"));   form.add(fieldLocalPort);
        form.add(label("IP:"));        form.add(fieldRemoteIP);
        form.add(label("P.Remota:"));  form.add(fieldRemotePort);
        form.add(btnConnect);

        btnConnect.addActionListener(e -> handleConnect());

        return form;
    }

    /** Área principal de mensagens */
    private JScrollPane buildChatArea() {
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(BG_DARK);
        chatPane.setBorder(new EmptyBorder(12, 16, 12, 16));
        chatDoc = chatPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(chatPane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setBackground(BG_DARK);

        appendSystemMessage("Bem-vindo ao UDP Chat! Preencha os campos acima e clique em Conectar.");

        return scroll;
    }

    /** Barra inferior: campo de mensagem + botão enviar */
    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(10, 16, 10, 16)));

        fieldMessage = new JTextField();
        fieldMessage.setBackground(BG_INPUT);
        fieldMessage.setForeground(TEXT_PRIMARY);
        fieldMessage.setCaretColor(ACCENT_HOVER);
        fieldMessage.setFont(FONT_FIELD);
        fieldMessage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        fieldMessage.setEnabled(false);

        btnSend = new JButton("Enviar ▶");
        styleButton(btnSend, ACCENT);
        btnSend.setEnabled(false);

        // Enviar com Enter
        fieldMessage.addActionListener(e -> handleSend());
        btnSend.addActionListener(e -> handleSend());

        bar.add(fieldMessage, BorderLayout.CENTER);
        bar.add(btnSend,      BorderLayout.EAST);

        return bar;
    }

    // ── Lógica de conexão ─────────────────────────────────────────────────────

    private void handleConnect() {
        if (connected) return;

        String name      = fieldUsername.getText().trim();
        String remoteIP  = fieldRemoteIP.getText().trim();
        String localTxt  = fieldLocalPort.getText().trim();
        String remoteTxt = fieldRemotePort.getText().trim();

        if (name.isEmpty() || remoteIP.isEmpty() || localTxt.isEmpty() || remoteTxt.isEmpty()) {
            showError("Preencha todos os campos antes de conectar.");
            return;
        }

        int localPort, remotePort;
        try {
            localPort  = Integer.parseInt(localTxt);
            remotePort = Integer.parseInt(remoteTxt);
        } catch (NumberFormatException ex) {
            showError("As portas devem ser números inteiros.");
            return;
        }

        username = name;

        try {
            sender = ChatFactory.build(remoteIP, remotePort, localPort, this);
            connected = true;

            // Atualizar UI no EDT
            SwingUtilities.invokeLater(() -> {
                lblStatus.setText("● conectado");
                lblStatus.setForeground(GREEN_ON);
                btnConnect.setText("Conectado");
                btnConnect.setEnabled(false);
                fieldLocalPort.setEnabled(false);
                fieldRemoteIP.setEnabled(false);
                fieldRemotePort.setEnabled(false);
                fieldUsername.setEnabled(false);
                fieldMessage.setEnabled(true);
                btnSend.setEnabled(true);
                fieldMessage.requestFocus();
                appendSystemMessage("Conectado! Envie suas mensagens abaixo.");
            });

        } catch (ChatException ex) {
            showError("Erro ao conectar: " + ex.getCause().getMessage());
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
            showError("Erro ao enviar: " + ex.getMessage());
        }
    }

    // ── MessageContainer (chamado pela thread de recebimento) ─────────────────

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

    // ── Renderização de mensagens no JTextPane ────────────────────────────────

    private void appendSentMessage(String from, String text) {
        appendBubble(from, text, true);
    }

    private void appendReceivedMessage(String from, String text) {
        appendBubble(from, text, false);
    }

    private void appendBubble(String from, String text, boolean sent) {
        try {
            String time = LocalTime.now().format(TIME_FMT);

            // Estilo do remetente
            SimpleAttributeSet nameStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(nameStyle, sent ? ACCENT_HOVER : GREEN_ON);
            StyleConstants.setBold(nameStyle, true);
            StyleConstants.setFontSize(nameStyle, 11);
            StyleConstants.setFontFamily(nameStyle, "SansSerif");

            // Estilo do texto
            SimpleAttributeSet textStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(textStyle, TEXT_PRIMARY);
            StyleConstants.setFontSize(textStyle, 13);
            StyleConstants.setFontFamily(textStyle, "SansSerif");
            StyleConstants.setBackground(textStyle, sent ? new Color(50, 50, 80) : BG_PANEL);

            // Estilo do horário
            SimpleAttributeSet timeStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(timeStyle, TEXT_MUTED);
            StyleConstants.setFontSize(timeStyle, 10);
            StyleConstants.setFontFamily(timeStyle, "SansSerif");

            // Estilo de parágrafo (alinhamento)
            SimpleAttributeSet paraStyle = new SimpleAttributeSet();
            StyleConstants.setAlignment(paraStyle,
                    sent ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            StyleConstants.setSpaceAbove(paraStyle, 6f);
            StyleConstants.setSpaceBelow(paraStyle, 2f);

            int len = chatDoc.getLength();
            chatDoc.setParagraphAttributes(len, 1, paraStyle, false);

            String prefix = sent ? "Você" : from;
            chatDoc.insertString(chatDoc.getLength(), prefix + "  ", nameStyle);
            chatDoc.insertString(chatDoc.getLength(), time + "\n", timeStyle);

            chatDoc.setParagraphAttributes(chatDoc.getLength(), 1, paraStyle, false);
            chatDoc.insertString(chatDoc.getLength(), text + "\n\n", textStyle);

            // Auto-scroll
            chatPane.setCaretPosition(chatDoc.getLength());

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void appendSystemMessage(String text) {
        try {
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, TEXT_MUTED);
            StyleConstants.setItalic(style, true);
            StyleConstants.setFontSize(style, 12);
            StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER);
            StyleConstants.setSpaceAbove(style, 4f);
            StyleConstants.setSpaceBelow(style, 4f);

            int len = chatDoc.getLength();
            chatDoc.setParagraphAttributes(len, 1, style, false);
            chatDoc.insertString(chatDoc.getLength(), "— " + text + " —\n", style);
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────────

    private JTextField styledField(String placeholder, int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(ACCENT_HOVER);
        f.setFont(FONT_FIELD);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(4, 7, 4, 7)));
        f.setToolTipText(placeholder);
        // placeholder visual
        f.setText(placeholder);
        f.setForeground(TEXT_MUTED);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) {
                    f.setText("");
                    f.setForeground(TEXT_PRIMARY);
                }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) {
                    f.setText(placeholder);
                    f.setForeground(TEXT_MUTED);
                }
            }
        });
        return f;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private void styleButton(JButton btn, Color base) {
        btn.setBackground(base);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BTN);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ACCENT_HOVER); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(base); }
        });
    }

    private void showError(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE));
    }

    // ── Main ──────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Usa FlatLaf se disponível, senão tema nativo
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(SwingChatView::new);
    }
}
