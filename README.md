# Atividades Sistemas Distribuídos

Este documento contém todos os comandos necessários para compilar e executar as atividades de Sockets e Threads.

---

## 1. Atividades de Sockets

**Local da pasta:** `C:\Distribuidos\EntregaSockets`

### Compilação:

```bash
# Na pasta EntregaSockets
javac -d bin src/*/*.java
```

### Execução:

| Exercício             | Comando Servidor / Principal               | Comando Cliente                           |
| :--------------------- | :----------------------------------------- | :---------------------------------------- |
| **01. Fortune**  | `java -cp bin fortune.ServidorFortune`   | `java -cp bin fortune.ClienteFortune`   |
| **02. Inteiros** | `java -cp bin inteiros.ServidorInteiros` | `java -cp bin inteiros.ClienteInteiros` |
| **03. Forca**    | `java -cp bin forca.ServidorForca`       | `java -cp bin forca.ClienteForca`       |
| **04. Banco**    | `java -cp bin banco.ServidorBanco`       | `java -cp bin banco.ClienteBanco`       |
| **05. Lojas**    | `java -cp bin lojas.SistemaCentral`      | `java -cp bin lojas.FilialSimulador`    |
| **06. Arquivos** | `java -cp bin arquivos.ServidorArquivos` | `java -cp bin arquivos.ClienteArquivos` |
| **07. Chat**     | `java -cp bin chat.ChatMulticast`        | (Executar em múltiplas instâncias)      |

---

## 2. Atividades de Threads

**Local da pasta:** `C:\Distribuidos\EntregaThreads`

### Compilação:

```bash
# Na pasta EntregaThreads
javac -d bin src/*/*.java
```

### Execução:

| Exercício                     | Comando de Execução                                 |
| :----------------------------- | :---------------------------------------------------- |
| **01. Bar**              | `java -cp bin bar.BarPrincipal`                     |
| **02. Barbearia**        | `java -cp bin barbearia.SistemaBarbearia`           |
| **03. Filósofos**       | `java -cp bin filosofos.JantarFilosofos`            |
| **04a. Roletas**         | `java -cp bin classicos.ProblemaRoletas`            |
| **04b. Contas**          | `java -cp bin classicos.ContasBancarias`            |
| **04c. P/C (Semáforo)** | `java -cp bin classicos.ProdutorConsumidorSemaforo` |
| **04c. P/C (Monitor)**   | `java -cp bin classicos.ProdutorConsumidorMonitor`  |

---

## 3. Interface Visual (Web)

**Local da pasta:** `C:\Distribuidos\InterfaceVisual`

### Execução:

```bash
node server.js
```

### No navegador:

[http://localhost:3000](http://localhost:3000)

---

## 4. Projeto WhatsUT (RMI)

**Local da pasta:** `C:\Users\Admin\Desktop\com1034\distribuidos\EntregaRMI\src`

O WhatsUT é um sistema de chat distribuído com interface gráfica utilizando Java RMI e o padrão Callback.

### Compilação:

```bash
# Na pasta EntregaRMI\src
javac common/*.java server/*.java client/*.java
```

### Execução:

**1. Iniciar o Servidor (RMI Registry + UI de Administração):**
Em um terminal na pasta `EntregaRMI\src`, execute:

```bash
java -cp bin server.ServidorMain
```

**2. Iniciar os Clientes (Chat UI):**
Abra novos terminais na mesma pasta (`EntregaRMI\src`) e execute o comando abaixo para cada cliente que deseja conectar:

```bash
java -cp bin client.ClienteMain
```
