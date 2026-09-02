package fz.exemple.aep.dto;

import java.util.List;

public class UsuarioResponse {

    private String id;
    private String nome;
    private String email;
    private List<EnderecoDTO> enderecos;

    public UsuarioResponse() {}

    public UsuarioResponse(String id, String nome, String email, List<EnderecoDTO> enderecos) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.enderecos = enderecos;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<EnderecoDTO> getEnderecos() { return enderecos; }
    public void setEnderecos(List<EnderecoDTO> enderecos) { this.enderecos = enderecos; }
}