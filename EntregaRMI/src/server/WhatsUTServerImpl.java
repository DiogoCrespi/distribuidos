package server;

import common.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WhatsUTServerImpl extends UnicastRemoteObject implements IWhatsUTServer {

    // Simulação de banco de dados (users / senhas-hashes)
    private Map<String, String> registeredUsers = new HashMap<>();
    
    // Usuarios logados no momento e seus stubs (callbacks)
    private Map<String, IWhatsUTClient> activeClients = new HashMap<>();
    
    // Grupos do sistema
    private Map<String, Grupo> grupos = new HashMap<>();

    public WhatsUTServerImpl() throws RemoteException {
        super();
    }

    private void broadcastUsuarios() {
        List<Usuario> list = getUsuariosLogados();
        for (IWhatsUTClient client : activeClients.values()) {
            try {
                client.atualizarListaUsuarios(list);
            } catch (RemoteException e) {
                // cliente desconectou de forma anormal, tratar dps
            }
        }
    }

    private void broadcastGrupos() {
        List<Grupo> list = getGrupos();
        for (IWhatsUTClient client : activeClients.values()) {
            try {
                client.atualizarListaGrupos(list);
            } catch (RemoteException e) {
                // ignorar
            }
        }
    }

    @Override
    public synchronized boolean registrarUsuario(String username, String passwordHash) throws RemoteException {
        if (registeredUsers.containsKey(username)) {
            return false;
        }
        registeredUsers.put(username, passwordHash);
        return true;
    }

    @Override
    public synchronized Usuario login(String username, String passwordHash, IWhatsUTClient clientInterface) throws RemoteException {
        if (registeredUsers.containsKey(username) && registeredUsers.get(username).equals(passwordHash)) {
            if (activeClients.containsKey(username)) {
                throw new RemoteException("Usuario ja esta logado noutro local!");
            }
            activeClients.put(username, clientInterface);
            Usuario u = new Usuario(username);
            broadcastUsuarios();
            broadcastGrupos();
            return u;
        }
        return null;
    }

    @Override
    public synchronized void logout(String username) throws RemoteException {
        activeClients.remove(username);
        broadcastUsuarios();
    }

    @Override
    public List<Usuario> getUsuariosLogados() {
        return activeClients.keySet().stream().map(Usuario::new).collect(Collectors.toList());
    }

    @Override
    public List<Grupo> getGrupos() {
        return new ArrayList<>(grupos.values());
    }

    @Override
    public synchronized boolean criarGrupo(String nomeGrupo, String adminUsername) throws RemoteException {
        if (grupos.containsKey(nomeGrupo)) {
            return false;
        }
        Grupo novo = new Grupo(nomeGrupo, adminUsername);
        grupos.put(nomeGrupo, novo);
        broadcastGrupos();
        return true;
    }

    @Override
    public synchronized void pedirEntradaGrupo(String nomeGrupo, String username) throws RemoteException {
        Grupo grupo = grupos.get(nomeGrupo);
        if (grupo != null) {
            IWhatsUTClient adminClient = activeClients.get(grupo.getAdminUsername());
            if (adminClient != null) {
                adminClient.pedirPermissaoGrupo(nomeGrupo, username);
            } else {
                throw new RemoteException("Admin do grupo nao esta online para aprovar.");
            }
        }
    }

    @Override
    public synchronized void responderEntradaGrupo(String nomeGrupo, String username, boolean aprovado) throws RemoteException {
        if (aprovado) {
            Grupo g = grupos.get(nomeGrupo);
            if (g != null) {
                g.adicionarMembro(username);
                IWhatsUTClient solicitante = activeClients.get(username);
                if (solicitante != null) {
                    solicitante.notificar("Sua entrada no grupo " + nomeGrupo + " foi aprovada!");
                }
                broadcastGrupos();
            }
        } else {
            IWhatsUTClient solicitante = activeClients.get(username);
            if (solicitante != null) {
                solicitante.notificar("Sua entrada no grupo " + nomeGrupo + " foi RECUSADA!");
            }
        }
    }

    @Override
    public synchronized void sairDoGrupo(String nomeGrupo, String username) throws RemoteException {
        Grupo g = grupos.get(nomeGrupo);
        if (g != null) {
            g.removerMembro(username);
            if (g.getAdminUsername().equals(username)) {
                // Admin saiu. Escolher novo admin ou deletar
                if (g.getMembros().isEmpty()) {
                    grupos.remove(nomeGrupo);
                } else {
                    g.setAdminUsername(g.getMembros().get(0));
                }
            }
            broadcastGrupos();
        }
    }

    @Override
    public void enviarMensagemPrivada(String de, String para, String texto) throws RemoteException {
        IWhatsUTClient client = activeClients.get(para);
        if (client != null) {
            client.receberMensagem(new Mensagem(de, para, texto, false));
        } else {
            throw new RemoteException("Usuario destino nao esta logado.");
        }
    }

    @Override
    public void enviarMensagemGrupo(String de, String grupo, String texto) throws RemoteException {
        Grupo g = grupos.get(grupo);
        if (g != null) {
            Mensagem msg = new Mensagem(de, grupo, texto, true);
            for (String membro : g.getMembros()) {
                if (!membro.equals(de)) { // Nao mandar para si mesmo? ou mandar pra ver que enviou, vamos mandar pra todos os outros
                    IWhatsUTClient c = activeClients.get(membro);
                    if (c != null) {
                        c.receberMensagem(msg);
                    }
                }
            }
        }
    }

    @Override
    public void enviarArquivo(String de, String para, ArquivoInfo arquivo) throws RemoteException {
        IWhatsUTClient client = activeClients.get(para);
        if (client != null) {
            client.receberArquivo(arquivo);
        } else {
            throw new RemoteException("Usuario destino nao esta logado para receber arquivo.");
        }
    }

    @Override
    public synchronized void banirDoGrupo(String adminUsername, String nomeGrupo, String userBanido) throws RemoteException {
        Grupo g = grupos.get(nomeGrupo);
        if (g != null && g.getAdminUsername().equals(adminUsername)) {
            g.removerMembro(userBanido);
            IWhatsUTClient c = activeClients.get(userBanido);
            if (c != null) {
                c.notificar("Voce foi banido do grupo " + nomeGrupo + "!");
            }
            broadcastGrupos();
        }
    }

    // Funcao extra para o ServerUI kikar do app geral
    public synchronized void banirAplicacao(String username) {
        registeredUsers.remove(username); // Banido msm
        IWhatsUTClient c = activeClients.remove(username);
        if (c != null) {
            try {
                c.serBanidoAplicacao();
            } catch (RemoteException e) {
                // ignorar se ja desconectou
            }
        }
        broadcastUsuarios();
    }
}
