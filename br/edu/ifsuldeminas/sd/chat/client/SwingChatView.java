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

public class SwingChatView extends JFrame implements MessageContainer {

    private static final Color BG_BLACK     = new Color(0, 0, 0);
    private static final Color BG_PANEL     = new Color(0, 8, 0);
    private static final Color BG_INPUT     = new Color(0, 15, 0);
    private static final Color GREEN_BRIGHT = new Color(0, 255, 70);
    private static final Color GREEN_MED    = new Color(0, 180, 40);
    private static final Color GREEN_DIM    = new Color(0, 110, 25);
    private static final Color GREEN_DARK   = new Color(0, 55, 10);
    private static final Color GREEN_LIME   = new Color(180, 255, 100);
    private static final Color RED_ERR      = new Color(255, 60, 60);

    private static final Font FONT_MONO_LG = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font FONT_MONO_SM = new Font("Monospaced", Font.PLAIN, 11);
    private static final Font FONT_MONO_BD = new Font("Monospaced", Font.BOLD,  13);

    private JTextField     fieldLocalPort, fieldRemoteIP, fieldRemotePort, fieldUsername;
    private JButton        btnConnect;
    private JLabel         lblStatus;
    private JTextPane      chatPane;
    private StyledDocument chatDoc;
    private JTextField     fieldMessage;
    private JButton        btnSend;

