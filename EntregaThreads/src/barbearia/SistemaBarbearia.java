package barbearia;

import java.util.concurrent.*;

public class SistemaBarbearia {
    public static void main(String[] args) {
        int nCadeiras = 3;
        Barbearia barbearia = new Barbearia(nCadeiras);
        
        new Thread(new Barbeiro(barbearia), "Barbeiro").start();
        
        // Gerador de clientes
        for (int i = 1; i <= 10; i++) {
            new Thread(new Cliente(i, barbearia), "Cliente-" + i).start();
            try { Thread.sleep(new java.util.Random().nextInt(2000)); } catch (InterruptedException e) {}
        }
    }
}

class Barbearia {
    private final int cadeirasEspera;
    private int clientesAguardando = 0;
    private boolean barbeiroOcupado = false;

    public Barbearia(int n) {
        this.cadeirasEspera = n;
    }

    public synchronized void entrarNaBarbearia(int id) throws InterruptedException {
        System.out.println("Cliente " + id + " chegou.");
        if (clientesAguardando < cadeirasEspera) {
            clientesAguardando++;
            System.out.println("Cliente " + id + " sentou na cadeira de espera. (Total: " + clientesAguardando + ")");
            notify(); // Acorda o barbeiro se ele estiver dormindo
            
            while (barbeiroOcupado) {
                wait(); // Espera sua vez
            }
            
            clientesAguardando--;
            barbeiroOcupado = true;
            System.out.println("Cliente " + id + " esta cortando o cabelo.");
        } else {
            System.out.println("Barbearia lotada. Cliente " + id + " foi embora.");
        }
    }

    public synchronized void cortarCabelo() throws InterruptedException {
        while (clientesAguardando == 0) {
            System.out.println("Barbeiro esta dormindo... ZzzZz");
            wait();
        }
        System.out.println("Barbeiro acordou e comecou a trabalhar.");
    }

    public synchronized void finalizarCorte() {
        System.out.println("Barbeiro terminou o corte.");
        barbeiroOcupado = false;
        notifyAll(); // Avisa que a cadeira do barbeiro liberou
    }
}

class Barbeiro implements Runnable {
    private Barbearia barbearia;
    public Barbeiro(Barbearia b) { this.barbearia = b; }
    
    public void run() {
        try {
            while (true) {
                barbearia.cortarCabelo();
                Thread.sleep(3000); // Simulando tempo de corte
                barbearia.finalizarCorte();
            }
        } catch (InterruptedException e) {}
    }
}

class Cliente implements Runnable {
    private int id;
    private Barbearia barbearia;
    public Cliente(int id, Barbearia b) { this.id = id; this.barbearia = b; }
    
    public void run() {
        try {
            barbearia.entrarNaBarbearia(id);
        } catch (InterruptedException e) {}
    }
}
