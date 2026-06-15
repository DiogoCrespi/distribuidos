package client;

import common.IStringProcessor;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClienteMain {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java -cp bin client.ClienteMain <operacao> <string> [parametro_extra]");
            System.out.println("Operacoes: P, I, +, -, V, C, A, Z, W, F");
            return;
        }

        String operacao = args[0].toUpperCase();
        String s = args[1];

        try {
            // Conecta ao Registry local
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            // Busca a referência do objeto remoto
            IStringProcessor processador = (IStringProcessor) registry.lookup("StringProcessorService");

            switch (operacao) {
                case "P":
                    System.out.println("Resposta: " + processador.verificaPalindromo(s));
                    break;
                case "I":
                    System.out.println("Resposta: " + processador.inverteString(s));
                    break;
                case "+":
                    System.out.println("Resposta: " + processador.maiusculas(s));
                    break;
                case "-":
                    System.out.println("Resposta: " + processador.minusculas(s));
                    break;
                case "V":
                    System.out.println("Resposta: " + processador.contaVogais(s));
                    break;
                case "C":
                    System.out.println("Resposta: " + processador.contaConsoantes(s));
                    break;
                case "A":
                    System.out.println("Resposta: " + processador.apenasVogais(s));
                    break;
                case "Z":
                    System.out.println("Resposta: " + processador.apenasConsoantes(s));
                    break;
                case "W":
                    if (args.length < 3) {
                        System.out.println("Erro: A operacao W exige um caractere como terceiro parametro.");
                        return;
                    }
                    char c = args[2].charAt(0);
                    System.out.println("Resposta: " + processador.encontraCaractere(s, c));
                    break;
                case "F":
                    if (args.length < 3) {
                        System.out.println("Erro: A operacao F exige uma substring como terceiro parametro.");
                        return;
                    }
                    String sub = args[2];
                    System.out.println("Resposta: " + processador.encontraSubstring(s, sub));
                    break;
                default:
                    System.out.println("Operacao desconhecida: " + operacao);
            }
        } catch (Exception e) {
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
