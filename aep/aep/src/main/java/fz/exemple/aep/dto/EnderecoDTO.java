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

    @NotBlank(message = "não pode ficar em branco")
    @Size(max = 100, message = "máximo de 100 caracteres")
    private String rua;

    @NotBlank(message = "não pode ficar em branco")
    @Size(max = 50, message = "máximo de 50 caracteres")
    private String cidade;

    @NotBlank(message = "não pode ficar em branco")
    @Size(max = 2, message = "use a sigla do estado com 2 letras")
    private String estado;
}
