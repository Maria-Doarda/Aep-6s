package fz.exemple.aep.mapper;

import fz.exemple.aep.dto.EnderecoDTO;
import fz.exemple.aep.dto.UsuarioCreateRequest;
import fz.exemple.aep.dto.UsuarioResponse;
import fz.exemple.aep.dto.UsuarioUpdateRequest;
import fz.exemple.aep.models.Usuario;

import java.util.ArrayList;
import java.util.List;

public final class UsuarioMapper {

    private UsuarioMapper() {}

    public static Usuario toEntity(UsuarioCreateRequest request) {
        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        if (request.getEnderecos() != null) {
            usuario.setEnderecos(toEnderecoEntities(request.getEnderecos()));
        }
        return usuario;
    }

    public static Usuario toEntity(UsuarioUpdateRequest request, Usuario existing) {
        existing.setNome(request.getNome());
        existing.setEmail(request.getEmail());
        if (request.getEnderecos() != null) {
            existing.setEnderecos(toEnderecoEntities(request.getEnderecos()));
        } else {
            existing.setEnderecos(new ArrayList<>());
        }
        return existing;
    }

    public static UsuarioResponse toResponse(Usuario usuario) {
        List<EnderecoDTO> dtos = new ArrayList<>();
        if (usuario.getEnderecos() != null) {
            dtos = toEnderecoDTOs(usuario.getEnderecos());
        }
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), dtos);
    }

    public static List<UsuarioResponse> toResponseList(List<Usuario> usuarioList) {
        List<UsuarioResponse> responses = new ArrayList<>();
        for (Usuario u : usuarioList) {
            responses.add(toResponse(u));
        }
        return responses;
    }

    public static EnderecoDTO toEnderecoDTO(fz.exemple.aep.models.Endereco endereco) {
        if (endereco == null) return null;
        EnderecoDTO dto = new EnderecoDTO();
        dto.setRua(endereco.getRua());
        dto.setCidade(endereco.getCidade());
        dto.setEstado(endereco.getEstado());
        return dto;
    }

    private static List<EnderecoDTO> toEnderecoDTOs(List<fz.exemple.aep.models.Endereco> list) {
        List<EnderecoDTO> dtos = new ArrayList<>();
        for (fz.exemple.aep.models.Endereco e : list) {
            dtos.add(toEnderecoDTO(e));
        }
        return dtos;
    }

    private static List<fz.exemple.aep.models.Endereco> toEnderecoEntities(List<EnderecoDTO> dtos) {
        List<fz.exemple.aep.models.Endereco> list = new ArrayList<>();
        for (EnderecoDTO d : dtos) {
            fz.exemple.aep.models.Endereco e = new fz.exemple.aep.models.Endereco();
            e.setRua(d.getRua());
            e.setCidade(d.getCidade());
            e.setEstado(d.getEstado());
            list.add(e);
        }
        return list;
    }
}