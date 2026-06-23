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

    // Simulacao de banco de dados (users / senhas-hashes)
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
        System.out.println("[REGISTRO] Tentativa de registrar o usuário: " + username);
        if (registeredUsers.containsKey(username)) {
            System.out.println("[REGISTRO] Falhou. Usuário " + username + " já existe.");
            return false;
        }
        registeredUsers.put(username, passwordHash);
        System.out.println("[REGISTRO] Usuário " + username + " registrado com sucesso.");
        return true;
    }

    @Override
    public synchronized Usuario login(String username, String passwordHash, IWhatsUTClient clientInterface) throws RemoteException {
        System.out.println("[LOGIN] Tentativa de login do usuário: " + username);
        if (registeredUsers.containsKey(username) && registeredUsers.get(username).equals(passwordHash)) {
            if (activeClients.containsKey(username)) {
                System.out.println("[LOGIN] Falhou. Usuário " + username + " já logado em outro local.");
                throw new RemoteException("Usuario ja esta logado noutro local!");
            }
            activeClients.put(username, clientInterface);
            Usuario u = new Usuario(username);
            System.out.println("[LOGIN] Login bem-sucedido para: " + username);
            broadcastUsuarios();
            broadcastGrupos();
            return u;
        }
        System.out.println("[LOGIN] Falhou. Credenciais incorretas para: " + username);
        return null;
    }

    @Override
    public synchronized void logout(String username) throws RemoteException {
        System.out.println("[LOGOUT] Usuário " + username + " desconectou.");
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
        System.out.println("[GRUPO] Tentativa de criar grupo '" + nomeGrupo + "' por " + adminUsername);
        if (grupos.containsKey(nomeGrupo)) {
            System.out.println("[GRUPO] Falhou. Grupo '" + nomeGrupo + "' já existe.");
            return false;
        }
        Grupo novo = new Grupo(nomeGrupo, adminUsername);
        grupos.put(nomeGrupo, novo);
        System.out.println("[GRUPO] Grupo '" + nomeGrupo + "' criado com sucesso por " + adminUsername);
        broadcastGrupos();
        return true;
    }

    @Override
    public synchronized void pedirEntradaGrupo(String nomeGrupo, String username) throws RemoteException {
        System.out.println("[GRUPO] Usuário '" + username + "' pediu entrada no grupo '" + nomeGrupo + "'");
        Grupo grupo = grupos.get(nomeGrupo);
        if (grupo != null) {
            IWhatsUTClient adminClient = activeClients.get(grupo.getAdminUsername());
            if (adminClient != null) {
                adminClient.pedirPermissaoGrupo(nomeGrupo, username);
            } else {
                System.out.println("[GRUPO] Entrada falhou: administrador " + grupo.getAdminUsername() + " está offline.");
                throw new RemoteException("Admin do grupo nao esta online para aprovar.");
            }
        }
    }

    @Override
    public synchronized void responderEntradaGrupo(String nomeGrupo, String username, boolean aprovado) throws RemoteException {
        System.out.println("[GRUPO] Resposta à solicitação de '" + username + "' para entrar no grupo '" + nomeGrupo + "': Aprovado = " + aprovado);
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
        System.out.println("[GRUPO] Usuário '" + username + "' saiu do grupo '" + nomeGrupo + "'");
        Grupo g = grupos.get(nomeGrupo);
        if (g != null) {
            g.removerMembro(username);
            if (g.getAdminUsername().equals(username)) {
                // Admin saiu. Escolher novo admin ou deletar
                if (g.getMembros().isEmpty()) {
                    System.out.println("[GRUPO] Grupo '" + nomeGrupo + "' foi excluído pois o último membro/admin saiu.");
                    grupos.remove(nomeGrupo);
                } else {
                    String novoAdmin = g.getMembros().get(0);
                    g.setAdminUsername(novoAdmin);
                    System.out.println("[GRUPO] Novo admin para '" + nomeGrupo + "': " + novoAdmin);
                }
            }
            broadcastGrupos();
        }
    }

    @Override
    public void enviarMensagemPrivada(String de, String para, String texto) throws RemoteException {
        System.out.println("[CHAT PRIVADO] De " + de + " para " + para + ": \"" + texto + "\"");
        IWhatsUTClient client = activeClients.get(para);
        if (client != null) {
            client.receberMensagem(new Mensagem(de, para, texto, false));
        } else {
            System.out.println("[CHAT PRIVADO] Falhou. " + para + " está offline.");
            throw new RemoteException("Usuario destino nao esta logado.");
        }
    }

    @Override
    public void enviarMensagemGrupo(String de, String grupo, String texto) throws RemoteException {
        System.out.println("[CHAT GRUPO] No grupo '" + grupo + "', de " + de + ": \"" + texto + "\"");
        Grupo g = grupos.get(grupo);
        if (g != null) {
            if (!g.getMembros().contains(de)) {
                System.out.println("[CHAT GRUPO] Falhou. " + de + " não é membro do grupo '" + grupo + "'.");
                throw new RemoteException("Voce nao eh membro deste grupo!");
            }
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
        String nomeArq = (arquivo != null) ? arquivo.getNomeArquivo() : "null";
        Grupo g = grupos.get(para);
        if (g != null) {
            System.out.println("[ARQUIVO GRUPO] Envio de '" + nomeArq + "' de " + de + " para grupo '" + para + "'");
            if (!g.getMembros().contains(de)) {
                System.out.println("[ARQUIVO GRUPO] Falhou. " + de + " não é membro do grupo '" + para + "'.");
                throw new RemoteException("Voce nao eh membro deste grupo!");
            }
            for (String membro : g.getMembros()) {
                if (!membro.equals(de)) {
                    IWhatsUTClient c = activeClients.get(membro);
                    if (c != null) {
                        c.receberArquivo(arquivo);
                    }
                }
            }
        } else {
            System.out.println("[ARQUIVO PRIVADO] Envio de '" + nomeArq + "' de " + de + " para " + para);
            IWhatsUTClient client = activeClients.get(para);
            if (client != null) {
                client.receberArquivo(arquivo);
            } else {
                System.out.println("[ARQUIVO PRIVADO] Falhou. " + para + " está offline.");
                throw new RemoteException("Usuario destino nao esta logado para receber arquivo.");
            }
        }
    }

    @Override
    public synchronized void banirDoGrupo(String adminUsername, String nomeGrupo, String userBanido) throws RemoteException {
        System.out.println("[GRUPO BAN] Administrador '" + adminUsername + "' baniu '" + userBanido + "' do grupo '" + nomeGrupo + "'");
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
        System.out.println("[APP BAN] Usuário '" + username + "' foi banido globalmente da aplicação.");
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
