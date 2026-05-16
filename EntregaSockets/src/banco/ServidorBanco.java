package banco;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServidorBanco {
    private static final int PORTA = 12348;
    private static Map<String, Double> contas = new ConcurrentHashMap<>();

    static {
        contas.put("123", 1000.0);
        contas.put("456", 500.0);
    }

    public static void main(String[] args) {
        System.out.println("Servidor Bancario iniciado na porta " + PORTA);
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                new ThreadCliente(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ThreadCliente extends Thread {
        private Socket socket;
        public ThreadCliente(Socket s) { this.socket = s; }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                
                out.println("BEM-VINDO AO BANCO UTFPR. DIGITE O NUMERO DA CONTA:");
                String contaInput = in.readLine();
                if (contaInput == null) return;
                String conta = contaInput.trim();
                if (!contas.containsKey(conta)) {
                    out.println("ERRO: Conta inexistente. Encerrando.");
                    return;
                }

                String cmd;
                while (true) {
                    out.println("MENU: 1.SALDO 2.DEPOSITO <valor> 3.SAQUE <valor> 4.SAIR");
                    cmd = in.readLine();
                    if (cmd == null || cmd.equalsIgnoreCase("4")) break;

                    String[] partes = cmd.split(" ");
                    String operacao = partes[0].toUpperCase();

                    switch (operacao) {
                        case "1":
                            out.println("SALDO ATUAL: " + contas.get(conta));
                            break;
                        case "2":
                            if (partes.length < 2) { out.println("ERRO: Informe o valor."); }
                            else {
                                double val = Double.parseDouble(partes[1]);
                                depositar(conta, val);
                                out.println("SUCESSO: Deposito realizado.");
                            }
                            break;
                        case "3":
                            if (partes.length < 2) { out.println("ERRO: Informe o valor."); }
                            else {
                                double val = Double.parseDouble(partes[1]);
                                if (sacar(conta, val)) out.println("SUCESSO: Saque realizado.");
                                else out.println("ERRO: Saldo insuficiente.");
                            }
                            break;
                        default:
                            out.println("ERRO: Operacao invalida.");
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro: " + e.getMessage());
            }
        }

        private synchronized void depositar(String conta, double valor) {
            contas.put(conta, contas.get(conta) + valor);
        }

        private synchronized boolean sacar(String conta, double valor) {
            double atual = contas.get(conta);
            if (atual >= valor) {
                contas.put(conta, atual - valor);
                return true;
            }
            return false;
        }
    }
}
