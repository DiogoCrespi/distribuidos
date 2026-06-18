package server;

import common.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ServerUI extends JFrame {
    private WhatsUTServerImpl serverImpl;
    private DefaultListModel<Usuario> listModel;
    private JList<Usuario> listUsuarios;

    public ServerUI(WhatsUTServerImpl serverImpl) {
        this.serverImpl = serverImpl;

        setTitle("WhatsUT - Painel do Servidor");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Estilizando a UI para ficar com tema claro
        getContentPane().setBackground(new Color(245, 245, 245));

        JLabel lblTitle = new JLabel("Usuarios Conectados", SwingConstants.CENTER);
        lblTitle.setForeground(new Color(50, 50, 50));
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(lblTitle, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        listUsuarios = new JList<>(listModel);
        listUsuarios.setBackground(Color.WHITE);
        listUsuarios.setForeground(new Color(50, 50, 50));
        listUsuarios.setSelectionBackground(new Color(200, 220, 240));
        listUsuarios.setSelectionForeground(Color.BLACK);
        listUsuarios.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JScrollPane scroll = new JScrollPane(listUsuarios);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scroll.getViewport().setBackground(new Color(245, 245, 245));
        add(scroll, BorderLayout.CENTER);

        JButton btnBan = new JButton("Banir Usuario Selecionado");
        btnBan.setBackground(new Color(220, 53, 69));
        btnBan.setForeground(Color.WHITE);
        btnBan.setFocusPainted(false);
        btnBan.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnBan.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        btnBan.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Usuario selecionado = listUsuarios.getSelectedValue();
                if (selecionado != null) {
                    int resp = JOptionPane.showConfirmDialog(ServerUI.this, 
                        "Deseja banir " + selecionado.getUsername() + " da aplicacao?", "Confirmar Banimento", JOptionPane.YES_NO_OPTION);
                    if (resp == JOptionPane.YES_OPTION) {
                        serverImpl.banirAplicacao(selecionado.getUsername());
                        atualizarLista();
                    }
                } else {
                    JOptionPane.showMessageDialog(ServerUI.this, "Selecione um usuario para banir.");
                }
            }
        });
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(245, 245, 245));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.add(btnBan, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Thread para atualizar a interface periodicamente, caso nao use callbacks do proprio server para a UI
        Timer timer = new Timer(2000, e -> atualizarLista());
        timer.start();
    }

    public void atualizarLista() {
        Usuario selecionado = listUsuarios.getSelectedValue();
        List<Usuario> logados = serverImpl.getUsuariosLogados();
        listModel.clear();
        for (Usuario u : logados) {
            listModel.addElement(u);
        }
        if (selecionado != null && listModel.contains(selecionado)) {
            listUsuarios.setSelectedValue(selecionado, true);
        }
    }
}
