package fz.exemple.aep.dto;

public class ResumoResponse {

    private long totalDoacoes;
    private int totalQuantidade;
    private long itensDistintos;

    public ResumoResponse(long totalDoacoes, int totalQuantidade, long itensDistintos) {
        this.totalDoacoes = totalDoacoes;
        this.totalQuantidade = totalQuantidade;
        this.itensDistintos = itensDistintos;
    }

    public long getTotalDoacoes() {
        return totalDoacoes;
    }

    public int getTotalQuantidade() {
        return totalQuantidade;
    }

    public long getItensDistintos() {
        return itensDistintos;
    }
}