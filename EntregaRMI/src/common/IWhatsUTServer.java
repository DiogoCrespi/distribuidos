package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IWhatsUTServer extends Remote {
    // 1. Auth
    boolean registrarUsuario(String username, String passwordHash) throws RemoteException;
    Usuario login(String username, String passwordHash, IWhatsUTClient clientInterface) throws RemoteException;
    void logout(String username) throws RemoteException;
    
    // 2. Usuarios
    List<Usuario> getUsuariosLogados() throws RemoteException;
    
    // 3. Grupos
    List<Grupo> getGrupos() throws RemoteException;
    boolean criarGrupo(String nomeGrupo, String adminUsername) throws RemoteException;
    void pedirEntradaGrupo(String nomeGrupo, String username) throws RemoteException;
    void responderEntradaGrupo(String nomeGrupo, String username, boolean aprovado) throws RemoteException;
    void sairDoGrupo(String nomeGrupo, String username) throws RemoteException;

    // 4. Chat
    void enviarMensagemPrivada(String de, String para, String texto) throws RemoteException;
    void enviarMensagemGrupo(String de, String grupo, String texto) throws RemoteException;
    
    // 5. Transferencia de Arquivos
    void enviarArquivo(String de, String para, ArquivoInfo arquivo) throws RemoteException;
    
    // 6. Gerencia de Grupos e Sistema
    void banirDoGrupo(String adminUsername, String nomeGrupo, String userBanido) throws RemoteException;
}
