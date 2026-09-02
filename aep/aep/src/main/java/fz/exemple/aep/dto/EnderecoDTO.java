package fz.exemple.aep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnderecoDTO {

    @NotBlank @Size(max = 100)
    private String rua;

    @NotBlank @Size(max = 50)
    private String cidade;

    @NotBlank @Size(max = 2)
    private String estado;
}