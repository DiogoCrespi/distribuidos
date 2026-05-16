package classicos;

import java.util.LinkedList;
import java.util.Queue;

public class ProdutorConsumidorMonitor {
    public static void main(String[] args) {
        BufferMonitor buffer = new BufferMonitor(5);
        new Thread(new ProdutorM(buffer), "Produtor").start();
        new Thread(new ConsumidorM(buffer), "Consumidor").start();
    }
}

class BufferMonitor {
    private final int limite;
    private final Queue<Integer> fila = new LinkedList<>();

    public BufferMonitor(int l) { this.limite = l; }

    public synchronized void produzir(int item) throws InterruptedException {
        while (fila.size() == limite) {
            wait();
        }
        fila.add(item);
        System.out.println("Monitor: Produtor inseriu " + item);
        notifyAll();
    }

    public synchronized int consumir() throws InterruptedException {
        while (fila.isEmpty()) {
            wait();
        }
        int item = fila.poll();
        System.out.println("Monitor: Consumidor removeu " + item);
        notifyAll();
        return item;
    }
}

class ProdutorM implements Runnable {
    private BufferMonitor buffer;
    public ProdutorM(BufferMonitor b) { this.buffer = b; }
    public void run() {
        try {
            int i = 0;
            while (true) {
                buffer.produzir(++i);
                Thread.sleep(1200);
            }
        } catch (InterruptedException e) {}
    }
}

class ConsumidorM implements Runnable {
    private BufferMonitor buffer;
    public ConsumidorM(BufferMonitor b) { this.buffer = b; }
    public void run() {
        try {
            while (true) {
                buffer.consumir();
                Thread.sleep(1800);
            }
        } catch (InterruptedException e) {}
    }
}
