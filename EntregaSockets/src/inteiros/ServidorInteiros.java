package inteiros;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorInteiros {
    private static final int PORTA = 12346;

    public static void main(String[] args) {
        System.out.println("Servidor de Processamento de Inteiros iniciado na porta " + PORTA);
        
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                try (Socket socket = serverSocket.accept();
                     BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter saida = new PrintWriter(socket.getOutputStream(), true)) {
                    
                    List<Integer> numeros = new ArrayList<>();
                    String linha;
                    String operacao = "SOMA"; // Padrao

                    while ((linha = entrada.readLine()) != null) {
                        try {
                            numeros.add(Integer.parseInt(linha.trim()));
                        } catch (NumberFormatException e) {
                            // Se nao for numero, pode ser a operacao
                            operacao = linha.trim().toUpperCase();
                        }
                    }

                    // Processar apos EOF
                    long resultado = processar(numeros, operacao);
                    saida.println("RESULTADO (" + operacao + "): " + resultado);
                    
                } catch (IOException e) {
                    System.err.println("Erro na conexao: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    private static long processar(List<Integer> numeros, String op) {
        if (op.equals("MULT")) {
            return numeros.stream().mapToLong(n -> n).reduce(1, (a, b) -> a * b);
        }
        // Padrao: SOMA
        return numeros.stream().mapToLong(n -> n).sum();
    }
}