    private Sender  sender;
    private String  username;
    private boolean connected = false;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public SwingChatView() {
        super("[ UDP-CHAT // IFSULDEMINAS ]");
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 640);
        setMinimumSize(new Dimension(660, 480));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUI() {
        getContentPane().setBackground(BG_BLACK);
        setLayout(new BorderLayout(0, 0));
        add(buildTopBar(),   BorderLayout.NORTH);
        add(buildChatArea(), BorderLayout.CENTER);
        add(buildInputBar(), BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_BLACK);
        outer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GREEN_DARK));

        JLabel ascii = new JLabel(
            "<html><pre style='color:#00ff46; background:#000000; font-family:Monospaced; font-size:11px; margin:0;'>" +
            " ██╗   ██╗██████╗ ██████╗      ██████╗██╗  ██╗ █████╗ ████████╗\n" +
            " ██║   ██║██╔══██╗██╔══██╗    ██╔════╝██║  ██║██╔══██╗╚══██╔══╝\n" +
            " ██║   ██║██║  ██║██████╔╝    ██║     ███████║███████║   ██║   \n" +
            " ██║   ██║██║  ██║██╔═══╝     ██║     ██╔══██║██╔══██║   ██║   \n" +
            " ╚██████╔╝██████╔╝██║         ╚██████╗██║  ██║██║  ██║   ██║   \n" +
            "  ╚═════╝ ╚═════╝ ╚═╝          ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝  \n" +
            "</pre></html>"
        );
        ascii.setBackground(BG_BLACK);
        ascii.setOpaque(true);
        ascii.setBorder(new EmptyBorder(6, 10, 0, 10));

        lblStatus = new JLabel("[ STATUS: OFFLINE ]");
        lblStatus.setFont(FONT_MONO_SM);
        lblStatus.setForeground(RED_ERR);
        lblStatus.setBackground(BG_BLACK);
        lblStatus.setOpaque(true);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        statusBar.setBackground(BG_BLACK);
        statusBar.add(lblStatus);

        JPanel form = buildConnectionForm();

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_BLACK);
        top.add(ascii,     BorderLayout.NORTH);
        top.add(statusBar, BorderLayout.CENTER);
        top.add(form,      BorderLayout.SOUTH);

        outer.add(top, BorderLayout.CENTER);
        return outer;
    }

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

        form.add(hackerLabel("> IDENTIDADE:"));  form.add(fieldUsername);
        form.add(hackerLabel("> PORTA_LOCAL:")); form.add(fieldLocalPort);
        form.add(hackerLabel("> IP_ALVO:"));     form.add(fieldRemoteIP);
        form.add(hackerLabel("> PORTA_ALVO:")); form.add(fieldRemotePort);

        btnConnect = new JButton(">> CONECTAR");
        btnConnect.setBackground(BG_BLACK);
        btnConnect.setForeground(GREEN_BRIGHT);
        btnConnect.setFont(FONT_MONO_BD);
        btnConnect.setFocusPainted(false);
        btnConnect.setBorder(BorderFactory.createLineBorder(GREEN_MED, 1));
        btnConnect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConnect.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnConnect.setBackground(GREEN_DARK); }
            public void mouseExited(MouseEvent e)  { btnConnect.setBackground(BG_BLACK); }
        });
        btnConnect.addActionListener(e -> handleConnect());

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setBackground(BG_PANEL);
        btnPanel.setBorder(new EmptyBorder(8, 6, 8, 0));
        btnPanel.add(btnConnect, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout(6, 0));
        wrapper.setBackground(BG_PANEL);
        wrapper.add(form,     BorderLayout.CENTER);
        wrapper.add(btnPanel, BorderLayout.EAST);

        return wrapper;
    }

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
        scroll.getVerticalScrollBar().setBackground(BG_BLACK);
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected void configureScrollBarColors() {
                thumbColor = GREEN_DARK;
                trackColor = BG_BLACK;
            }
            protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });

        appendSystemMessage("SISTEMA INICIALIZADO. AGUARDANDO CONEXAO...");
        appendSystemMessage("Preencha os campos acima e clique em >> CONECTAR");

        return scroll;
    }

    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(6, 0));
        bar.setBackground(BG_BLACK);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, GREEN_DARK),
                new EmptyBorder(8, 12, 8, 12)));

        JLabel prompt = new JLabel("root@udpchat:~$ ");
        prompt.setFont(FONT_MONO_BD);
        prompt.setForeground(GREEN_BRIGHT);
        prompt.setBackground(BG_BLACK);
        prompt.setOpaque(true);

        fieldMessage = new JTextField();
        fieldMessage.setBackground(BG_BLACK);
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

    private void handleConnect() {
        if (connected) return;

        String name      = getVal(fieldUsername,   "IDENTIDADE");
        String remoteIP  = getVal(fieldRemoteIP,   "IP_ALVO");
        String localTxt  = getVal(fieldLocalPort,  "PORTA_LOCAL");
        String remoteTxt = getVal(fieldRemotePort, "PORTA_ALVO");

        if (name.isEmpty() || remoteIP.isEmpty() || localTxt.isEmpty() || remoteTxt.isEmpty()) {
            appendErrorMessage("ERRO: Todos os campos devem ser preenchidos.");
            return;
        }

        int localPort, remotePort;
        try {
            localPort  = Integer.parseInt(localTxt);
            remotePort = Integer.parseInt(remoteTxt);
        } catch (NumberFormatException ex) {
            appendErrorMessage("ERRO: As portas precisam ser numeros inteiros.");
            return;
        }

        username = name;
        appendSystemMessage("Iniciando conexao com " + remoteIP + ":" + remoteTxt + "...");

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
                appendSystemMessage("CONEXAO ESTABELECIDA. CANAL ATIVO.");
            });
        } catch (ChatException ex) {
            appendErrorMessage("FALHA NA CONEXAO: " + ex.getCause().getMessage());
        }
    }

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

    @Override
    public void newMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        SwingUtilities.invokeLater(() -> {
            String[] parts = message.split(MessageContainer.FROM, 2);
            if (parts.length == 2) {
                String text = parts[0].trim();
                String from = parts[1].trim();
                if (!text.isEmpty()) appendReceivedMessage(from, text);
            }
        });
    }

    private void appendSentMessage(String from, String text) {
        try {
            SimpleAttributeSet s = new SimpleAttributeSet();
            StyleConstants.setForeground(s, GREEN_BRIGHT);
            StyleConstants.setFontFamily(s, "Monospaced");
            StyleConstants.setFontSize(s, 13);
            String line = "[" + LocalTime.now().format(TIME_FMT) + "] <" + from.toUpperCase() + "> " + text;
            chatDoc.insertString(chatDoc.getLength(), line + "\n", s);
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException e) { e.printStackTrace(); }
    }

    private void appendReceivedMessage(String from, String text) {
        try {
            SimpleAttributeSet s = new SimpleAttributeSet();
            StyleConstants.setForeground(s, GREEN_LIME);
            StyleConstants.setFontFamily(s, "Monospaced");
            StyleConstants.setFontSize(s, 13);
            String line = "[" + LocalTime.now().format(TIME_FMT) + "] <" + from.toUpperCase() + "> " + text;
            chatDoc.insertString(chatDoc.getLength(), line + "\n", s);
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException e) { e.printStackTrace(); }
    }

    private void appendSystemMessage(String text) {
        try {
            SimpleAttributeSet s = new SimpleAttributeSet();
            StyleConstants.setForeground(s, GREEN_DIM);
            StyleConstants.setFontFamily(s, "Monospaced");
            StyleConstants.setFontSize(s, 12);
            chatDoc.insertString(chatDoc.getLength(), "// " + text + "\n", s);
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException e) { e.printStackTrace(); }
    }

    private void appendErrorMessage(String text) {
        try {
            SimpleAttributeSet s = new SimpleAttributeSet();
            StyleConstants.setForeground(s, RED_ERR);
            StyleConstants.setBold(s, true);
            StyleConstants.setFontFamily(s, "Monospaced");
            StyleConstants.setFontSize(s, 12);
            chatDoc.insertString(chatDoc.getLength(), "[!] " + text + "\n", s);
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException e) { e.printStackTrace(); }
    }

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
                if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(GREEN_BRIGHT); }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(GREEN_DARK); }
            }
        });
        return f;
    }

    private JLabel hackerLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_MONO_SM);
        l.setForeground(GREEN_MED);
        l.setBackground(BG_PANEL);
        l.setOpaque(true);
        return l;
    }

    private String getVal(JTextField f, String placeholder) {
        String v = f.getText().trim();
        return v.equals(placeholder) ? "" : v;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(SwingChatView::new);
    }
}
