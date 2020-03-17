package Product;

import java.io.Serializable;

public class FormulaItem implements Serializable {

    private Formula parent;
    private String itemName;
    private double amount;
    private double unitPrice;
    private double totalPrice;

    public Formula getParent() {
        return parent;
    }

    public void setParent(Formula parent) {
        this.parent = parent;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice() {
        this.totalPrice = amount * unitPrice;
    }

    /**
     * Given a formula item, convert to a formula object
     * @param formulaItem the item that needs to be converted
     * @return a formula object that has all the information
     */
    public static Formula convertToFormula(FormulaItem formulaItem) {
        Formula returnVal = new Formula();
        returnVal.setAmount(formulaItem.getAmount());
        returnVal.setUnitPrice(formulaItem.getUnitPrice());
        returnVal.setTotalPrice();
        returnVal.setFormulaName(formulaItem.getItemName());
        returnVal.setFolderPath(formulaItem.getParent().getFolderPath());
        return returnVal;
    }

}
