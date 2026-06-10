package classicos;

import java.util.concurrent.Semaphore;
import java.util.LinkedList;
import java.util.Queue;

public class ProdutorConsumidorSemaforo {
    private static final int TAMANHO_BUFFER = 5;
    private static final Queue<Integer> buffer = new LinkedList<>();
    
    private static final Semaphore semVazio = new Semaphore(TAMANHO_BUFFER);
    private static final Semaphore semCheio = new Semaphore(0);
    private static final Semaphore mutex = new Semaphore(1);

    public static void main(String[] args) {
        new Thread(new Produtor(), "Produtor").start();
        new Thread(new Consumidor(), "Consumidor").start();
    }

    static class Produtor implements Runnable {
        public void run() {
            try {
                int item = 0;
                while (true) {
                    semVazio.acquire();//espera
                    mutex.acquire();//tranca do buffer.
                    buffer.add(++item);
                    System.out.println("Produtor inseriu: " + item);
                    mutex.release();//libera o buffer.
                    semCheio.release();//notifyAll
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {}
        }
    }

    static class Consumidor implements Runnable {
        public void run() {
            try {
                while (true) {
                    semCheio.acquire();
                    mutex.acquire();//tranca do buffer.
                    int item = buffer.poll();
                    System.out.println("Consumidor removeu: " + item);
                    mutex.release();//libera o buffer.
                    semVazio.release();
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {}
        }
    }
}
