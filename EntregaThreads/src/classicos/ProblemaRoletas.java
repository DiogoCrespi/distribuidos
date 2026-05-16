package classicos;

import java.util.concurrent.atomic.AtomicInteger;

public class ProblemaRoletas {
    private static AtomicInteger contadorGlobal = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        int nRoletas = 5;
        int girosPorRoleta = 1000;
        Thread[] threads = new Thread[nRoletas];

        for (int i = 0; i < nRoletas; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < girosPorRoleta; j++) {
                    contadorGlobal.incrementAndGet();
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) t.join();

        System.out.println("Total de pessoas que passaram pelas " + nRoletas + " roletas: " + contadorGlobal.get());
        System.out.println("Esperado: " + (nRoletas * girosPorRoleta));
    }
}
