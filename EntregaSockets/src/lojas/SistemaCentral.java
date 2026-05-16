package lojas;
 
 import java.io.*;
 import java.net.*;
 
 public class SistemaCentral {
     private static final int PORTA = 12349;
     private static double totalVendas = 0;
 
     public static void main(String[] args) {
         System.out.println("Sistema Central de Lojas iniciado na porta " + PORTA);
         try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
             while (true) {
                 new ThreadFilial(serverSocket.accept()).start();
             }
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
 
     private static class ThreadFilial extends Thread {
         private Socket socket;
         public ThreadFilial(Socket s) { this.socket = s; }
 
         public void run() {
             try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                 String filialId = in.readLine();
                 System.out.println("Recebendo dados da Filial: " + filialId);
                 
                 String registro;
                 int contador = 0;
                 while ((registro = in.readLine()) != null) {
                     double valor = Double.parseDouble(registro);
                     adicionarVenda(valor);
                     contador++;
                 }
                 System.out.println("Finalizado Filial " + filialId + ". Ocorrencias: " + contador);
                 System.out.println("TOTAL ACUMULADO NO SISTEMA CENTRAL: R$ " + totalVendas);
             } catch (IOException e) {
                 System.err.println("Erro na comunicacao com filial.");
             }
         }
 
         private synchronized static void adicionarVenda(double valor) {
             totalVendas += valor;
         }
     }
 }
