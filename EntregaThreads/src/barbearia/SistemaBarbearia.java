package barbearia;

import java.util.*;

public class SistemaBarbearia {
    public static void main(String[] args) {
        int nCadeiras = 4;
        Barbearia barbearia = new Barbearia(nCadeiras);
        
        new Thread(new Barbeiro(barbearia), "Barbeiro").start();
        
        String[] nomes = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
        
        // Gerador de clientes 
        for (int i = 0; i < nomes.length; i++) {
            new Thread(new Cliente(nomes[i], barbearia), "Cliente-" + nomes[i]).start();
            try { 
                Thread.sleep(new Random().nextInt(1500)); 
            } catch (InterruptedException e) {}
        }
    }
}

class Barbearia {
    private final int cadeirasEspera;
    private final List<String> filaClientes = new LinkedList<>();
    private boolean barbeiroOcupado = false;
    private boolean clientePronto = false;

    public Barbearia(int n) {
        this.cadeirasEspera = n;
    }

    public synchronized void entrarNaBarbearia(String nome) throws InterruptedException {
        System.out.println("Cliente " + nome + " chegou.");
        if (filaClientes.size() < cadeirasEspera) {
            filaClientes.add(nome);
            System.out.println("Cliente " + nome + " sentou na cadeira de espera. (Total: " + filaClientes.size() + ")");
            notifyAll(); // Acorda o barbeiro se estiver dormindo
            
            // Fica esperando até que o barbeiro esteja livre E seja o primeiro da fila (FIFO)
            while (barbeiroOcupado || !filaClientes.get(0).equals(nome)) {
                wait();
            }
            
            filaClientes.remove(0);
            barbeiroOcupado = true;
            clientePronto = true;
            System.out.println("Cliente " + nome + " esta cortando o cabelo.");
            notifyAll(); // Notifica o barbeiro de que o cliente sentou na cadeira de corte
            
            // Espera o barbeiro terminar de cortar o cabelo
            while (barbeiroOcupado) {
                wait();
            }
        } else {
            System.out.println("Barbearia lotada. Cliente " + nome + " foi embora.");
        }
    }

    public synchronized void cortarCabelo() throws InterruptedException {
        while (filaClientes.isEmpty() && !clientePronto) {
            System.out.println("Barbeiro esta dormindo... ZzzZz");
            wait();
        }
        clientePronto = false; // Consome o sinal
        System.out.println("Barbeiro acordou e comecou a trabalhar.");
    }

    public synchronized void finalizarCorte() {
        System.out.println("Barbeiro terminou o corte.");
        barbeiroOcupado = false;
        notifyAll(); // Avisa que o corte terminou e libera a cadeira
    }
}

class Barbeiro implements Runnable {
    private Barbearia barbearia;
    public Barbeiro(Barbearia b) { this.barbearia = b; }
    
    public void run() {
        try {
            while (true) {
                barbearia.cortarCabelo();
                // Tempo de corte aleatório (entre 1s e 3s)
                Thread.sleep(new Random().nextInt(2000) + 1000);
                barbearia.finalizarCorte();
            }
        } catch (InterruptedException e) {}
    }
}

class Cliente implements Runnable {
    private String nome;
    private Barbearia barbearia;
    public Cliente(String nome, Barbearia b) { this.nome = nome; this.barbearia = b; }
    
    public void run() {
        try {
            barbearia.entrarNaBarbearia(nome);
        } catch (InterruptedException e) {}
    }
}
