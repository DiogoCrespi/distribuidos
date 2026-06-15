package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorMain {
    public static void main(String[] args) {
        try {
            // Cria a instância da implementação remota
            StringProcessorImpl processador = new StringProcessorImpl();
            
            // Inicia o RMI Registry na porta padrão 1099
            Registry registry = LocateRegistry.createRegistry(1099);
            
            // Registra o objeto remoto no Registry com o nome "StringProcessorService"
            registry.rebind("StringProcessorService", processador);
            
            System.out.println("Servidor RMI (Processador de Strings) iniciado na porta 1099.");
        } catch (Exception e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
