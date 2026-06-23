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
        if (msg != null) {
            System.out.println("[CLIENTE EVENTO] Recebeu mensagem de " + msg.getRemetente() + " para " + msg.getDestinatario() + ": \"" + msg.getTexto() + "\"");
        }
        if (ui != null) {
            ui.exibirMensagem(msg);
        }
    }

    @Override
    public void receberArquivo(ArquivoInfo arquivo) throws RemoteException {
        if (arquivo != null) {
            System.out.println("[CLIENTE EVENTO] Recebeu arquivo '" + arquivo.getNomeArquivo() + "' de " + arquivo.getRemetente());
        }
        if (ui != null) {
            ui.receberEProcessarArquivo(arquivo);
        }
    }

    @Override
    public void atualizarListaUsuarios(List<Usuario> usuarios) throws RemoteException {
        System.out.println("[CLIENTE EVENTO] Atualizou lista de usuários logados. Total online: " + (usuarios != null ? usuarios.size() : 0));
        if (ui != null) {
            ui.atualizarUsuarios(usuarios);
        }
    }

    @Override
    public void atualizarListaGrupos(List<Grupo> grupos) throws RemoteException {
        System.out.println("[CLIENTE EVENTO] Atualizou lista de grupos. Total: " + (grupos != null ? grupos.size() : 0));
        if (ui != null) {
            ui.atualizarGrupos(grupos);
        }
    }

    @Override
    public void notificar(String notificacao) throws RemoteException {
        System.out.println("[CLIENTE EVENTO] Notificação recebida do servidor: \"" + notificacao + "\"");
        if (ui != null) {
            ui.exibirNotificacao(notificacao);
        }
    }

    @Override
    public void pedirPermissaoGrupo(String nomeGrupo, String solicitante) throws RemoteException {
        System.out.println("[CLIENTE EVENTO] Solicitação de aprovação: usuário '" + solicitante + "' quer entrar no grupo '" + nomeGrupo + "'");
        if (ui != null) {
            ui.lidarComPedidoGrupo(nomeGrupo, solicitante);
        }
    }

    @Override
    public void serBanidoAplicacao() throws RemoteException {
        System.out.println("[CLIENTE EVENTO] Você foi BANIDO globalmente pelo administrador.");
        if (ui != null) {
            ui.encerrarPorBanimento();
        }
    }
}
