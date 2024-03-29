package Product;

import Main.*;

import com.jfoenix.controls.JFXToggleButton;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.controlsfx.control.textfield.TextFields;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ProdEditOrder {

    // prod table headers
    private static final String[] prodHeaders = new String[]{"订单日期", "送货单号", "\u3000\u3000客户", "产品名称",
            "\u3000\u3000规格", "\u3000\u3000数量", "\u3000\u3000单价", "\u3000\u3000备注", "张家港生产"};

    // all prod property listed
    private static final String[] prodProperty = new String[]{"OrderDate", "Sku", "Customer", "Name",
            "UnitAmount", "Amount", "UnitPrice", "Note", "Remote"};

    @FXML
    GridPane prodEditOrderGrid;
    @FXML
    Label prodEditOrderTitleLabel;
    @FXML
    Button cancelButton;
    @FXML
    Button completeButton;

    Stage currentStage;
    ProductOrder selectedOrder;
    ArrayList<Node> prodOrderInputArray;

    /**
     * Called by main controller to give the selected order
     *
     * @param selectedOrder the order that was selected, to fill the information
     * @param currentStage  the stage, so it can be closed later
     */
    public void initData(ProductOrder selectedOrder, Stage currentStage) {
        this.selectedOrder = selectedOrder;
        this.currentStage = currentStage;
        init();
    }

    /**
     * Initialize all the element on the screen
     */
    public void init() {
        prodEditOrderTitleLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        cancelButton.setOnAction(actionEvent -> {
            if (ConfirmBox.display("确认", "确定取消？订单信息即将还原", "确认", "取消"))
                currentStage.close();
        });

        completeButton.setOnAction(event -> updateOrder());

        int row = 1;
        int col = 0;

        // setting up all the labels
        ArrayList<Label> prodOrderLabelArray = new ArrayList<>();
        for (int i = 0; i < prodHeaders.length; i++) {
            Label newLabel = new Label(prodHeaders[i]);
            newLabel.setStyle("-fx-font-size: 20px;" +
                    "-fx-alignment: center-right;");
            GridPane.setConstraints(newLabel, col, row++);
            prodOrderLabelArray.add(newLabel);
            if ((i + 4) % 3 == 0) {
                row = 1;
                col += 2;
            }
        }

        row = 1;
        col = 1;

        // setting up all the text field
        prodOrderInputArray = new ArrayList<>();
        for (int i = 0; i < prodProperty.length; i++) {

            Method getter;

            // dates, date picker
            if (i == 0) {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                DatePicker datePicker = new DatePicker();
                Date currentDate = selectedOrder.getOrderDate();
                datePicker.setValue(LocalDate.of(currentDate.getY(), currentDate.getM(), currentDate.getD()));

                datePicker.setConverter(new StringConverter<>() {
                    @Override
                    public String toString(LocalDate localDate) {
                        if (localDate == null) {
                            return "0/0/0";
                        }
                        return dateTimeFormatter.format(localDate);
                    }

                    @Override
                    public LocalDate fromString(String string) {
                        if (string == null || string.isEmpty()) {
                            return null;
                        }
                        return LocalDate.from(dateTimeFormatter.parse(string));
                    }
                });


                GridPane.setConstraints(datePicker, col, row++);
                prodOrderInputArray.add(datePicker);
            }

            else if (i == 8) {
                JFXToggleButton toggleButton = new JFXToggleButton();
                toggleButton.selectedProperty().set(selectedOrder.getRemoteInt() != 0);
                GridPane.setConstraints(toggleButton, col, row++);
                prodOrderInputArray.add(toggleButton);
            }

            // regular text field
            else {
                TextField newTextField = new TextField();
                if (i == 2) {
                    FinalConstants.updateAutoCompleteProdCustomerName();
                    TextFields.bindAutoCompletion(newTextField, FinalConstants.autoCompleteProdCustomerName);
                }
                if (i == 3) {
                    FinalConstants.updateAutoCompleteProdName();
                    TextFields.bindAutoCompletion(newTextField, FinalConstants.autoCompleteProdName);
                }
                newTextField.setPromptText("输入" + prodHeaders[i].replace("\u3000", ""));
                try {
                    getter = ProductOrder.class.getDeclaredMethod("get" + prodProperty[i]);
                    newTextField.setText(String.valueOf(getter.invoke(selectedOrder)));
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    AlertBox.display("错误", "摘取信息错误！");
                    new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                            e.getMessage(), e.getStackTrace(), false);
                }
                GridPane.setConstraints(newTextField, col, row++);
                prodOrderInputArray.add(newTextField);
            }

            if ((i + 4) % 3 == 0) {
                row = 1;
                col += 2;
            }
        }

        // auto unit price
        TextField unitPrice = (TextField) prodOrderInputArray.get(6);
        unitPrice.setOnKeyTyped(keyEvent -> {
            try {
                unitPrice.setText(String.valueOf(DatabaseUtil.GetProdUnitPrice(((TextField) prodOrderInputArray.get(3)).getText(),
                        ((TextField) prodOrderInputArray.get(2)).getText())));
            } catch (Exception e) {
                new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                        e.getMessage(), e.getStackTrace(), false);
            }
        });
        unitPrice.setOnMouseClicked(keyEvent -> {
            try {
                System.out.println(((TextField) prodOrderInputArray.get(3)).getText());
                unitPrice.setText(String.valueOf(DatabaseUtil.GetProdUnitPrice(((TextField) prodOrderInputArray.get(3)).getText(),
                        ((TextField) prodOrderInputArray.get(2)).getText())));
            } catch (Exception e) {
                new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                        e.getMessage(), e.getStackTrace(), false);
            }
        });

        prodEditOrderGrid.setVgap(10);
        prodEditOrderGrid.setHgap(10);
        prodEditOrderGrid.getChildren().addAll(prodOrderLabelArray);
        prodEditOrderGrid.getChildren().addAll(prodOrderInputArray);

    }

    /**
     * Obtain all the new information, update order, and push it to database
     */
    private void updateOrder() {
        ProductOrder newOrder = new ProductOrder(selectedOrder.getSerialNum());
        newOrder.setFormulaIndex(selectedOrder.getFormulaIndex());
        newOrder.setOrderDate(((DatePicker) prodOrderInputArray.get(0)).getValue() == null ? new Date(0, 0, 0) :
                new Date(((DatePicker) prodOrderInputArray.get(0)).getValue().getYear(),
                        ((DatePicker) prodOrderInputArray.get(0)).getValue().getMonthValue(),
                        ((DatePicker) prodOrderInputArray.get(0)).getValue().getDayOfMonth()));

        Method setter;

        for (int i = 1; i < prodOrderInputArray.size(); i++) {
            if (i != 8) {
                TextField currentTextField = (TextField) prodOrderInputArray.get(i);
                try {
                    if (i == 4 || i == 5 || i == 6) {
                        setter = ProductOrder.class.getDeclaredMethod("set" + prodProperty[i], double.class);
                        setter.invoke(newOrder, Double.parseDouble(currentTextField.getText()));
                    } else {
                        setter = ProductOrder.class.getDeclaredMethod("set" + prodProperty[i], String.class);
                        setter.invoke(newOrder, currentTextField.getText());
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    AlertBox.display("错误", "无法更新");
                    new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                            e.getMessage(), e.getStackTrace(), false);
                }
            } else {
                newOrder.setRemote(((JFXToggleButton) prodOrderInputArray.get(i)).selectedProperty().get() ? 1 : 0);
            }
        }

        try {
            DatabaseUtil.UpdateProdOrder(newOrder);
            currentStage.close();
        } catch (Exception e) {
            AlertBox.display("错误", "无法更新");
            new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                    e.getMessage(), e.getStackTrace(), false);
        }
    }

}
