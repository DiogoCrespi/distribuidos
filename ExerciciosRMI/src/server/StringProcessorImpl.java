package server;

import common.IStringProcessor;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

public class StringProcessorImpl extends UnicastRemoteObject implements IStringProcessor {

    protected StringProcessorImpl() throws RemoteException {
        super();
    }

    @Override
    public boolean verificaPalindromo(String s) throws RemoteException {
        if (s == null) return false;
        String clean = s.replaceAll("\\s+", "").toLowerCase();
        String rev = new StringBuilder(clean).reverse().toString();
        return clean.equals(rev);
    }

    @Override
    public String inverteString(String s) throws RemoteException {
        if (s == null) return null;
        return new StringBuilder(s).reverse().toString();
    }

    @Override
    public String maiusculas(String s) throws RemoteException {
        if (s == null) return null;
        return s.toUpperCase();
    }

    @Override
    public String minusculas(String s) throws RemoteException {
        if (s == null) return null;
        return s.toLowerCase();
    }

    @Override
    public int contaVogais(String s) throws RemoteException {
        if (s == null) return 0;
        int count = 0;
        String lower = s.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') {
                count++;
            }
        }
        return count;
    }

    @Override
    public int contaConsoantes(String s) throws RemoteException {
        if (s == null) return 0;
        int count = 0;
        String lower = s.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetter(c) && !(c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u')) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String apenasVogais(String s) throws RemoteException {
        if (s == null) return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char lower = Character.toLowerCase(c);
            if (lower == 'a' || lower == 'e' || lower == 'i' || lower == 'o' || lower == 'u') {
                result.append(c);
            }
        }
        return result.toString();
    }

    @Override
    public String apenasConsoantes(String s) throws RemoteException {
        if (s == null) return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char lower = Character.toLowerCase(c);
            if (Character.isLetter(c) && !(lower == 'a' || lower == 'e' || lower == 'i' || lower == 'o' || lower == 'u')) {
                result.append(c);
            }
        }
        return result.toString();
    }

    @Override
    public int encontraCaractere(String s, char c) throws RemoteException {
        if (s == null) return -1;
        return s.indexOf(c);
    }

    @Override
    public int encontraSubstring(String s, String sub) throws RemoteException {
        if (s == null || sub == null) return -1;
        return s.indexOf(sub);
    }
}
