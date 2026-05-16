package chat;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class ChatMulticast {
    private static final String IP_GRUPO = "224.0.0.1";
    private static final int PORTA = 12351;

    public static void main(String[] args) {
        System.out.print("Digite seu apelido: ");
        Scanner sc = new Scanner(System.in);
        String apelido = sc.nextLine();

        try (MulticastSocket socket = new MulticastSocket(PORTA)) {
            InetAddress group = InetAddress.getByName(IP_GRUPO);
            socket.joinGroup(group);

            // Thread para receber mensagens
            new Thread(() -> {
                try {
                    while (true) {
                        byte[] buf = new byte[1000];
                        DatagramPacket pacote = new DatagramPacket(buf, buf.length);
                        socket.receive(pacote);
                        String msg = new String(pacote.getData(), 0, pacote.getLength());
                        if (!msg.startsWith(apelido + ":")) {
                            System.out.println("\n" + msg);
                            System.out.print("> ");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Conexao fechada.");
                }
            }).start();

            System.out.println("Chat iniciado! Digite suas mensagens:");
            while (true) {
                System.out.print("> ");
                String msg = sc.nextLine();
                if (msg.equalsIgnoreCase("SAIR")) break;
                
                String formatada = apelido + ": " + msg;
                byte[] b = formatada.getBytes();
                DatagramPacket p = new DatagramPacket(b, b.length, group, PORTA);
                socket.send(p);
            }
            socket.leaveGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
