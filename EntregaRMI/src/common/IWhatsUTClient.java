package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IWhatsUTClient extends Remote {
    void receberMensagem(Mensagem msg) throws RemoteException;
    void receberArquivo(ArquivoInfo arquivo) throws RemoteException;
    void atualizarListaUsuarios(List<Usuario> usuarios) throws RemoteException;
    void atualizarListaGrupos(List<Grupo> grupos) throws RemoteException;
    void notificar(String notificacao) throws RemoteException;
    void pedirPermissaoGrupo(String nomeGrupo, String solicitante) throws RemoteException;
    void serBanidoAplicacao() throws RemoteException; // servidor kika o usuario da aplicacao
}
