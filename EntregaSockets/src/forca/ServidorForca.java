package forca;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServidorForca {
    private static final int PORTA = 12347;
    private static final String[] PALAVRAS = {"SISTEMAS", "DISTRIBUIDOS", "SOCKETS", "THREADS", "CONCORRENCIA"};

    public static void main(String[] args) {
        System.out.println("Servidor de Jogo da Forca iniciado na porta " + PORTA);
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                new ThreadJogo(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ThreadJogo extends Thread {
        private Socket socket;

        public ThreadJogo(Socket s) { this.socket = s; }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                
                String palavra = PALAVRAS[new Random().nextInt(PALAVRAS.length)];
                char[] estado = new char[palavra.length()];
                Arrays.fill(estado, '_');
                int erros = 0;
                int maxErros = 6;
                Set<Character> tentadas = new HashSet<>();

                out.println("BEM-VINDO AO JOGO DA FORCA REMOTO!");
                out.println("TAMANHO:" + palavra.length());

                while (erros < maxErros && new String(estado).contains("_")) {
                    out.println("ESTADO:" + new String(estado) + " ERROS:" + erros + "/" + maxErros);
                    String palpiteRaw = in.readLine();
                    if (palpiteRaw == null) break;
                    
                    char palpite = palpiteRaw.toUpperCase().charAt(0);
                    if (tentadas.contains(palpite)) {
                        out.println("AVISO:Voce ja tentou essa letra!");
                        continue;
                    }
                    
                    tentadas.add(palpite);
                    boolean acertou = false;
                    for (int i = 0; i < palavra.length(); i++) {
                        if (palavra.charAt(i) == palpite) {
                            estado[i] = palpite;
                            acertou = true;
                        }
                    }
                    
                    if (!acertou) erros++;
                }

                if (!new String(estado).contains("_")) {
                    out.println("VITORIA:Parabens! A palavra era " + palavra);
                } else {
                    out.println("DERROTA:Voce foi enforcado! A palavra era " + palavra);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
