package classicos;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ContasBancarias {
    public static void main(String[] args) {
        Conta contaA = new Conta("Conta-A", 1000);
        Conta contaB = new Conta("Conta-B", 500);

        // Acao 1: Transferencia de A para B
        new Thread(() -> contaA.transferir(contaB, 200), "T1").start();
        
        // Acao 2: Deposito em A
        new Thread(() -> contaA.depositar(300), "T2").start();
        
        // Acao 3: Juros em B
        new Thread(() -> contaB.creditarJuros(0.05), "T3").start();
        
        // Acao 4: Saque em B
        new Thread(() -> contaB.sacar(100), "T4").start();
    }
}

class Conta {
    private String nome;
    private double saldo;
    private final Lock lock = new ReentrantLock();

    public Conta(String n, double s) { this.nome = n; this.saldo = s; }

    public void depositar(double valor) {
        lock.lock();
        try {
            saldo += valor;
            System.out.println(nome + ": Depositado " + valor + ". Novo saldo: " + saldo);
        } finally {
            lock.unlock();
        }
    }

    public void sacar(double valor) {
        lock.lock();
        try {
            if (saldo >= valor) {
                saldo -= valor;
                System.out.println(nome + ": Sacado " + valor + ". Novo saldo: " + saldo);
            } else {
                System.out.println(nome + ": Saldo insuficiente para saque de " + valor);
            }
        } finally {
            lock.unlock();
        }
    }

    public void creditarJuros(double taxa) {
        lock.lock();
        try {
            double juros = saldo * taxa;
            saldo += juros;
            System.out.println(nome + ": Juros de " + juros + " creditados. Novo saldo: " + saldo);
        } finally {
            lock.unlock();
        }
    }

    public void transferir(Conta destino, double valor) {
        // Para evitar deadlock, tranca em ordem (por nome ou id)
        Conta primeira = this.nome.compareTo(destino.nome) < 0 ? this : destino;
        Conta segunda = primeira == this ? destino : this;

        primeira.lock.lock();
        segunda.lock.lock();
        try {
            if (saldo >= valor) {
                this.saldo -= valor;
                destino.saldo += valor;
                System.out.println("Transferencia de " + valor + " de " + this.nome + " para " + destino.nome + " OK.");
            }
        } finally {
            segunda.lock.unlock();
            primeira.lock.unlock();
        }
    }
}
