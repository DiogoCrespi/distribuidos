package common;

import java.io.Serializable;

public class ArquivoInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String remetente;
    private String nomeArquivo;
    private byte[] dados;

    public ArquivoInfo(String remetente, String nomeArquivo, byte[] dados) {
        this.remetente = remetente;
        this.nomeArquivo = nomeArquivo;
        this.dados = dados;
    }

    public String getRemetente() { return remetente; }
    public String getNomeArquivo() { return nomeArquivo; }
    public byte[] getDados() { return dados; }
}
