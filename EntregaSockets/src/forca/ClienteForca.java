package forca;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteForca {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12347);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {
            
            System.out.println(in.readLine()); // Bem-vindo
            String tamanhoInfo = in.readLine();
            System.out.println("Palavra secreta tem " + tamanhoInfo.split(":")[1] + " letras.");

            String msg;
            while ((msg = in.readLine()) != null) {
                if (msg.startsWith("ESTADO:")) {
                    System.out.println(msg);
                    System.out.print("Sua letra: ");
                    out.println(scanner.nextLine());
                } else if (msg.startsWith("VITORIA:") || msg.startsWith("DERROTA:")) {
                    System.out.println(msg);
                    break;
                } else {
                    System.out.println(msg);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
