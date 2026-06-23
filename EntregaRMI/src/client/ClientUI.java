package client;

import common.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;

public class ClientUI extends JFrame {

    private Usuario me;
    private IWhatsUTServer server;
    private WhatsUTClientImpl callbackImpl;

    private DefaultListModel<Usuario> usersModel;
    private JList<Usuario> listUsers;

    private DefaultListModel<Grupo> groupsModel;
    private JList<Grupo> listGroups;

    private JPanel chatPanel;
    private JScrollPane scrollChat;
    private JTextField txtInput;
    private JLabel lblCurrentChat;
    private JButton btnJoinGroupHeader;
    private JButton btnLeaveGroupHeader;

    // Historico de conversas (Chave: nome do usuario ou grupo)
    private java.util.Map<String, java.util.List<Mensagem>> historicoMensagens = new java.util.HashMap<>();
    private java.util.Map<ArquivoInfo, File> arquivosSalvos = new java.util.HashMap<>();

    // WhatsApp Light Theme Colors
    private Color bgDark = new Color(240, 242, 245);
    private Color bgList = new Color(255, 255, 255);
    private Color bgChat = new Color(239, 234, 226);
    private Color colorSent = new Color(217, 253, 211); // Light green
    private Color colorReceived = new Color(255, 255, 255); // White
    private Color textColor = new Color(17, 27, 33);
    private Color headerBg = new Color(240, 242, 245);
    private Color dividerColor = new Color(233, 237, 239);

