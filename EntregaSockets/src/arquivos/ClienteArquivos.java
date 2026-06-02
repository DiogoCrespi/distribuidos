package arquivos;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteArquivos {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("IP do servidor (Enter para localhost): ");
        String ipServidor = sc.nextLine().trim().replaceAll("[^a-zA-Z0-9\\.\\-:]", "");
        if (ipServidor.isEmpty()) ipServidor = "localhost";

        System.out.println("1. UPLOAD  2. DOWNLOAD");
        int op = sc.nextInt();
        sc.nextLine();

        try (Socket socket = new Socket(ipServidor, 8350);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            System.out.print("Login: ");
            String login = sc.nextLine();
            System.out.print("Senha: ");
            String senha = sc.nextLine();

            // Envia credenciais
            dos.writeUTF("LOGIN");
            dos.writeUTF(login);
            dos.writeUTF(senha);

            String respostaAuth = dis.readUTF();
            System.out.println("Servidor: " + respostaAuth);
            if (respostaAuth.startsWith("ERRO")) {
                System.out.println("Acesso negado. Conexao encerrada.");
                return;
            }

            if (op == 1) {
                System.out.print("Caminho do arquivo local: ");
                File f = new File(sc.nextLine());
                dos.writeUTF("UPLOAD");
                dos.writeUTF(f.getName());
                dos.writeLong(f.length());
                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = fis.read(buf)) != -1) dos.write(buf, 0, r);
                }
                System.out.println(dis.readUTF());
            } else {
                System.out.print("Nome do arquivo no servidor: ");
                String nome = sc.nextLine();
                dos.writeUTF("DOWNLOAD");
                dos.writeUTF(nome);
                long tam = dis.readLong();
                if (tam == -1) System.out.println("Erro: Arquivo nao encontrado.");
                else {
                    File dir = new File("armazenamento/baixados");
                    dir.mkdirs();
                    File destino = new File(dir, "baixado_" + nome);
                    try (FileOutputStream fos = new FileOutputStream(destino)) {
                        byte[] buf = new byte[4096];
                        long total = 0;
                        while (total < tam) {
                            int r = dis.read(buf, 0, (int) Math.min(buf.length, tam - total));
                            fos.write(buf, 0, r);
                            total += r;
                        }
                    }
                    System.out.println("Arquivo baixado com sucesso em: " + destino.getPath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
