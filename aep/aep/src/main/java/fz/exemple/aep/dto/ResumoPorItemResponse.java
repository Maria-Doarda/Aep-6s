package fz.exemple.aep.dto;

/**
 * Linha do resumo agrupado por item, produzida no proprio MongoDB
 * (aggregation pipeline com $group) em GET /api/doacoes/resumo/por-item.
 */
public class ResumoPorItemResponse {

    private String item;
    private int quantidadeTotal;
    private long totalDoacoes;

    public ResumoPorItemResponse() {}

    public ResumoPorItemResponse(String item, int quantidadeTotal, long totalDoacoes) {
        this.item = item;
        this.quantidadeTotal = quantidadeTotal;
        this.totalDoacoes = totalDoacoes;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getQuantidadeTotal() {
        return quantidadeTotal;
    }

    public void setQuantidadeTotal(int quantidadeTotal) {
        this.quantidadeTotal = quantidadeTotal;
    }

    public long getTotalDoacoes() {
        return totalDoacoes;
    }

    public void setTotalDoacoes(long totalDoacoes) {
        this.totalDoacoes = totalDoacoes;
    }
}