    public ClientUI(Usuario me, IWhatsUTServer server, WhatsUTClientImpl callbackImpl) {
        this.me = me;
        this.server = server;
        this.callbackImpl = callbackImpl;

        setTitle("WhatsApp Web (Swing) - " + me.getUsername());
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(bgDark);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                fazerLogout();
            }
        });

        initComponents();
        
        try {
            atualizarUsuarios(server.getUsuariosLogados());
            atualizarGrupos(server.getGrupos());
        } catch (Exception e) {
            exibirNotificacao("Erro ao buscar listas");
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Left Panel
        JPanel pnlLeft = new JPanel(new BorderLayout());
        pnlLeft.setPreferredSize(new Dimension(320, 0));
        pnlLeft.setBackground(bgList);
        pnlLeft.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, dividerColor));

        // Header Left Panel
        JPanel pnlLeftHeader = new JPanel();
        pnlLeftHeader.setLayout(new BoxLayout(pnlLeftHeader, BoxLayout.Y_AXIS));
        pnlLeftHeader.setBackground(headerBg);
        pnlLeftHeader.setPreferredSize(new Dimension(0, 95));
        pnlLeftHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, dividerColor));
        
        // User Info Row
        JPanel pnlUserRow = new JPanel(new BorderLayout());
        pnlUserRow.setOpaque(false);
        pnlUserRow.setBorder(new EmptyBorder(10, 15, 5, 15));
        
        JLabel lblMe = new JLabel(me.getUsername());
        lblMe.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblMe.setForeground(textColor);
        lblMe.setIcon(createAvatarIcon(me.getUsername().substring(0,1).toUpperCase(), 36));
        pnlUserRow.add(lblMe, BorderLayout.WEST);
        
        // Group Actions Row
        JPanel pnlGroupRow = new JPanel(new BorderLayout());
        pnlGroupRow.setOpaque(false);
        pnlGroupRow.setBorder(new EmptyBorder(5, 15, 10, 15));
        
        JButton btnAddGroup = createGroupButton("Criar Grupo");
        btnAddGroup.setToolTipText("Criar Grupo");
        btnAddGroup.addActionListener(e -> criarGrupo());
        
        pnlGroupRow.add(btnAddGroup, BorderLayout.CENTER);
        
        pnlLeftHeader.add(pnlUserRow);
        pnlLeftHeader.add(pnlGroupRow);

        pnlLeft.add(pnlLeftHeader, BorderLayout.NORTH);

        // Lists (Users Top, Groups Bottom)
        JPanel pnlLists = new JPanel(new GridLayout(2, 1));

        usersModel = new DefaultListModel<>();
        listUsers = new JList<>(usersModel);
        setupList(listUsers);
        listUsers.addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting() && listUsers.getSelectedValue() != null) {
                listGroups.clearSelection();
                String target = listUsers.getSelectedValue().getUsername();
                lblCurrentChat.setText(target);
                carregarHistorico(target);
                atualizarVisibilidadeBotaoEntrar();
            }
        });
        
        groupsModel = new DefaultListModel<>();
        listGroups = new JList<>(groupsModel);
        setupList(listGroups);
        listGroups.addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting() && listGroups.getSelectedValue() != null) {
                listUsers.clearSelection();
                String target = listGroups.getSelectedValue().getNome();
                lblCurrentChat.setText(target);
                carregarHistorico(target);
                atualizarVisibilidadeBotaoEntrar();
            }
        });

        pnlLists.add(createSectionPanel("Contatos", createScroll(listUsers)));
        pnlLists.add(createSectionPanel("Grupos", createScroll(listGroups)));
        pnlLeft.add(pnlLists, BorderLayout.CENTER);

        add(pnlLeft, BorderLayout.WEST);

        // Right Panel
        JPanel pnlRight = new JPanel(new BorderLayout());
        pnlRight.setBackground(bgChat);

        // Header Right
        JPanel pnlRightHeader = new JPanel(new BorderLayout());
        pnlRightHeader.setBackground(headerBg);
        pnlRightHeader.setPreferredSize(new Dimension(0, 65));
        pnlRightHeader.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        lblCurrentChat = new JLabel("Selecione um chat na barra lateral");
        lblCurrentChat.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblCurrentChat.setForeground(textColor);
        pnlRightHeader.add(lblCurrentChat, BorderLayout.WEST);

        JPanel pnlHeaderButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlHeaderButtons.setOpaque(false);

        btnJoinGroupHeader = createGroupButton("Entrar Grupo");
        btnJoinGroupHeader.setToolTipText("Entrar em Grupo");
        btnJoinGroupHeader.addActionListener(e -> pedirEntradaGrupo());
        btnJoinGroupHeader.setVisible(false);
        pnlHeaderButtons.add(btnJoinGroupHeader);

        btnLeaveGroupHeader = createGroupButton("Sair Grupo");
        btnLeaveGroupHeader.setToolTipText("Sair do Grupo");
        btnLeaveGroupHeader.addActionListener(e -> sairGrupo());
        btnLeaveGroupHeader.setVisible(false);
        pnlHeaderButtons.add(btnLeaveGroupHeader);

        pnlRightHeader.add(pnlHeaderButtons, BorderLayout.EAST);

        pnlRight.add(pnlRightHeader, BorderLayout.NORTH);

        // Chat Area
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(bgChat);
        chatPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        JPanel chatWrapper = new JPanel(new BorderLayout());
        chatWrapper.setBackground(bgChat);
        chatWrapper.add(chatPanel, BorderLayout.NORTH);
        
        scrollChat = new JScrollPane(chatWrapper);
        scrollChat.setBorder(null);
        scrollChat.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollChat.getVerticalScrollBar().setUnitIncrement(16);
        scrollChat.getViewport().setBackground(bgChat);
        pnlRight.add(scrollChat, BorderLayout.CENTER);

        // Bottom Input
        JPanel pnlBottom = new JPanel(new BorderLayout(10, 0));
        pnlBottom.setBackground(headerBg);
        pnlBottom.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        JButton btnAttach = createMenuButton("+");
        btnAttach.addActionListener(e -> enviarArquivo());
        pnlBottom.add(btnAttach, BorderLayout.WEST);

        txtInput = new JTextField();
        txtInput.setBackground(new Color(255, 255, 255));
        txtInput.setForeground(textColor);
        txtInput.setCaretColor(textColor);
        txtInput.setFont(new Font("SansSerif", Font.PLAIN, 15));
        txtInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255), 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        txtInput.addActionListener(e -> enviarMensagem());
        pnlBottom.add(txtInput, BorderLayout.CENTER);

        JButton btnSend = createMenuButton(">");
        btnSend.addActionListener(e -> enviarMensagem());
        pnlBottom.add(btnSend, BorderLayout.EAST);

        pnlRight.add(pnlBottom, BorderLayout.SOUTH);
        add(pnlRight, BorderLayout.CENTER);
    }

    private JScrollPane createScroll(JComponent comp) {
        JScrollPane scroll = new JScrollPane(comp);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(bgList);
        scroll.getVerticalScrollBar().setBackground(bgList);
        return scroll;
    }

    private JPanel createSectionPanel(String title, JScrollPane scroll) {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBackground(bgList);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(new Color(134, 150, 160));
        lbl.setBorder(new EmptyBorder(10, 15, 5, 15));
        pnl.add(lbl, BorderLayout.NORTH);
        pnl.add(scroll, BorderLayout.CENTER);
        return pnl;
    }

    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(new Color(134, 150, 160));
        btn.setBorder(new EmptyBorder(5, 5, 5, 5));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        return btn;
    }

    private JButton createGroupButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setContentAreaFilled(true);
        btn.setForeground(new Color(84, 101, 111));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 224, 228), 1, true),
            new EmptyBorder(6, 6, 6, 6)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        return btn;
    }

    private void setupList(JList<?> list) {
        list.setBackground(bgList);
        list.setForeground(textColor);
        list.setSelectionBackground(new Color(240, 242, 245));
        list.setSelectionForeground(textColor);
        list.setCellRenderer(new ContactListRenderer());
        list.setFixedCellHeight(70);
    }

    private class ContactListRenderer extends DefaultListCellRenderer {
        private JPanel pnl = new JPanel(new BorderLayout(15, 0));
        private JLabel lblIcon = new JLabel();
        private JLabel lblName = new JLabel();
        private JPanel bottomLine = new JPanel();
        
        public ContactListRenderer() {
            pnl.setBorder(new EmptyBorder(10, 15, 0, 15));
            lblName.setFont(new Font("SansSerif", Font.PLAIN, 16));
            
            bottomLine.setBackground(dividerColor);
            bottomLine.setPreferredSize(new Dimension(0, 1));
            
            pnl.add(lblIcon, BorderLayout.WEST);
            pnl.add(lblName, BorderLayout.CENTER);
            pnl.add(bottomLine, BorderLayout.SOUTH);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String name = value.toString();
            lblName.setText(name);
            lblName.setForeground(textColor);
            lblIcon.setIcon(createAvatarIcon(name.substring(0,1).toUpperCase()));
            pnl.setBackground(isSelected ? new Color(240, 242, 245) : bgList);
            return pnl;
        }
    }

    private Icon createAvatarIcon(String letter) {
        return createAvatarIcon(letter, 48);
    }

    private Icon createAvatarIcon(String letter, int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(97, 175, 239));
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, (int)(size * 0.45)));
                FontMetrics fm = g2.getFontMetrics();
                int w = fm.stringWidth(letter);
                int h = fm.getAscent();
                g2.drawString(letter, x + size/2 - w/2, y + size/2 + h/2 - 2);
                g2.dispose();
            }
            @Override
            public int getIconWidth() { return size; }
            @Override
            public int getIconHeight() { return size; }
        };
    }

    private class ChatBubble extends JPanel {
        public ChatBubble(String senderName, Mensagem msg, boolean isMine) {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(5, isMine ? 50 : 0, 5, isMine ? 0 : 50));
            
            JPanel innerBubble = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isMine ? colorSent : colorReceived);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                    g2.dispose();
                }
            };
            innerBubble.setOpaque(false);
            innerBubble.setLayout(new BoxLayout(innerBubble, BoxLayout.Y_AXIS));
            innerBubble.setBorder(new EmptyBorder(8, 12, 8, 12));

            if (!isMine) {
                JLabel lblSender = new JLabel(senderName);
                lblSender.setForeground(new Color(15, 142, 186));
                lblSender.setFont(new Font("SansSerif", Font.BOLD, 13));
                lblSender.setAlignmentX(Component.LEFT_ALIGNMENT);
                innerBubble.add(lblSender);
                innerBubble.add(Box.createVerticalStrut(4));
            }

            if (msg.getArquivo() != null) {
                // File Card Panel
                JPanel fileCard = new JPanel(new BorderLayout(12, 0)) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(0, 0, 0, 15));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                        g2.dispose();
                    }
                };
                fileCard.setOpaque(false);
                fileCard.setBorder(new EmptyBorder(8, 12, 8, 12));
                fileCard.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                ArquivoInfo ai = msg.getArquivo();
                String fileName = ai.getNomeArquivo();
                long sizeBytes = ai.getDados().length;
                String ext = "FILE";
                if (fileName.contains(".")) {
                    ext = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
                }
                
                String sizeStr = "";
                if (sizeBytes < 1024) sizeStr = sizeBytes + " B";
                else if (sizeBytes < 1024 * 1024) sizeStr = String.format("%.1f KB", sizeBytes / 1024.0);
                else sizeStr = String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
                
                // Left: File Icon
                JLabel lblFileIcon = new JLabel(createFileIcon(ext, 36, 42));
                fileCard.add(lblFileIcon, BorderLayout.WEST);
                
                // Center: Name and details
                JPanel pnlDetails = new JPanel();
                pnlDetails.setLayout(new BoxLayout(pnlDetails, BoxLayout.Y_AXIS));
                pnlDetails.setOpaque(false);
                
                JLabel lblFileName = new JLabel(fileName);
                lblFileName.setFont(new Font("SansSerif", Font.BOLD, 14));
                lblFileName.setForeground(textColor);
                
                JLabel lblFileSub = new JLabel(ext + " · " + sizeStr);
                lblFileSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
                lblFileSub.setForeground(new Color(134, 150, 160));
                
                pnlDetails.add(lblFileName);
                pnlDetails.add(Box.createVerticalStrut(2));
                pnlDetails.add(lblFileSub);
                fileCard.add(pnlDetails, BorderLayout.CENTER);
                
                // Right: Download Icon
                JLabel lblDownload = new JLabel(createDownloadIcon(24));
                File savedFile = arquivosSalvos.get(ai);
                boolean isAlreadySaved = (savedFile != null && savedFile.exists());
                lblDownload.setVisible(!isAlreadySaved);
                fileCard.add(lblDownload, BorderLayout.EAST);
                
                innerBubble.add(fileCard);
                
                // Click listener
                innerBubble.setCursor(new Cursor(Cursor.HAND_CURSOR));
                innerBubble.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        salvarEAbrirArquivo(ai, lblDownload);
                    }
                });
            } else {
                // Text Message Panel
                JTextArea txtArea = new JTextArea(msg.getTexto());
                txtArea.setEditable(false);
                txtArea.setOpaque(false);
                txtArea.setLineWrap(true);
                txtArea.setWrapStyleWord(true);
                txtArea.setFont(new Font("SansSerif", Font.PLAIN, 15));
                txtArea.setForeground(textColor);
                txtArea.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                int approxCols = Math.min(msg.getTexto().length(), 40);
                txtArea.setColumns(approxCols > 0 ? approxCols : 1);
                
                innerBubble.add(txtArea);
            }
            
            JPanel wrap = new JPanel(new FlowLayout(isMine ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
            wrap.setOpaque(false);
            wrap.add(innerBubble);
            
            add(wrap, BorderLayout.CENTER);
        }
    }

    private Icon createFileIcon(String extension, int width, int height) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(110, 118, 125));
                g2.fillRoundRect(x, y, width, height, 6, 6);
                
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                String text = extension.toUpperCase();
                int w = fm.stringWidth(text);
                int h = fm.getAscent();
                g2.drawString(text, x + width/2 - w/2, y + height/2 + h/2 - 1);
                g2.dispose();
            }
            @Override
            public int getIconWidth() { return width; }
            @Override
            public int getIconHeight() { return height; }
        };
    }

    private Icon createDownloadIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(84, 101, 111, 60));
                g2.drawOval(x, y, size, size);
                
                g2.setColor(new Color(84, 101, 111));
                g2.setStroke(new BasicStroke(2));
                int cx = x + size/2;
                int cy = y + size/2;
                g2.drawLine(cx, cy - 5, cx, cy + 3);
                g2.drawLine(cx - 3, cy, cx, cy + 3);
                g2.drawLine(cx + 3, cy, cx, cy + 3);
                g2.dispose();
            }
            @Override
            public int getIconWidth() { return size; }
            @Override
            public int getIconHeight() { return size; }
        };
    }

    private void salvarEAbrirArquivo(ArquivoInfo ai, JLabel lblDownload) {
        if (ai == null) return;
        
        File savedFile = arquivosSalvos.get(ai);
        if (savedFile != null && savedFile.exists()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(savedFile);
                } else {
                    exibirNotificacao("Abertura automática de arquivos não suportada neste sistema.");
                }
            } catch (Exception e) {
                exibirNotificacao("Erro ao abrir o arquivo: " + e.getMessage());
            }
            return;
        }

        JFileChooser jfc = new JFileChooser();
        jfc.setSelectedFile(new File(ai.getNomeArquivo()));
        if (jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            savedFile = jfc.getSelectedFile();
            try (FileOutputStream fos = new FileOutputStream(savedFile)) {
                fos.write(ai.getDados());
                arquivosSalvos.put(ai, savedFile);
                
                if (lblDownload != null) {
                    lblDownload.setVisible(false);
                    if (lblDownload.getParent() != null) {
                        lblDownload.getParent().revalidate();
                        lblDownload.getParent().repaint();
                    }
                }
                
                int abrir = JOptionPane.showConfirmDialog(this,
                    "Arquivo salvo com sucesso!\nDeseja abrir o arquivo?",
                    "Abrir Arquivo", JOptionPane.YES_NO_OPTION);
                if (abrir == JOptionPane.YES_OPTION) {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(savedFile);
                    } else {
                        exibirNotificacao("Abertura automática de arquivos não suportada neste sistema.");
                    }
                }
            } catch (Exception e) {
                exibirNotificacao("Erro ao salvar/abrir o arquivo: " + e.getMessage());
            }
        }
    }

    private void fazerLogout() {
        try {
            System.out.println("[CLIENTE AÇÃO] Efetuando logout do usuário: " + me.getUsername());
            server.logout(me.getUsername());
            System.exit(0);
        } catch (Exception ex) {
            System.out.println("[CLIENTE ERRO] Falha no logout: " + ex.getMessage());
            System.exit(1);
        }
    }

    private void criarGrupo() {
        String nome = JOptionPane.showInputDialog(this, "Nome do novo grupo:");
        if (nome != null && !nome.trim().isEmpty()) {
            try {
                System.out.println("[CLIENTE AÇÃO] Criando grupo: '" + nome.trim() + "'");
                boolean criado = server.criarGrupo(nome.trim(), me.getUsername());
                if (!criado) {
                    System.out.println("[CLIENTE INFO] Criação de grupo falhou: Grupo já existe.");
                    exibirNotificacao("Grupo ja existe!");
                } else {
                    System.out.println("[CLIENTE INFO] Grupo '" + nome.trim() + "' criado com sucesso.");
                }
            } catch (Exception e) {
                System.out.println("[CLIENTE ERRO] Erro ao criar grupo: " + e.getMessage());
                exibirNotificacao("Erro ao criar grupo.");
            }
        }
    }

    private void pedirEntradaGrupo() {
        Grupo g = listGroups.getSelectedValue();
        if (g != null) {
            try {
                System.out.println("[CLIENTE AÇÃO] Solicitando entrada no grupo: '" + g.getNome() + "'");
                server.pedirEntradaGrupo(g.getNome(), me.getUsername());
                exibirNotificacao("Pedido enviado ao admin do grupo " + g.getNome());
            } catch (Exception e) {
                System.out.println("[CLIENTE ERRO] Erro ao solicitar entrada no grupo: " + e.getMessage());
                exibirNotificacao("Erro: " + e.getMessage());
            }
        } else {
            exibirNotificacao("Selecione um grupo.");
        }
    }

    private void sairGrupo() {
        Grupo g = listGroups.getSelectedValue();
        if (g != null) {
            try {
                System.out.println("[CLIENTE AÇÃO] Saindo do grupo: '" + g.getNome() + "'");
                server.sairDoGrupo(g.getNome(), me.getUsername());
            } catch (Exception e) {
                System.out.println("[CLIENTE ERRO] Erro ao sair do grupo: " + e.getMessage());
            }
        }
    }

    private void enviarMensagem() {
        String text = txtInput.getText().trim();
        if (text.isEmpty()) return;

        Grupo g = listGroups.getSelectedValue();
        Usuario u = listUsers.getSelectedValue();

        try {
            if (g != null) {
                if (!g.getMembros().contains(me.getUsername())) {
                    int resp = JOptionPane.showConfirmDialog(this,
                        "Você não é membro do grupo " + g.getNome() + ". Deseja solicitar entrada?",
                        "Solicitar Entrada", JOptionPane.YES_NO_OPTION);
                    if (resp == JOptionPane.YES_OPTION) {
                        System.out.println("[CLIENTE AÇÃO] Solicitando entrada no grupo '" + g.getNome() + "' para enviar mensagem.");
                        server.pedirEntradaGrupo(g.getNome(), me.getUsername());
                        exibirNotificacao("Pedido enviado ao admin do grupo " + g.getNome());
                    }
                    return;
                }
                System.out.println("[CLIENTE AÇÃO] Enviando mensagem para grupo '" + g.getNome() + "': \"" + text + "\"");
                server.enviarMensagemGrupo(me.getUsername(), g.getNome(), text);
                exibirMensagemLocal(new Mensagem(me.getUsername(), g.getNome(), text, true));
            } else if (u != null) {
                System.out.println("[CLIENTE AÇÃO] Enviando mensagem privada para '" + u.getUsername() + "': \"" + text + "\"");
                server.enviarMensagemPrivada(me.getUsername(), u.getUsername(), text);
                exibirMensagemLocal(new Mensagem(me.getUsername(), u.getUsername(), text, false));
            } else {
                exibirNotificacao("Selecione um usuario ou grupo destino.");
            }
            txtInput.setText("");
        } catch (Exception e) {
            System.out.println("[CLIENTE ERRO] Erro ao enviar mensagem: " + e.getMessage());
            exibirNotificacao("Erro: " + e.getMessage());
        }
    }

    private void enviarArquivo() {
        Usuario u = listUsers.getSelectedValue();
        Grupo g = listGroups.getSelectedValue();
        if (u == null && g == null) {
            exibirNotificacao("Selecione um usuario ou grupo destino.");
            return;
        }

        if (g != null && !g.getMembros().contains(me.getUsername())) {
            exibirNotificacao("Você não é membro deste grupo para enviar arquivos.");
            return;
        }

        JFileChooser jfc = new JFileChooser();
        if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = jfc.getSelectedFile();
            try {
                byte[] data = Files.readAllBytes(f.toPath());
                String dest = (g != null) ? g.getNome() : u.getUsername();
                boolean isGrp = (g != null);
                
                System.out.println("[CLIENTE AÇÃO] Enviando arquivo '" + f.getName() + "' (" + data.length + " bytes) para " + (isGrp ? "grupo '" + dest + "'" : "usuário '" + dest + "'"));
                ArquivoInfo ai = new ArquivoInfo(me.getUsername(), dest, f.getName(), data, isGrp);
                server.enviarArquivo(me.getUsername(), dest, ai);
                
                Mensagem msg = new Mensagem(me.getUsername(), dest, "[Arquivo] " + f.getName(), isGrp, ai);
                exibirMensagemLocal(msg);
            } catch (Exception e) {
                System.out.println("[CLIENTE ERRO] Erro ao enviar arquivo: " + e.getMessage());
                exibirNotificacao("Erro ao ler/enviar arquivo: " + e.getMessage());
            }
        }
    }

    private void adicionarMensagemHistorico(String chatTarget, Mensagem msg) {
        historicoMensagens.computeIfAbsent(chatTarget, k -> new java.util.ArrayList<>()).add(msg);
    }

    private void carregarHistorico(String chatTarget) {
        chatPanel.removeAll();
        java.util.List<Mensagem> msgs = historicoMensagens.get(chatTarget);
        if (msgs != null) {
            for (Mensagem msg : msgs) {
                boolean isMine = msg.getRemetente().equals(me.getUsername());
                chatPanel.add(new ChatBubble(msg.getRemetente(), msg, isMine));
            }
        }
        chatPanel.revalidate();
        chatPanel.repaint();
        scrollToBottom();
    }

    public void exibirMensagem(Mensagem msg) {
        SwingUtilities.invokeLater(() -> {
            String chatTarget = msg.isGrupo() ? msg.getDestinatario() : msg.getRemetente();
            adicionarMensagemHistorico(chatTarget, msg);
            
            String currentChat = listUsers.getSelectedValue() != null ? listUsers.getSelectedValue().getUsername() : 
                                (listGroups.getSelectedValue() != null ? listGroups.getSelectedValue().getNome() : null);
            
            if (chatTarget.equals(currentChat)) {
                chatPanel.add(new ChatBubble(msg.getRemetente(), msg, false));
                chatPanel.revalidate();
                chatPanel.repaint();
                scrollToBottom();
            } else {
                exibirNotificacao("Nova mensagem de " + msg.getRemetente() + (msg.isGrupo() ? " no grupo " + msg.getDestinatario() : ""));
            }
        });
    }
    
    private void exibirMensagemLocal(Mensagem msg) {
        String chatTarget = msg.getDestinatario();
        adicionarMensagemHistorico(chatTarget, msg);
        
        chatPanel.add(new ChatBubble(me.getUsername(), msg, true));
        chatPanel.revalidate();
        chatPanel.repaint();
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            if (scrollChat != null) {
                JScrollBar vertical = scrollChat.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
    }

    public void atualizarUsuarios(List<Usuario> usuarios) {
        SwingUtilities.invokeLater(() -> {
            Usuario sel = listUsers.getSelectedValue();
            usersModel.clear();
            for (Usuario u : usuarios) {
                if (!u.getUsername().equals(me.getUsername())) {
                    usersModel.addElement(u);
                }
            }
            if (sel != null && usersModel.contains(sel)) {
                listUsers.setSelectedValue(sel, true);
            }
        });
    }

    public void atualizarGrupos(List<Grupo> grupos) {
        SwingUtilities.invokeLater(() -> {
            Grupo sel = listGroups.getSelectedValue();
            groupsModel.clear();
            for (Grupo g : grupos) {
                groupsModel.addElement(g);
            }
            if (sel != null && groupsModel.contains(sel)) {
                listGroups.setSelectedValue(sel, true);
            }
            atualizarVisibilidadeBotaoEntrar();
        });
    }

    public void exibirNotificacao(String notificacao) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, notificacao);
        });
    }

    public void lidarComPedidoGrupo(String nomeGrupo, String solicitante) {
        SwingUtilities.invokeLater(() -> {
            int resp = JOptionPane.showConfirmDialog(this, 
                "O usuario " + solicitante + " deseja entrar no grupo " + nomeGrupo + ".\nAprovar?",
                "Pedido de Entrada", JOptionPane.YES_NO_OPTION);
            
            boolean aprovado = (resp == JOptionPane.YES_OPTION);
            try {
                server.responderEntradaGrupo(nomeGrupo, solicitante, aprovado);
            } catch (Exception e) {
                exibirNotificacao("Erro ao responder pedido.");
            }
        });
    }

    public void encerrarPorBanimento() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "VOCE FOI BANIDO DO SERVIDOR.");
            System.exit(0);
        });
    }

    public void receberEProcessarArquivo(ArquivoInfo ai) {
        SwingUtilities.invokeLater(() -> {
            String chatTarget = ai.isGrupo() ? ai.getDestinatario() : ai.getRemetente();
            Mensagem msg = new Mensagem(ai.getRemetente(), ai.getDestinatario(), "[Arquivo] " + ai.getNomeArquivo(), ai.isGrupo(), ai);
            adicionarMensagemHistorico(chatTarget, msg);
            
            String currentChat = listUsers.getSelectedValue() != null ? listUsers.getSelectedValue().getUsername() : 
                                (listGroups.getSelectedValue() != null ? listGroups.getSelectedValue().getNome() : null);
            
            if (chatTarget.equals(currentChat)) {
                chatPanel.add(new ChatBubble(ai.getRemetente(), msg, false));
                chatPanel.revalidate();
                chatPanel.repaint();
                scrollToBottom();
            } else {
                exibirNotificacao("Novo arquivo de " + ai.getRemetente() + (ai.isGrupo() ? " no grupo " + ai.getDestinatario() : ""));
            }
        });
    }

    private void atualizarVisibilidadeBotaoEntrar() {
        Grupo g = listGroups.getSelectedValue();
        if (g != null) {
            boolean isMember = g.getMembros().contains(me.getUsername());
            btnJoinGroupHeader.setVisible(!isMember);
            btnLeaveGroupHeader.setVisible(isMember);
        } else {
            btnJoinGroupHeader.setVisible(false);
            btnLeaveGroupHeader.setVisible(false);
        }
        if (btnJoinGroupHeader != null && btnJoinGroupHeader.getParent() != null) {
            btnJoinGroupHeader.getParent().revalidate();
            btnJoinGroupHeader.getParent().repaint();
        }
    }
}
