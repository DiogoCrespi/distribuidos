package fortune;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorFortune {
    private static final int PORTA = 12345;
    private static List<String> bancoDeFrases = new ArrayList<>(Arrays.asList(
        "A sorte favorece os audazes.",
        "O aprendizado é um tesouro que segue seu dono em qualquer lugar.",
        "A persistência é o caminho do êxito."
    ));

    public static void main(String[] args) {
        System.out.println("Servidor de Biscoitos da Sorte iniciado na porta " + PORTA);
        
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                try (Socket socketCliente = serverSocket.accept();
                     BufferedReader entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
                     PrintWriter saida = new PrintWriter(socketCliente.getOutputStream(), true)) {
                    
                    String linhaInput = entrada.readLine();
                    if (linhaInput != null) {
                        processarComando(linhaInput.trim(), saida);
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao processar cliente: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    private static void processarComando(String comandoCompleto, PrintWriter saida) {
        String[] partes = comandoCompleto.split(" ", 3);
        String comando = partes[0].toUpperCase();

        switch (comando) {
            case "GET-FORTUNE":
                if (bancoDeFrases.isEmpty()) {
                    saida.println("ERRO: Banco de frases vazio.");
                } else {
                    int index = new Random().nextInt(bancoDeFrases.size());
                    saida.println(bancoDeFrases.get(index));
                }
                break;

            case "ADD-FORTUNE":
                if (partes.length < 2) {
                    saida.println("ERRO: Formato correto ADD-FORTUNE <frase>");
                } else {
                    String novaFrase = comandoCompleto.substring(12).trim();
                    bancoDeFrases.add(novaFrase);
                    saida.println("SUCESSO: Frase adicionada.");
                }
                break;

            case "UPD-FORTUNE":
                try {
                    String[] tokens = comandoCompleto.split(" ", 3);
                    int pos = Integer.parseInt(tokens[1]);
                    String fraseEditada = tokens[2];
                    if (pos >= 0 && pos < bancoDeFrases.size()) {
                        bancoDeFrases.set(pos, fraseEditada);
                        saida.println("SUCESSO: Frase na posicao " + pos + " atualizada.");
                    } else {
                        saida.println("ERRO: Posicao invalida.");
                    }
                } catch (Exception e) {
                    saida.println("ERRO: Formato correto UPD-FORTUNE <pos> <nova frase>");
                }
                break;

            case "LST-FORTUNE":
                if (bancoDeFrases.isEmpty()) {
                    saida.println("Banco vazio.");
                } else {
                    StringBuilder lista = new StringBuilder();
                    for (int i = 0; i < bancoDeFrases.size(); i++) {
                        lista.append(i).append(": ").append(bancoDeFrases.get(i)).append(" | ");
                    }
                    saida.println(lista.toString());
                }
                break;

            default:
                saida.println("ERRO: Comando desconhecido.");
                break;
        }
    }
}
