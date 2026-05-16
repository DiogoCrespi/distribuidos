package filosofos;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JantarFilosofos {
    public static void main(String[] args) {
        int n = 5;
        Filosofo[] filosofos = new Filosofo[n];
        Lock[] hashis = new ReentrantLock[n];

        for (int i = 0; i < n; i++) {
            hashis[i] = new ReentrantLock();
        }

        for (int i = 0; i < n; i++) {
            Lock hachisEsquerdo = hashis[i];
            Lock hachisDireito = hashis[(i + 1) % n];

            // Para evitar deadlock: o ultimo filosofo inverte a ordem de pegar os hashis
            if (i == n - 1) {
                filosofos[i] = new Filosofo(i, hachisDireito, hachisEsquerdo);
            } else {
                filosofos[i] = new Filosofo(i, hachisEsquerdo, hachisDireito);
            }
            new Thread(filosofos[i], "Filosofo-" + i).start();
        }
    }
}

class Filosofo implements Runnable {
    private final int id;
    private final Lock hachiEsquerdo;
    private final Lock hachiDireito;

    public Filosofo(int id, Lock esq, Lock dir) {
        this.id = id;
        this.hachiEsquerdo = esq;
        this.hachiDireito = dir;
    }

    private void realizarAcao(String acao) throws InterruptedException {
        System.out.println("Filosofo " + id + ": " + acao);
        Thread.sleep((long) (Math.random() * 2000));
    }

    public void run() {
        try {
            while (true) {
                realizarAcao("Meditando...");
                hachiEsquerdo.lock();
                try {
                    realizarAcao("Pegou hachi esquerdo. Tentando o direito...");
                    hachiDireito.lock();
                    try {
                        realizarAcao("Comendo arroz...");
                    } finally {
                        hachiDireito.unlock();
                    }
                } finally {
                    hachiEsquerdo.unlock();
                }
            }
        } catch (InterruptedException e) {}
    }
}
