package fz.exemple.aep.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class UsuarioCreateRequest {

    @NotBlank @Size(max = 100)
    private String nome;

    @NotBlank @Email
    private String email;

    private List<EnderecoDTO> enderecos;

    public UsuarioCreateRequest() {}

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<EnderecoDTO> getEnderecos() { return enderecos; }
    public void setEnderecos(List<EnderecoDTO> enderecos) { this.enderecos = enderecos; }
}