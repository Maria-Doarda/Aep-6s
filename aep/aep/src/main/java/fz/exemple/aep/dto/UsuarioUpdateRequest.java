package fz.exemple.aep.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class UsuarioUpdateRequest {

    @NotBlank(message = "não pode ficar em branco")
    @Size(max = 100, message = "máximo de 100 caracteres")
    private String nome;

    @NotBlank(message = "não pode ficar em branco")
    @Email(message = "deve ser um endereço de e-mail válido")
    private String email;

    @Valid
    private List<EnderecoDTO> enderecos;

    public UsuarioUpdateRequest() {}

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<EnderecoDTO> getEnderecos() { return enderecos; }
    public void setEnderecos(List<EnderecoDTO> enderecos) { this.enderecos = enderecos; }
}
