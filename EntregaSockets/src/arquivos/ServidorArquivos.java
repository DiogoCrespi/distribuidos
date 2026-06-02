package arquivos;

import java.io.*;
import java.net.*;

public class ServidorArquivos {
    private static final int PORTA = 8350;
    private static final String DIR_BASE = "armazenamento/";

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
                
                String usuarioLogado = "";
                String comando = dis.readUTF();
                if (comando.startsWith("LOGIN")) {
                    String user = dis.readUTF();
                    String pass = dis.readUTF();
                    if (autenticar(user, pass)) {
                        dos.writeUTF("SUCESSO: Autenticado.");
                        usuarioLogado = user;
                        comando = dis.readUTF(); // Ler o proximo comando após login
                    } else {
                        dos.writeUTF("ERRO: Credenciais invalidas.");
                        return;
                    }
                }

                String subDir = usuarioLogado.isEmpty() ? "" : usuarioLogado + "/";

                if (comando.startsWith("UPLOAD")) {
                    String nome = dis.readUTF();
                    long tamanho = dis.readLong();
                    receberArquivo(subDir, nome, tamanho, dis);
                    dos.writeUTF("SUCESSO: Arquivo recebido.");
                } else if (comando.startsWith("DOWNLOAD")) {
                    String nome = dis.readUTF();
                    enviarArquivo(subDir, nome, dos);
                }
            } catch (IOException e) {
                System.err.println("Erro no processo de arquivo: " + e.getMessage());
            }
        }

        private boolean autenticar(String user, String pass) {
            return (user.equals("admin") && pass.equals("1234")) || (user.equals("user") && pass.equals("123"));
        }

        private void receberArquivo(String subDir, String nome, long tamanho, DataInputStream dis) throws IOException {
            File dir = new File(DIR_BASE + subDir);
            dir.mkdirs();
            File f = new File(dir, nome);
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

        private void enviarArquivo(String subDir, String nome, DataOutputStream dos) throws IOException {
            File f = new File(DIR_BASE + subDir + nome);
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
