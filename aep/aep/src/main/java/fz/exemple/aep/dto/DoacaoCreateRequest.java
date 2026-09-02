package fz.exemple.aep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public class DoacaoCreateRequest {

    @NotBlank
    private String usuarioId;

    @NotBlank
    private String item;

    @Positive
    private int quantidade;

    private LocalDate dataDoacao;

    public DoacaoCreateRequest() {}

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public LocalDate getDataDoacao() { return dataDoacao; }
    public void setDataDoacao(LocalDate dataDoacao) { this.dataDoacao = dataDoacao; }
}