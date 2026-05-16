package fortune;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteFortune {
    private static final String ENDERECO = "localhost";
    private static final int PORTA = 12345;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n--- MENU FORTUNE ---");
            System.out.println("1. GET-FORTUNE");
            System.out.println("2. ADD-FORTUNE <frase>");
            System.out.println("3. UPD-FORTUNE <pos> <frase>");
            System.out.println("4. LST-FORTUNE");
            System.out.println("0. Sair");
            System.out.print("Comando: ");
            
            String opcao = scanner.nextLine();
            if (opcao.equals("0")) break;

            enviarComando(opcao);
        }
        scanner.close();
    }

    private static void enviarComando(String cmd) {
        try (Socket socket = new Socket(ENDERECO, PORTA);
             PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            saida.println(cmd);
            String resposta = entrada.readLine();
            System.out.println("Resposta do Servidor: " + resposta);
            
        } catch (IOException e) {
            System.err.println("Erro ao conectar ao servidor: " + e.getMessage());
        }
    }
}
