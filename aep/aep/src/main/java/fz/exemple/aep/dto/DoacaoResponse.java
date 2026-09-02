package fz.exemple.aep.dto;

import java.time.LocalDate;

public class DoacaoResponse {

    private String id;
    private String usuarioId;
    private String item;
    private int quantidade;
    private LocalDate dataDoacao;

    public DoacaoResponse() {}

    public DoacaoResponse(String id, String usuarioId, String item, int quantidade, LocalDate dataDoacao) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.item = item;
        this.quantidade = quantidade;
        this.dataDoacao = dataDoacao;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public LocalDate getDataDoacao() { return dataDoacao; }
    public void setDataDoacao(LocalDate dataDoacao) { this.dataDoacao = dataDoacao; }
}