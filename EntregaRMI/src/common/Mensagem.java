package common;

import java.io.Serializable;
import java.util.Date;

public class Mensagem implements Serializable {
    private static final long serialVersionUID = 1L;
    private String remetente;
    private String destinatario; // Pode ser null se for broadcast/sistema ou o nome do grupo se for isGrupo
    private String texto;
    private boolean isGrupo;
    private Date timestamp;
    private ArquivoInfo arquivo;

    public Mensagem(String remetente, String destinatario, String texto, boolean isGrupo) {
        this(remetente, destinatario, texto, isGrupo, null);
    }

    public Mensagem(String remetente, String destinatario, String texto, boolean isGrupo, ArquivoInfo arquivo) {
        this.remetente = remetente;
        this.destinatario = destinatario;
        this.texto = texto;
        this.isGrupo = isGrupo;
        this.timestamp = new Date();
        this.arquivo = arquivo;
    }

    public String getRemetente() { return remetente; }
    public String getDestinatario() { return destinatario; }
    public String getTexto() { return texto; }
    public boolean isGrupo() { return isGrupo; }
    public Date getTimestamp() { return timestamp; }
    public ArquivoInfo getArquivo() { return arquivo; }

    @Override
    public String toString() {
        if (isGrupo) {
            return "[" + destinatario + "] " + remetente + ": " + (arquivo != null ? "[Arquivo: " + arquivo.getNomeArquivo() + "]" : texto);
        } else {
            return "(Privado) " + remetente + ": " + (arquivo != null ? "[Arquivo: " + arquivo.getNomeArquivo() + "]" : texto);
        }
    }
}
