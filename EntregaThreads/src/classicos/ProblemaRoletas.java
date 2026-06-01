package classicos;
 
 public class ProblemaRoletas {
     private static int contadorGlobal = 0;
     private static final Object lock = new Object();
 
     public static void main(String[] args) throws InterruptedException {
         int nRoletas = 5;
         if (args.length >= 1) {
             try {
                 nRoletas = Integer.parseInt(args[0]);
             } catch (NumberFormatException e) {
                 System.out.println("Erro ao converter argumento de roletas. Usando o valor padrao.");
             }
         }
         int girosPorRoleta = 1000;
         Thread[] threads = new Thread[nRoletas];
 
         for (int i = 0; i < nRoletas; i++) {
             threads[i] = new Thread(() -> {
                 for (int j = 0; j < girosPorRoleta; j++) {
                     synchronized (lock) {
                         contadorGlobal++;
                     }
                 }
             });
             threads[i].start();
         }
 
         for (Thread t : threads) t.join();
 
         System.out.println("Total de pessoas que passaram pelas " + nRoletas + " roletas: " + contadorGlobal);
         System.out.println("Esperado: " + (nRoletas * girosPorRoleta));
     }
 }
