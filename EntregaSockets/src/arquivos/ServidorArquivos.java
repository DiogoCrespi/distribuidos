package arquivos;

import java.io.*;
import java.net.*;

public class ServidorArquivos {
    private static final int PORTA = 12350;
    private static final String DIR_BASE = "c:/Users/Admin/Downloads/distribuidos/EntregaSockets/armazenamento/";

    public static void main(String[] args) {
        new File(DIR_BASE).mkdirs();
        System.out.println("Servidor de Arquivos iniciado na porta " + PORTA);
        
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                new ThreadProcesso(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ThreadProcesso extends Thread {
        private Socket socket;
        public ThreadProcesso(Socket s) { this.socket = s; }

        public void run() {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
                
                String comando = dis.readUTF();
                if (comando.startsWith("UPLOAD")) {
                    String nome = dis.readUTF();
                    long tamanho = dis.readLong();
                    receberArquivo(nome, tamanho, dis);
                    dos.writeUTF("SUCESSO: Arquivo recebido.");
                } else if (comando.startsWith("DOWNLOAD")) {
                    String nome = dis.readUTF();
                    enviarArquivo(nome, dos);
                }
            } catch (IOException e) {
                System.err.println("Erro no processo de arquivo.");
            }
        }

        private void receberArquivo(String nome, long tamanho, DataInputStream dis) throws IOException {
            File f = new File(DIR_BASE + nome);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                byte[] buffer = new byte[4096];
                long lidoTotal = 0;
                while (lidoTotal < tamanho) {
                    int lido = dis.read(buffer, 0, (int) Math.min(buffer.length, tamanho - lidoTotal));
                    fos.write(buffer, 0, lido);
                    lidoTotal += lido;
                }
            }
        }

        private void enviarArquivo(String nome, DataOutputStream dos) throws IOException {
            File f = new File(DIR_BASE + nome);
            if (!f.exists()) {
                dos.writeLong(-1); // Sinaliza erro
                return;
            }
            dos.writeLong(f.length());
            try (FileInputStream fis = new FileInputStream(f)) {
                byte[] buffer = new byte[4096];
                int lido;
                while ((lido = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, lido);
                }
            }
        }
    }
}
