package model;

public class ProfitBySaleDTO {
    private String transactionId;
    private double revenue;
    private double cost;
    private double profit;

    public ProfitBySaleDTO(String transactionId, double revenue, double cost, double profit) {
        this.transactionId = transactionId;
        this.revenue = revenue;
        this.cost = cost;
        this.profit = profit;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public double getRevenue() { return revenue; }
    public void setRevenue(double revenue) { this.revenue = revenue; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public double getProfit() { return profit; }
    public void setProfit(double profit) { this.profit = profit; }
}
