package fz.exemple.aep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public class DoacaoUpdateRequest {

    @NotBlank(message = "selecione o doador")
    private String usuarioId;

    @NotBlank(message = "não pode ficar em branco")
    private String item;

    @Positive(message = "deve ser maior que zero")
    private int quantidade;

    private LocalDate dataDoacao;

    public DoacaoUpdateRequest() {}

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public LocalDate getDataDoacao() { return dataDoacao; }
    public void setDataDoacao(LocalDate dataDoacao) { this.dataDoacao = dataDoacao; }
}
