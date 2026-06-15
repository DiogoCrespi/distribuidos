package server;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class ServidorMain {
    public static void main(String[] args) {
        try {
            // Cria o registro RMI nativo na porta 1099
            LocateRegistry.createRegistry(1099);
            System.out.println("RMI Registry iniciado na porta 1099");

            // Instancia o servente
            WhatsUTServerImpl serverImpl = new WhatsUTServerImpl();

            // Registra o servente no Registry com nome publico
            Naming.rebind("rmi://localhost:1099/WhatsUTServer", serverImpl);
            System.out.println("Servidor WhatsUT registrado com sucesso!");

            // Inicia a interface grafica do servidor
            ServerUI ui = new ServerUI(serverImpl);
            ui.setVisible(true);
            
        } catch (Exception e) {
            System.err.println("Erro no Servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
