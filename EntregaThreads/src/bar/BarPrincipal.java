package bar;

import java.util.*;

public class BarPrincipal {
    public static void main(String[] args) {
        // Parametros solicitados com valores padrao
        int nClientes = 10;
        int xGarcons = 3;
        int cCapacidade = 3;
        int nRodadas = 5;

        // Leitura dinâmica dos parametros passados por argumento no console
        if (args.length >= 4) {
            try {
                nClientes = Integer.parseInt(args[0]);
                xGarcons = Integer.parseInt(args[1]);
                cCapacidade = Integer.parseInt(args[2]);
                nRodadas = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                System.out.println("Erro ao converter argumentos. Usando os valores padrao.");
            }
        }

        System.out.println("--- Simulacao do Bar Iniciada ---");
        System.out.println("Clientes: " + nClientes + " | Garcons: " + xGarcons + " | Capacidade: " + cCapacidade);

        Bar bar = new Bar(nRodadas, cCapacidade, nClientes);

        // Inicia a thread do Bartender
        new Bartender(bar).start();

        // Inicia as threads dos Fregueses
        for (int i = 0; i < nClientes; i++) {
            new Fregues(i, bar).start();
        }

        // Inicia as threads dos Garçons
        for (int i = 0; i < xGarcons; i++) {
            new Garcom(i, bar).start();
        }
    }
}

class CopaJob {
    final int waiterId;
    final List<Integer> pedidos;
    boolean pronto = false;

    CopaJob(int waiterId, List<Integer> pedidos) {
        this.waiterId = waiterId;
        this.pedidos = pedidos;
    }
}

class Bar {
    private final int maxRodadas;
    private final int capacidadeGarcom;
    private final int totalClientes;
    private int rodadaAtual = 0;
    private boolean barAberto = true;

    // Listas e mapas de controle de concorrência com Monitor
    private final List<Integer> filaPedidos = new LinkedList<>();
    private final Set<Integer> pedidosEntregues = new HashSet<>();
    private final String[] estadoCliente;

    // Fila da copa para o Bartender
    private final List<CopaJob> copaQueue = new LinkedList<>();

    public Bar(int rodadas, int cap, int clientes) {
        this.maxRodadas = rodadas;
        this.capacidadeGarcom = cap;
        this.totalClientes = clientes;
        this.estadoCliente = new String[clientes];
        for (int i = 0; i < clientes; i++) {
            estadoCliente[i] = "PENSANDO";
        }
    }

    public synchronized boolean estaAberto() {
        return barAberto;
    }

    private boolean temClientesPensando() {
        for (String estado : estadoCliente) {
            if ("PENSANDO".equals(estado)) {
                return true;
            }
        }
        return false;
    }

    // Cliente faz o pedido e aguarda o garçom entregar
    public synchronized void fazerPedido(int clienteId) throws InterruptedException {
        pedidosEntregues.remove(clienteId);
        filaPedidos.add(clienteId);
        estadoCliente[clienteId] = "NA_FILA";
        System.out.println("Cliente " + clienteId + " fez um pedido.");
        notifyAll(); // Notifica garçons que há novos pedidos

        // Bloqueia até que o pedido seja entregue ou o bar feche
        while (!pedidosEntregues.contains(clienteId) && barAberto) {
            wait();
        }
    }

    // Garçom coleta pedidos respeitando o FIFO
    public synchronized List<Integer> coletarPedidos() throws InterruptedException {
        // Aguarda enquanto o bar estiver aberto e a fila de pedidos estiver vazia
        // ou enquanto a fila não atingir a capacidade máxima de atendimento do garçom,
        // desde que ainda existam clientes em estado de "PENSANDO" que possam fazer novos pedidos.
        while (barAberto && (filaPedidos.isEmpty() || (filaPedidos.size() < capacidadeGarcom && temClientesPensando()))) {
            wait();
        }

        List<Integer> pedidos = new ArrayList<>();
        if (filaPedidos.isEmpty()) {
            return pedidos;
        }

        int coletar = Math.min(filaPedidos.size(), capacidadeGarcom);
        for (int i = 0; i < coletar; i++) {
            int cId = filaPedidos.remove(0);
            pedidos.add(cId);
            estadoCliente[cId] = "ESPERANDO_ENTREGA";
        }
        notifyAll(); // Notifica mudança nos estados
        return pedidos;
    }

