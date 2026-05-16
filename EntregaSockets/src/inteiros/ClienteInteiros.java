package inteiros;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteInteiros {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12346);
             PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            Scanner scanner = new Scanner(System.in);
            System.out.println("Digite os numeros (um por linha). Digite a operacao (SOMA/MULT) para finalizar:");
            
            while (scanner.hasNext()) {
                String input = scanner.next();
                saida.println(input);
                if (input.equalsIgnoreCase("SOMA") || input.equalsIgnoreCase("MULT")) {
                    break;
                }
            }
            
            // Fecha o output stream para sinalizar EOF para o servidor
            socket.shutdownOutput();
            
            String resposta = entrada.readLine();
            System.out.println("Resposta do servidor: " + resposta);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
