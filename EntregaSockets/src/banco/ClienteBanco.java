package banco;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteBanco {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12348);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {
            
            System.out.println(in.readLine()); // Bem-vindo
            out.println(scanner.nextLine()); // Envia conta

            String msg;
            while ((msg = in.readLine()) != null) {
                System.out.println(msg);
                if (msg.contains("MENU:")) {
                    System.out.print("> ");
                    String cmd = scanner.nextLine();
                    out.println(cmd);
                    if (cmd.equals("4")) break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