    // Garçom solicita a preparação dos pedidos ao Bartender na Copa e aguarda
    public synchronized void pedirCopa(int waiterId, List<Integer> pedidos) throws InterruptedException {
        CopaJob job = new CopaJob(waiterId, pedidos);
        copaQueue.add(job);
        notifyAll(); // Acorda o Bartender

        while (!job.pronto && barAberto) {
            wait();
        }
    }

    // Bartender busca pedidos para preparar na Copa
    public synchronized CopaJob pegarTrabalhoCopa() throws InterruptedException {
        while (copaQueue.isEmpty() && barAberto) {
            wait();
        }
        if (copaQueue.isEmpty() && !barAberto) {
            return null;
        }
        return copaQueue.remove(0);
    }

    public synchronized boolean temCopaPendente() {
        return !copaQueue.isEmpty();
    }

    // Bartender finaliza a preparação e acorda o garçom
    public synchronized void finalizarCopa(CopaJob job) {
        job.pronto = true;
        notifyAll();
    }

    // Garçom entrega os pedidos aos clientes
    public synchronized void entregarPedidos(List<Integer> pedidos) {
        for (int cId : pedidos) {
            pedidosEntregues.add(cId);
            estadoCliente[cId] = "PENSANDO"; // Retorna ao estado inicial para poder consumir/pensar
        }
        notifyAll(); // Acorda os clientes correspondentes que estavam esperando
    }

    public synchronized void incrementarRodada() {
        rodadaAtual++;
        if (rodadaAtual >= maxRodadas) {
            barAberto = false;
            System.out.println("!!! O BAR ESTA FECHANDO !!!");
            notifyAll(); // Acorda todas as threads para encerrarem a execução
        }
    }
}

class Fregues extends Thread {
    private final int id;
    private final Bar bar;

    public Fregues(int id, Bar bar) {
        this.id = id;
        this.bar = bar;
    }

    public void run() {
        try {
            while (bar.estaAberto()) {
                // Simula um atraso antes de fazer o pedido
                Thread.sleep(new Random().nextInt(2000) + 1000);

                if (!bar.estaAberto()) break;

                // Faz o pedido (bloqueante até a entrega)
                bar.fazerPedido(id);

                // Simula o tempo consumindo o pedido entregue
                Thread.sleep(new Random().nextInt(2000) + 1000);
            }
        } catch (InterruptedException e) {
            System.out.println("Cliente " + id + " saiu.");
        }
    }
}

class Garcom extends Thread {
    private final int id;
    private final Bar bar;

    public Garcom(int id, Bar bar) {
        this.id = id;
        this.bar = bar;
    }

    public void run() {
        try {
            while (bar.estaAberto()) {
                List<Integer> pedidos = bar.coletarPedidos();
                if (!pedidos.isEmpty()) {
                    // Log idêntico para o parser da Interface Visual
                    System.out.println("Garcom " + id + " levando " + pedidos.size() + " pedidos para a copa.");

                    // Envia para o Bartender na Copa e espera
                    bar.pedirCopa(id, pedidos);

                    // Log idêntico para o parser da Interface Visual
                    System.out.println("Garcom " + id + " entregando pedidos: " + pedidos);
                    bar.entregarPedidos(pedidos);
                    bar.incrementarRodada();
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Garcom " + id + " encerrou turno.");
        }
    }
}

class Bartender extends Thread {
    private final Bar bar;

    public Bartender(Bar bar) {
        this.bar = bar;
    }

    public void run() {
        try {
            while (bar.estaAberto() || bar.temCopaPendente()) {
                CopaJob job = bar.pegarTrabalhoCopa();
                if (job != null) {
                    // Mensagem de log ilustrando a briga/preparação pelo bartender na copa
                    System.out.println("Bartender processando " + job.pedidos.size() + " pedidos do Garcom " + job.waiterId + ".");
                    Thread.sleep(1000); // Simula preparação das bebidas
                    bar.finalizarCopa(job);
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Bartender encerrou turno.");
        }
    }
}
