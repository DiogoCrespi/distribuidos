package client;

import common.*;

import javax.swing.*;
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

    private JTextArea txtChat;
    private JTextField txtInput;

    public ClientUI(Usuario me, IWhatsUTServer server, WhatsUTClientImpl callbackImpl) {
        this.me = me;
        this.server = server;
        this.callbackImpl = callbackImpl;

        setTitle("WhatsUT - " + me.getUsername());
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                fazerLogout();
            }
        });

        initComponents();
        
        // Pede as listas iniciais
        try {
            atualizarUsuarios(server.getUsuariosLogados());
            atualizarGrupos(server.getGrupos());
        } catch (Exception e) {
            exibirNotificacao("Erro ao buscar listas");
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Painel Esquerdo: Listas
        JPanel pnlLeft = new JPanel(new GridLayout(2, 1));
        
        usersModel = new DefaultListModel<>();
        listUsers = new JList<>(usersModel);
        pnlLeft.add(criarTitledScroll(listUsers, "Usuários Online"));

        groupsModel = new DefaultListModel<>();
        listGroups = new JList<>(groupsModel);
        pnlLeft.add(criarTitledScroll(listGroups, "Grupos"));

        add(pnlLeft, BorderLayout.WEST);

        // Painel Central: Chat
        txtChat = new JTextArea();
        txtChat.setEditable(false);
        txtChat.setFont(new Font("Monospaced", Font.PLAIN, 14));
        txtChat.setBackground(new Color(40, 44, 52));
        txtChat.setForeground(Color.WHITE);
        JScrollPane scrollChat = new JScrollPane(txtChat);
        add(scrollChat, BorderLayout.CENTER);

        // Painel Inferior: Input e Botoes
        JPanel pnlBottom = new JPanel(new BorderLayout());
        txtInput = new JTextField();
        pnlBottom.add(txtInput, BorderLayout.CENTER);

        JPanel pnlActions = new JPanel(new FlowLayout());
        JButton btnSendMsg = new JButton("Enviar Msg");
        JButton btnSendFile = new JButton("Enviar Arquivo");
        
        btnSendMsg.addActionListener(e -> enviarMensagem());
        btnSendFile.addActionListener(e -> enviarArquivo());
        
        pnlActions.add(btnSendMsg);
        pnlActions.add(btnSendFile);
        pnlBottom.add(pnlActions, BorderLayout.EAST);
        
        add(pnlBottom, BorderLayout.SOUTH);

        // Painel Superior: Açoes globais
        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnCreateGrp = new JButton("Criar Grupo");
        JButton btnJoinGrp = new JButton("Entrar em Grupo");
        JButton btnLeaveGrp = new JButton("Sair de Grupo");
        
        btnCreateGrp.addActionListener(e -> criarGrupo());
        btnJoinGrp.addActionListener(e -> pedirEntradaGrupo());
        btnLeaveGrp.addActionListener(e -> sairGrupo());

        pnlTop.add(btnCreateGrp);
        pnlTop.add(btnJoinGrp);
        pnlTop.add(btnLeaveGrp);
        add(pnlTop, BorderLayout.NORTH);
    }

    private JScrollPane criarTitledScroll(JComponent comp, String title) {
        JScrollPane scroll = new JScrollPane(comp);
        scroll.setBorder(BorderFactory.createTitledBorder(title));
        return scroll;
    }

    private void fazerLogout() {
        try {
            server.logout(me.getUsername());
            System.exit(0);
        } catch (Exception ex) {
            System.exit(1);
        }
    }

    private void criarGrupo() {
        String nome = JOptionPane.showInputDialog(this, "Nome do novo grupo:");
        if (nome != null && !nome.trim().isEmpty()) {
            try {
                boolean criado = server.criarGrupo(nome.trim(), me.getUsername());
                if (!criado) exibirNotificacao("Grupo já existe!");
            } catch (Exception e) {
                exibirNotificacao("Erro ao criar grupo.");
            }
        }
    }

    private void pedirEntradaGrupo() {
        Grupo g = listGroups.getSelectedValue();
        if (g != null) {
            try {
                server.pedirEntradaGrupo(g.getNome(), me.getUsername());
                exibirNotificacao("Pedido enviado ao admin do grupo " + g.getNome());
            } catch (Exception e) {
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
                server.sairDoGrupo(g.getNome(), me.getUsername());
            } catch (Exception e) {}
        }
    }

    private void enviarMensagem() {
        String text = txtInput.getText().trim();
        if (text.isEmpty()) return;

        // Se tiver grupo selecionado, envia pro grupo. Senao tenta privado
        Grupo g = listGroups.getSelectedValue();
        Usuario u = listUsers.getSelectedValue();

        try {
            if (g != null) {
                server.enviarMensagemGrupo(me.getUsername(), g.getNome(), text);
                exibirMensagemLocal(new Mensagem(me.getUsername(), g.getNome(), text, true));
            } else if (u != null) {
                server.enviarMensagemPrivada(me.getUsername(), u.getUsername(), text);
                exibirMensagemLocal(new Mensagem(me.getUsername(), u.getUsername(), text, false));
            } else {
                exibirNotificacao("Selecione um usuario ou grupo destino.");
            }
            txtInput.setText("");
        } catch (Exception e) {
            exibirNotificacao("Erro: " + e.getMessage());
        }
    }

    private void enviarArquivo() {
        Usuario u = listUsers.getSelectedValue();
        if (u == null) {
            exibirNotificacao("Envio de arquivo eh apenas privado. Selecione um usuario.");
            return;
        }

        JFileChooser jfc = new JFileChooser();
        if (jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = jfc.getSelectedFile();
            try {
                byte[] data = Files.readAllBytes(f.toPath());
                ArquivoInfo ai = new ArquivoInfo(me.getUsername(), f.getName(), data);
                server.enviarArquivo(me.getUsername(), u.getUsername(), ai);
                exibirNotificacao("Arquivo " + f.getName() + " enviado para " + u.getUsername());
            } catch (Exception e) {
                exibirNotificacao("Erro ao ler/enviar arquivo.");
            }
        }
    }

    // --- Metodos de Callback ---

    public void exibirMensagem(Mensagem msg) {
        SwingUtilities.invokeLater(() -> {
            txtChat.append(msg.toString() + "\n");
        });
    }
    
    private void exibirMensagemLocal(Mensagem msg) {
        txtChat.append("Você " + msg.toString() + "\n");
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
                "O usuário " + solicitante + " deseja entrar no grupo " + nomeGrupo + ".\nAprovar?",
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
            JOptionPane.showMessageDialog(this, "VOCÊ FOI BANIDO DO SERVIDOR.");
            System.exit(0);
        });
    }

    public void receberEProcessarArquivo(ArquivoInfo ai) {
        SwingUtilities.invokeLater(() -> {
            int resp = JOptionPane.showConfirmDialog(this, 
                "Usuário " + ai.getRemetente() + " te enviou o arquivo: " + ai.getNomeArquivo() + "\nDeseja salvar?",
                "Recebimento de Arquivo", JOptionPane.YES_NO_OPTION);
            
            if (resp == JOptionPane.YES_OPTION) {
                JFileChooser jfc = new JFileChooser();
                jfc.setSelectedFile(new File(ai.getNomeArquivo()));
                if (jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    try (FileOutputStream fos = new FileOutputStream(jfc.getSelectedFile())) {
                        fos.write(ai.getDados());
                        exibirNotificacao("Arquivo salvo!");
                    } catch (Exception e) {
                        exibirNotificacao("Erro ao salvar arquivo.");
                    }
                }
            }
        });
    }
}
