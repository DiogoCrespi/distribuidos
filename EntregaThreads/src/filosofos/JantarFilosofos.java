package filosofos;
 
 public class JantarFilosofos {
     public static void main(String[] args) {
         int n = 5;
         Filosofo[] filosofos = new Filosofo[n];
         Object[] hashis = new Object[n];
 
         for (int i = 0; i < n; i++) {
             hashis[i] = new Object();
         }
 
         for (int i = 0; i < n; i++) {
             Object hachisEsquerdo = hashis[i];
             Object hachisDireito = hashis[(i + 1) % n];
 
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
     private final Object hachiEsquerdo;
     private final Object hachiDireito;
 
     public Filosofo(int id, Object esq, Object dir) {
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
                 synchronized (hachiEsquerdo) {
                     realizarAcao("Pegou hachi esquerdo. Tentando o direito...");
                     synchronized (hachiDireito) {
                         realizarAcao("Comendo arroz...");
                     }
                 }
             }
         } catch (InterruptedException e) {}
     }
 }
