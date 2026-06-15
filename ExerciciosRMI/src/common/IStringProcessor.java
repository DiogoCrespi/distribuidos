package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IStringProcessor extends Remote {
    
    // P: verifica se S é um palíndromo; retorna true ou false.
    boolean verificaPalindromo(String s) throws RemoteException;
    
    // I: gera uma nova string na qual os caracteres de S aparecem na ordem inversa.
    String inverteString(String s) throws RemoteException;
    
    // +: gera uma nova string na qual as letras minúsculas presentes em S aparecem em maiúsculas.
    String maiusculas(String s) throws RemoteException;
    
    // -: gera uma nova string na qual as letras maiúsculas presentes em S aparecem em minúsculas.
    String minusculas(String s) throws RemoteException;
    
    // V: descobre a quantidade de vogais presentes em S.
    int contaVogais(String s) throws RemoteException;
    
    // C: descobre a quantidade de consoantes presentes em S.
    int contaConsoantes(String s) throws RemoteException;
    
    // A: gera uma nova string na qual constam somente as vogais presentes em S.
    String apenasVogais(String s) throws RemoteException;
    
    // Z: gera uma nova string na qual constam somente as consoantes presentes em S.
    String apenasConsoantes(String s) throws RemoteException;
    
    // W: descobre o local em S onde conste, pela primeira vez, um certo caractere fornecido como parâmetro.
    int encontraCaractere(String s, char c) throws RemoteException;
    
    // F: descobre o local em S onde se inicie, pela primeira vez, uma certa substring fornecida como parâmetro.
    int encontraSubstring(String s, String sub) throws RemoteException;
}
