package model;

public class ProfitByItemDTO {
    private String itemCode;
    private String itemName;
    private int totalSold;
    private double profit;

    public ProfitByItemDTO(String itemCode, String itemName, int totalSold, double profit) {
        this.itemCode  = itemCode;
        this.itemName  = itemName;
        this.totalSold = totalSold;
        this.profit    = profit;
    }

    public String getItemCode()  { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemName()  { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public int getTotalSold()    { return totalSold; }
    public void setTotalSold(int totalSold) { this.totalSold = totalSold; }

    public double getProfit()    { return profit; }
    public void setProfit(double profit) { this.profit = profit; }
}
