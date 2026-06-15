package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Grupo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String nome;
    private String adminUsername;
    private List<String> membros;

    public Grupo(String nome, String adminUsername) {
        this.nome = nome;
        this.adminUsername = adminUsername;
        this.membros = new ArrayList<>();
        this.membros.add(adminUsername); // O criador ja e membro
    }

    public String getNome() { return nome; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public List<String> getMembros() { return membros; }

    public void adicionarMembro(String membro) {
        if (!membros.contains(membro)) {
            membros.add(membro);
        }
    }

    public void removerMembro(String membro) {
        membros.remove(membro);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Grupo grupo = (Grupo) o;
        return Objects.equals(nome, grupo.nome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome);
    }

    @Override
    public String toString() {
        return nome + " (Admin: " + adminUsername + ")";
    }
}
