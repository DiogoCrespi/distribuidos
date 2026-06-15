package client;

import common.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class WhatsUTClientImpl extends UnicastRemoteObject implements IWhatsUTClient {

    private ClientUI ui;

    public WhatsUTClientImpl() throws RemoteException {
        super();
    }

    public void setUI(ClientUI ui) {
        this.ui = ui;
    }

    @Override
    public void receberMensagem(Mensagem msg) throws RemoteException {
        if (ui != null) {
            ui.exibirMensagem(msg);
        }
    }

    @Override
    public void receberArquivo(ArquivoInfo arquivo) throws RemoteException {
        if (ui != null) {
            ui.receberEProcessarArquivo(arquivo);
        }
    }

    @Override
    public void atualizarListaUsuarios(List<Usuario> usuarios) throws RemoteException {
        if (ui != null) {
            ui.atualizarUsuarios(usuarios);
        }
    }

    @Override
    public void atualizarListaGrupos(List<Grupo> grupos) throws RemoteException {
        if (ui != null) {
            ui.atualizarGrupos(grupos);
        }
    }

    @Override
    public void notificar(String notificacao) throws RemoteException {
        if (ui != null) {
            ui.exibirNotificacao(notificacao);
        }
    }

    @Override
    public void pedirPermissaoGrupo(String nomeGrupo, String solicitante) throws RemoteException {
        if (ui != null) {
            ui.lidarComPedidoGrupo(nomeGrupo, solicitante);
        }
    }

    @Override
    public void serBanidoAplicacao() throws RemoteException {
        if (ui != null) {
            ui.encerrarPorBanimento();
        }
    }
}
