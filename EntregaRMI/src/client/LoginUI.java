package client;

import common.IWhatsUTServer;
import common.Usuario;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginUI extends JFrame {

    private JTextField txtIp, txtUser;
    private JPasswordField txtPass;

    public LoginUI() {
        setTitle("WhatsUT - Login");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        getContentPane().setBackground(new Color(245, 245, 245));
        setLayout(new GridLayout(5, 1, 10, 10));

        JPanel pnlIp = criarPainelComLabel("IP Servidor:", "localhost");
        txtIp = (JTextField) pnlIp.getComponent(1);
        
        JPanel pnlUser = criarPainelComLabel("Usuario:", "");
        txtUser = (JTextField) pnlUser.getComponent(1);
        
        JPanel pnlPass = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlPass.setOpaque(false);
        JLabel lblPass = new JLabel("Senha:");
        lblPass.setForeground(Color.BLACK);
        txtPass = new JPasswordField(15);
        pnlPass.add(lblPass);
        pnlPass.add(txtPass);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        pnlBtns.setOpaque(false);
        JButton btnLogin = estilizarBotao(new JButton("Login"));
        JButton btnReg = estilizarBotao(new JButton("Registrar"));
        
        btnLogin.addActionListener(e -> tentarLogin());
        btnReg.addActionListener(e -> tentarRegistro());

        pnlBtns.add(btnLogin);
        pnlBtns.add(btnReg);

        JLabel lblTitle = new JLabel("WhatsUT", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitle.setForeground(new Color(0, 102, 204));

        add(lblTitle);
        add(pnlIp);
        add(pnlUser);
        add(pnlPass);
        add(pnlBtns);
    }

    private JPanel criarPainelComLabel(String text, String defaultVal) {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnl.setOpaque(false);
        JLabel lbl = new JLabel(text);
        lbl.setForeground(Color.BLACK);
        JTextField txt = new JTextField(defaultVal, 15);
        pnl.add(lbl);
        pnl.add(txt);
        return pnl;
    }

    private JButton estilizarBotao(JButton btn) {
        btn.setBackground(new Color(0, 120, 215));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        return btn;
    }

    private String getPasswordHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(new String(txtPass.getPassword()).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private void tentarRegistro() {
        try {
            String url = "rmi://" + txtIp.getText().trim() + ":1099/WhatsUTServer";
            IWhatsUTServer server = (IWhatsUTServer) Naming.lookup(url);
            
            String user = txtUser.getText().trim();
            if (user.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Usuario nao pode ser vazio.");
                return;
            }

            boolean res = server.registrarUsuario(user, getPasswordHash());
            if (res) {
                JOptionPane.showMessageDialog(this, "Registrado com sucesso! Faca login.");
            } else {
                JOptionPane.showMessageDialog(this, "Nome de usuario ja existe.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro de conexao: " + ex.getMessage());
        }
    }

    private void tentarLogin() {
        try {
            String url = "rmi://" + txtIp.getText().trim() + ":1099/WhatsUTServer";
            IWhatsUTServer server = (IWhatsUTServer) Naming.lookup(url);
            
            String user = txtUser.getText().trim();
            WhatsUTClientImpl clientCallback = new WhatsUTClientImpl();
            
            Usuario usuario = server.login(user, getPasswordHash(), clientCallback);
            if (usuario != null) {
                ClientUI chatUi = new ClientUI(usuario, server, clientCallback);
                clientCallback.setUI(chatUi);
                chatUi.setVisible(true);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Credenciais invalidas.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro no login: " + ex.getMessage());
        }
    }
}
