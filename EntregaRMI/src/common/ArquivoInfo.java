package common;

import java.io.Serializable;

public class ArquivoInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String remetente;
    private String destinatario;
    private String nomeArquivo;
    private byte[] dados;
    private boolean isGrupo;

    public ArquivoInfo(String remetente, String destinatario, String nomeArquivo, byte[] dados, boolean isGrupo) {
        this.remetente = remetente;
        this.destinatario = destinatario;
        this.nomeArquivo = nomeArquivo;
        this.dados = dados;
        this.isGrupo = isGrupo;
    }

    public String getRemetente() { return remetente; }
    public String getDestinatario() { return destinatario; }
    public String getNomeArquivo() { return nomeArquivo; }
    public byte[] getDados() { return dados; }
    public boolean isGrupo() { return isGrupo; }
}
