package client;

public class ClienteMain {
    public static void main(String[] args) {
        // Exibe a tela de login inicial
        java.awt.EventQueue.invokeLater(() -> {
            new LoginUI().setVisible(true);
        });
    }
}
