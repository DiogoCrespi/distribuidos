package bar;
 
 import java.util.*;
 
 public class BarPrincipal {
     public static void main(String[] args) {
         // Parametros solicitados
         int nClientes = 10;
         int xGarcons = 2;
         int cCapacidade = 3;
         int nRodadas = 5;
 
         System.out.println("--- Simulacao do Bar Iniciada ---");
         System.out.println("Clientes: " + nClientes + " | Garcons: " + xGarcons + " | Capacidade: " + cCapacidade);
 
         Bar bar = new Bar(nRodadas, cCapacidade, nClientes);
         
         for (int i = 0; i < nClientes; i++) {
             new Fregues(i, bar).start();
         }
 
         for (int i = 0; i < xGarcons; i++) {
             new Garcom(i, bar).start();
         }
     }
 }
 
 class Bar {
     private final int maxRodadas;
     private final int capacidadeGarcom;
     private final int totalClientes;
     private int rodadaAtual = 0;
     private boolean barAberto = true;
     
     // Fila de pedidos usando lista comum para sincronizacao manual
     private final List<Integer> filaPedidos = new LinkedList<>();
     
     public Bar(int rodadas, int cap, int clientes) {
         this.maxRodadas = rodadas;
         this.capacidadeGarcom = cap;
         this.totalClientes = clientes;
     }
 
     public synchronized boolean estaAberto() {
         return barAberto;
     }
 
     public synchronized void fazerPedido(int clienteId) throws InterruptedException {
         filaPedidos.add(clienteId);
         System.out.println("Cliente " + clienteId + " fez um pedido.");
         notifyAll(); // Notifica garçons que há um novo pedido
     }
 
     public synchronized List<Integer> coletarPedidos() throws InterruptedException {
         while (filaPedidos.size() < capacidadeGarcom && estaAberto()) {
             // Garçom espera até ter pedidos suficientes ou o bar fechar
             wait(1000); 
             if (!estaAberto() && filaPedidos.isEmpty()) break;
         }
         
         List<Integer> pedidos = new ArrayList<>();
         int coletar = Math.min(filaPedidos.size(), capacidadeGarcom);
         for (int i = 0; i < coletar; i++) {
             pedidos.add(filaPedidos.remove(0));
         }
         return pedidos;
     }
 
     public synchronized void incrementarRodada() {
         rodadaAtual++;
         if (rodadaAtual >= maxRodadas) {
             barAberto = false;
             System.out.println("!!! O BAR ESTA FECHANDO !!!");
             notifyAll(); // Acorda todos para encerrarem
         }
     }
 }
 
 class Fregues extends Thread {
     private final int id;
     private final Bar bar;
 
     public Fregues(int id, Bar bar) {
         this.id = id;
         this.bar = bar;
     }
 
     public void run() {
         try {
             while (bar.estaAberto()) {
                 bar.fazerPedido(id);
                 // Simula espera e consumo
                 Thread.sleep(new Random().nextInt(2000) + 1000);
             }
         } catch (InterruptedException e) {
             System.out.println("Cliente " + id + " saiu.");
         }
     }
 }
 
 class Garcom extends Thread {
     private final int id;
     private final Bar bar;
 
     public Garcom(int id, Bar bar) {
         this.id = id;
         this.bar = bar;
     }
 
     public void run() {
         try {
             while (bar.estaAberto()) {
                 List<Integer> pedidos = bar.coletarPedidos();
                 if (!pedidos.isEmpty()) {
                     System.out.println("Garcom " + id + " levando " + pedidos.size() + " pedidos para a copa.");
                     Thread.sleep(1500); // Tempo na copa
                     System.out.println("Garcom " + id + " entregando pedidos: " + pedidos);
                     bar.incrementarRodada();
                 }
             }
         } catch (InterruptedException e) {
             System.out.println("Garcom " + id + " encerrou turno.");
         }
     }
 }
