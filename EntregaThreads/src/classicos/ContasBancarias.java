package classicos;

import java.util.*;

public class ContasBancarias {
    public static void main(String[] args) {
        Conta contaA = new Conta("Conta-A", 1000);
        Conta contaB = new Conta("Conta-B", 500);

        System.out.println("--- Simulacao de Contas Bancarias Iniciada ---");
        System.out.println("Saldo Inicial - Conta-A: R$ 1000 | Conta-B: R$ 500");

        // Simula DDoS: 10 transferencias, 6 depositos, 9 saques ocorrendo ao mesmo tempo
        List<Thread> threads = new ArrayList<>();

        // 10 Transferências
        for (int i = 0; i < 10; i++) {
            final double valor = 50 + (i * 10);
            if (i % 2 == 0) {
                threads.add(new Thread(() -> contaA.transferir(contaB, valor), "Transf-A-B-" + i));
            } else {
                threads.add(new Thread(() -> contaB.transferir(contaA, valor), "Transf-B-A-" + i));
            }
        }

        // 6 Depósitos
        for (int i = 0; i < 6; i++) {
            final double valor = 100 + (i * 20);
            if (i % 2 == 0) {
                threads.add(new Thread(() -> contaA.depositar(valor), "Dep-A-" + i));
            } else {
                threads.add(new Thread(() -> contaB.depositar(valor), "Dep-B-" + i));
            }
        }

        // 9 Saques
        for (int i = 0; i < 9; i++) {
            final double valor = 30 + (i * 10);
            if (i % 2 == 0) {
                threads.add(new Thread(() -> contaA.sacar(valor), "Saq-A-" + i));
            } else {
                threads.add(new Thread(() -> contaB.sacar(valor), "Saq-B-" + i));
            }
        }

        // 1 Crédito de juros concorrente
        threads.add(new Thread(() -> contaB.creditarJuros(0.05), "Juros-B"));

        // Embaralha as threads para misturar a execução (DDoS real)
        Collections.shuffle(threads);

        for (Thread t : threads) {
            t.start();
        }

        // Aguarda todas concluírem
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {}
        }

        System.out.println("--- Simulacao Finalizada ---");
        System.out.println("Saldo Final - Conta-A: R$ " + contaA.getSaldo() + " | Conta-B: R$ " + contaB.getSaldo());
    }
}

class Conta {
    private final String nome;
    private double saldo;

    public Conta(String n, double s) { this.nome = n; this.saldo = s; }

    public synchronized double getSaldo() { return saldo; }

    public synchronized void depositar(double valor) {
        double saldoAnterior = saldo;
        saldo += valor;
        System.out.println(nome + ": Depositado " + valor + ". Saldo anterior: " + saldoAnterior + ". Novo saldo: " + saldo);
    }

    public synchronized void sacar(double valor) {
        double saldoAnterior = saldo;
        if (saldo >= valor) {
            saldo -= valor;
            System.out.println(nome + ": Sacado " + valor + ". Saldo anterior: " + saldoAnterior + ". Novo saldo: " + saldo);
        } else {
            System.out.println(nome + ": Saldo insuficiente para saque de " + valor + ". Saldo atual: " + saldo);
        }
    }

    public synchronized void creditarJuros(double taxa) {
        double saldoAnterior = saldo;
        double juros = saldo * taxa;
        saldo += juros;
        System.out.println(nome + ": Juros de " + juros + " creditados. Saldo anterior: " + saldoAnterior + ". Novo saldo: " + saldo);
    }

    public void transferir(Conta destino, double valor) {
        // Para evitar deadlock, tranca em ordem alfabética de nome
        Conta primeira = this.nome.compareTo(destino.nome) < 0 ? this : destino;
        Conta segunda = primeira == this ? destino : this;

        synchronized (primeira) {
            synchronized (segunda) {
                double saldoAnteriorOrigem = this.saldo;
                double saldoAnteriorDestino = destino.saldo;
                if (this.saldo >= valor) {
                    this.saldo -= valor;
                    destino.saldo += valor;
                    System.out.println("Transferencia de " + valor + " de " + this.nome + " para " + destino.nome + " OK. Saldo anterior " + this.nome + ": " + saldoAnteriorOrigem + " | Saldo anterior " + destino.nome + ": " + saldoAnteriorDestino + ". Novo saldo " + this.nome + ": " + this.saldo + " | Novo saldo " + destino.nome + ": " + destino.saldo);
                } else {
                    System.out.println("Transferencia de " + valor + " de " + this.nome + " para " + destino.nome + " falhou. Saldo insuficiente.");
                }
            }
        }
    }
}
