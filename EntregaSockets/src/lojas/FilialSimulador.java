package lojas;

import java.io.*;
import java.net.*;
import java.util.Random;

public class FilialSimulador {
    public static void main(String[] args) {
        String id = args.length > 0 ? args[0] : "Filial_" + new Random().nextInt(100);
        System.out.println("Iniciando simulacao da " + id);

        try (Socket socket = new Socket("localhost", 12349);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println(id);
            Random rand = new Random();

            for (int i = 1; i <= 1500; i++) {
                double valor = 10 + (500 - 10) * rand.nextDouble(); // Venda entre 10 e 500
                out.println(valor);

                if (i % 100 == 0)
                    System.out.println(id + ": " + i + " ocorrencias enviadas...");

                // Simula delay real
                Thread.sleep(10);
            }
            System.out.println(id + " finalizou o envio de dados.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
