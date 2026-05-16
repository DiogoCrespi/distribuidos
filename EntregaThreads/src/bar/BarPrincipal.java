package bar;

import java.util.concurrent.*;
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
    
    // Fila de pedidos aguardando garçom
    private final BlockingQueue<Integer> filaPedidos = new LinkedBlockingQueue<>();
    
    public Bar(int rodadas, int cap, int clientes) {
        this.maxRodadas = rodadas;
        this.capacidadeGarcom = cap;
        this.totalClientes = clientes;
    }

    public synchronized boolean estaAberto() {
        return barAberto;
    }

    public void fazerPedido(int clienteId) throws InterruptedException {
        filaPedidos.put(clienteId);
        System.out.println("Cliente " + clienteId + " fez um pedido.");
    }

    public List<Integer> coletarPedidos() throws InterruptedException {
        List<Integer> pedidos = new ArrayList<>();
        while (pedidos.size() < capacidadeGarcom) {
            // Garcom espera ate ter pedidos ou o bar fechar
            Integer p = filaPedidos.poll(1, TimeUnit.SECONDS);
            if (p != null) {
                pedidos.add(p);
            } else if (!estaAberto() && filaPedidos.isEmpty()) {
                break;
            }
        }
        return pedidos;
    }

    public synchronized void incrementarRodada() {
        rodadaAtual++;
        if (rodadaAtual >= maxRodadas) {
            barAberto = false;
            System.out.println("!!! O BAR ESTA FECHANDO !!!");
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
