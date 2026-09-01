package fz.exemple.aep.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "doacoes")
public class Doacao {

    @Id
    private String id;

    private String usuarioId;
    private String item;
    private int quantidade;
    private LocalDate data_doacao;

    public Doacao(){}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public LocalDate getData_doacao() {
        return data_doacao;
    }

    public void setData_doacao(LocalDate data_doacao) {
        this.data_doacao = data_doacao;
    }
}
