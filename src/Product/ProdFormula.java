package Product;

import Main.*;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.controlsfx.control.textfield.TextFields;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class ProdFormula {

    private static String[] propertyMethodName = new String[]{"Name", "Amount", "UnitPrice", "TotalPrice"};
    private static String[] property = new String[]{"name", "amount", "unitPrice", "totalPrice"};
    private static String[] header = new String[]{"原料名称", "数量", "单价", "金额"};
    private static String[] parentNullFormulaInfoHeader = new String[]{"配方名称", "成本价", "配方总量"};
    private static String[] parentNullFormulaInfoProperty = new String[]{"Name", "BasePrice"};
    private static String[] parentFormulaInfoHeader = new String[]{"原料名称", "数量", "单价", "金额", "配方总量"};

    public ImageView backButton;
    public TableView<Formula> formulaTable;
    public HBox formulaTopHBox;
    public HBox formulaInfoInputBottomHBox;
    public Button defaultButton;

    public Button addItemButton;
    public Button overrideButton;
    public Button saveNewButton;
    public Button cancelButton;

    boolean isNewFormula;

    Stage currentStage;
    ProductOrder productOrder;
    Formula currentFormula;

    ArrayList<TableColumn<Formula, ?>> formulaColumnList;
    ArrayList<TextField> bottomInputTextFieldArray;
    ArrayList<TextField> topFormulaInfoArray;

    LinkedList<Formula> formulaLinkedList;

    /**
     * initialize everything on screen
     * @param productOrder the select order
     * @param currentFormula the formula fo the table view
     */
    public void init(ProductOrder productOrder, Formula currentFormula, LinkedList<Formula> linkedList) {
        currentStage = Main.mainStage;
        this.productOrder = productOrder;
        this.currentFormula = currentFormula;
        this.formulaLinkedList = linkedList;

        if (currentFormula == null) getFormula();

        if (formulaLinkedList == null) {
            formulaLinkedList = new LinkedList<>();
            formulaLinkedList.add(this.currentFormula);
        }

        if (formulaLinkedList.size() != 1)
            FinalConstants.setButtonImagesAndCursor(backButton, FinalConstants.backWhite, FinalConstants.backBlack);
        else
            backButton.setVisible(false);

        backButton.setOnMouseClicked(event -> {
            saveFormulaWithinFormula();
            loadParentPage();
        });
        formulaColumnList = new ArrayList<>();
        

        initTableView();
        initTopHBox();
        initBottomInputHBox();

        defaultButton.setOnMouseClicked(event -> saveDefaultFormula());

        defaultButton.setVisible(false);
        overrideButton.setVisible(false);
        saveNewButton.setVisible(false);
        cancelButton.setVisible(false);

        if (formulaLinkedList.size() == 1) {
            defaultButton.setVisible(true);
            overrideButton.setVisible(true);
            saveNewButton.setVisible(true);
            cancelButton.setVisible(true);
        }

        cancelButton.setOnMouseClicked(event -> {
            if (ConfirmBox.display("确认", "确定取消？配方不会被保存", "是", "否")) {
                try {
                    FXMLLoader loader = new FXMLLoader();
                    InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "MainScreen.fxml");
                    Parent newScene = loader.load(fileInputStream);
                    Main.mainStage.setTitle("订单管理系统");

                    Scene scene = new Scene(newScene);
                    scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
                    Main.mainStage.setScene(scene);
                } catch (Exception e) {
                    AlertBox.display("错误", "主页窗口错误！");
                    new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                            e.getMessage(), e.getStackTrace(), false);
                }
            }
        });
        saveNewButton.setOnMouseClicked(event -> saveNewFormula());
        overrideButton.setOnMouseClicked(event -> overrideCurrentFormula());

        calcBasePrice();

    }

    private void saveFormulaWithinFormula() {
        calcBasePrice();
        if (!formulaLinkedList.getLast().equals(currentFormula)) {
            AlertBox.display("错误", "配方套配方链条错误。(联系管理员）");
        }

        formulaLinkedList.removeLast();
        Formula parentFormula = formulaLinkedList.getLast();

        int index = -1;
        for (int i = 0; i < parentFormula.getFormulaList().size(); i++) {
            if (parentFormula.getFormulaList().get(i).equals(currentFormula)) index = i;
        }

        if (index == -1) {
            AlertBox.display("错误", "配方套配方链条错误。(联系管理员）");
        }

        currentFormula.setFormulaList(new ArrayList<>(formulaTable.getItems()));
        currentFormula.setName(topFormulaInfoArray.get(0).getText());
        try {
            currentFormula.setAmount(Double.parseDouble(topFormulaInfoArray.get(1).getText()));

        } catch (Exception e) {
            AlertBox.display("错误", "规格输入错误，默认为0");
            currentFormula.setAmount(0);
        }
        try {
            currentFormula.setUnitPrice(Double.parseDouble(topFormulaInfoArray.get(2).getText()));
        } catch (Exception e) {
            AlertBox.display("错误", "单价输入错误，默认为0");
            currentFormula.setAmount(0);
        }
        currentFormula.setTotalPrice();
        parentFormula.getFormulaList().set(index, currentFormula);
    }

    private void loadParentPage() {
        if (formulaLinkedList.size() == 0) {
            try {
                FXMLLoader loader = new FXMLLoader();
                InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "MainScreen.fxml");
                Parent newScene = loader.load(fileInputStream);
                Main.mainStage.setTitle("订单管理系统");

                Scene scene = new Scene(newScene);
                scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
                Main.mainStage.setScene(scene);
            } catch (Exception e) {
                AlertBox.display("错误", "主页窗口错误！");
                new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                        e.getMessage(), e.getStackTrace(), false);
            }
        } else {
            try {
                FXMLLoader loader = new FXMLLoader();
                InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "ProdFormula.fxml");
                Parent newScene = loader.load(fileInputStream);

                ProdFormula prodFormula = loader.getController();
                prodFormula.init(productOrder, formulaLinkedList.getLast(), formulaLinkedList);

                Scene scene = new Scene(newScene);
                scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
                Main.mainStage.setScene(scene);
            } catch (Exception e) {
                AlertBox.display("错误", "主页窗口错误！");
                new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                        e.getMessage(), e.getStackTrace(), false);
            }
        }
    }

    /**
     * populate table view
     */
    private void initTableView() {
        // loop to set up all regular columns
        for (int i = 0; i < property.length; i++) {
            if (i == 1 || i == 2 || i == 3) {
                // Doubles
                TableColumn<Formula, Double> newColumn = new TableColumn<>(header[i]);
                newColumn.setCellValueFactory(new PropertyValueFactory<>(property[i]));
                newColumn.setStyle("-fx-alignment: CENTER;");
                newColumn.setMinWidth(100);
                formulaColumnList.add(newColumn);
            } else {
                // String
                TableColumn<Formula, String> newColumn = new TableColumn<>(header[i]);
                newColumn.setCellValueFactory(new PropertyValueFactory<>(property[i]));
                newColumn.setStyle("-fx-alignment: CENTER;");
                newColumn.setMinWidth(130);
                formulaColumnList.add(newColumn);
            }
        }

        formulaTable.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.BACK_SPACE || keyEvent.getCode() == KeyCode.DELETE) {
                if (ConfirmBox.display("确认", "确定删除原料？", "是", "否"))
                    removeFormulaFromList(formulaTable.getSelectionModel().getSelectedItem());
            }
        });

        formulaTable.setRowFactory(param -> {
            TableRow<Formula> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    viewFormula(row.getItem());
                }
            });
            return row;
        });

        formulaTable.getColumns().setAll(formulaColumnList);
        if (currentFormula != null) formulaTable.getItems().setAll(currentFormula.getFormulaList());
    }

    /**
     * Get current formula, if does not exists, null
     */
    private void getFormula() {
        // getting current formula
        try {
            // if this is a product that doesn't have existing formula
                if (productOrder.getFormulaIndex() != -1) {
                currentFormula = DatabaseUtil.GetFormulaByIndex(productOrder.getFormulaIndex());
                if (productOrder.getBasePrice() == 0.0) {
                    productOrder.setBasePrice(calcBasePrice());
                    DatabaseUtil.UpdateProdOrder(productOrder);
                }
            } else {
                if (DatabaseUtil.CheckIfNameExistsInNewestFormula(productOrder.getName())) {
                    int newIndex = DatabaseUtil.GetNewestFormulaIndex(productOrder.getName());
                    currentFormula = DatabaseUtil.GetFormulaByIndex(newIndex);
                    productOrder.setFormulaIndex(newIndex);
                    productOrder.setBasePrice(calcBasePrice());
                    DatabaseUtil.UpdateProdOrder(productOrder);
                } else currentFormula = null;
            }
        } catch (Exception e) {
            new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                    e.getMessage(), e.getStackTrace(), false);
            productOrder.setFormulaIndex(-1);
            currentFormula = null;
        }
        isNewFormula = false;
        if (currentFormula == null) {
            isNewFormula = true;
            currentFormula = new Formula(productOrder.getName());
        }
    }

    /**
     * Calculate the base price
     *
     * @return the base price
     */
    private double calcBasePrice() {

        double currentEnteredBasePrice = 0.0;
        try {
            currentEnteredBasePrice = Double.parseDouble(topFormulaInfoArray.get(2).getText());
        } catch (Exception ignored) {}
        double totalSum = 0.0;
        double totalAmount = 0.0;
        if (currentStage == null) return 0.0;
        for (Formula formula : currentFormula.getFormulaList()) {
            totalSum += formula.getTotalPrice();
            totalAmount += formula.getAmount();
        }
        double basePrice = Math.round(totalSum / totalAmount * 1.05 * 100.0) / 100.0;
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        System.out.println(totalAmount);
        System.out.println(basePrice);

        try {
            // top formula
            if (formulaLinkedList.size() == 1) {
                // top formula
                topFormulaInfoArray.get(1).setText(String.valueOf(basePrice));
                topFormulaInfoArray.get(2).setText(String.valueOf(totalAmount));
            } else {
                // formula within formula
                topFormulaInfoArray.get(4).setText(String.valueOf(totalAmount));
                if ((totalAmount == 0.0 || basePrice == 0.0) && currentEnteredBasePrice == 0.0) {
                    double tempBasePrice = DatabaseUtil.GetMatUnitPrice(topFormulaInfoArray.get(0).getText());
                    if (tempBasePrice > 0) topFormulaInfoArray.get(2).setText(String.valueOf(tempBasePrice));
                    else topFormulaInfoArray.get(2).setText(String.valueOf(currentEnteredBasePrice));
                } else if ((totalAmount == 0.0 || basePrice == 0.0) && currentEnteredBasePrice != 0.0) {
                    topFormulaInfoArray.get(2).setText(String.valueOf(currentEnteredBasePrice));
                } else {
                    topFormulaInfoArray.get(2).setText(String.valueOf(basePrice));
                }
            }
        } catch (Exception ignored) {
        }
        return basePrice;
    }

    /**
     * public function for other controller to call, to remove item from the list, and refresh table
     *
     * @param inputFormula the formula to be removed to list
     */
    public void removeFormulaFromList(Formula inputFormula) {
        currentFormula.removeFormula(inputFormula);
        formulaTable.getItems().clear();
        formulaTable.getItems().setAll(currentFormula.getFormulaList());
        topFormulaInfoArray.get(2).setText("0.0");
        calcBasePrice();
    }

    /**
     * Init top formula into hbox
     */
    private void initTopHBox() {
        if (formulaLinkedList.size() == 1) initTopHBoxSelectedOrder();
        else initTopHBoxParentFormula();
    }

    private void initTopHBoxSelectedOrder() {
        // populating the info hbox
        topFormulaInfoArray = new ArrayList<>();
        Method getter;

        // adding the text fields and labels
        for (int i = 0; i < parentNullFormulaInfoHeader.length; i++) {

            // new label
            Label newLabel = new Label(parentNullFormulaInfoHeader[i]);
            newLabel.setStyle("-fx-font-size: 20px; -fx-alignment: center-right;");

            // new text field
            JFXTextField newTextField = new JFXTextField();
            newTextField.setDisable(true);

            if (i == 2) {
                // obtaining amount by adding all formula's amount
                if (currentFormula.getFormulaList().size() == 0)
                    newTextField.setText("0.0");
                else {
                    double totalAmount = 0;
                    for (Formula formula : currentFormula.getFormulaList()) {
                        totalAmount += formula.getAmount();
                    }
                    newTextField.setText(String.valueOf(totalAmount));
                }
            } else {
                try {
                    getter = ProductOrder.class.getDeclaredMethod("get" + parentNullFormulaInfoProperty[i]);
                    newTextField.setText(String.valueOf(getter.invoke(productOrder)));
                } catch (Exception e) {
                    AlertBox.display("错误", "读取信息错误！");
                    new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                            e.getMessage(), e.getStackTrace(), false);
                }
            }

            // add to Hbox
            formulaTopHBox.getChildren().add(newLabel);
            formulaTopHBox.getChildren().add(newTextField);

            // add text fields to array
            topFormulaInfoArray.add(newTextField);
        }
    }

    private void initTopHBoxParentFormula() {
        // populating the info hbox
        topFormulaInfoArray = new ArrayList<>();
        Method getter;
        for (int i = 0; i < parentFormulaInfoHeader.length; i++) {
            Label newLabel = new Label(parentFormulaInfoHeader[i]);
            newLabel.setStyle("-fx-font-size: 20px; -fx-alignment: center-right;");

            JFXTextField newTextField = new JFXTextField();

            // user can not edit amount
            if (i == 4) {
                newTextField.setDisable(true);
                double totalAmount = 0;
                for (Formula formula : currentFormula.getFormulaList()) {
                    totalAmount += formula.getAmount();
                }
                newTextField.setText(String.valueOf(totalAmount));
            } else {
                if (i == 3) newTextField.setDisable(true);
                try {
                    getter = Formula.class.getDeclaredMethod("get" + propertyMethodName[i]);
                    newTextField.setText(String.valueOf(getter.invoke(currentFormula)));
                } catch (Exception e) {
                    new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                            e.getMessage(), e.getStackTrace(), false);
                }
            }

            formulaTopHBox.getChildren().add(newLabel);
            formulaTopHBox.getChildren().add(newTextField);
            topFormulaInfoArray.add(newTextField);
        }

        // auto total price
        TextField amount = topFormulaInfoArray.get(1);
        TextField unitPrice = topFormulaInfoArray.get(2);
        TextField totalPrice = topFormulaInfoArray.get(3);

        ChangeListener listener = (ChangeListener<String>) (observable, oldValue, newValue) -> {
            try {
                totalPrice.setText(String.valueOf(
                        Double.parseDouble(unitPrice.getText()) * Double.parseDouble(amount.getText())));
            } catch (Exception ignored) {
                totalPrice.setText("0.0");
            }
        };
        amount.textProperty().addListener(listener);
        unitPrice.textProperty().addListener(listener);
    }

    /**
     * Initialize info hbox
     */
    private void initBottomInputHBox() {
        // populating the info hbox
        bottomInputTextFieldArray = new ArrayList<>();
        for (String s : header) {

            // new text field
            JFXTextField newTextField = new JFXTextField();
            if (s.equals("原料名称")) TextFields.bindAutoCompletion(newTextField, FinalConstants.autoCompleteMatName);
            newTextField.setPromptText("输入" + s);

            // add to the HBox
            formulaInfoInputBottomHBox.getChildren().add(newTextField);

            // add to array
            bottomInputTextFieldArray.add(newTextField);
        }

        // info hbox add button
        addItemButton = new JFXButton("添加");
        addItemButton.getStyleClass().add("actionButtons");
        formulaInfoInputBottomHBox.getChildren().add(addItemButton);

        addItemButton.setOnAction(event -> {
            addItem();
            defaultButton.setVisible(false);
        });
        addItemButton.setOnKeyReleased(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                addItem();
                defaultButton.setVisible(false);
            }
        });


        // auto price
        TextField totalPrice = bottomInputTextFieldArray.get(3);
        EventHandler autoTotalPriceEventHandler = event -> {
            try {
                totalPrice.setText(String.valueOf(Double.parseDouble(bottomInputTextFieldArray.get(1).getText()) *
                        Double.parseDouble(bottomInputTextFieldArray.get(2).getText())));
            } catch (Exception ignored) {
            }
        };
        for (int i = 1; i < bottomInputTextFieldArray.size(); i++) {
            TextField textField = bottomInputTextFieldArray.get(i);
            textField.setOnKeyTyped(autoTotalPriceEventHandler);
            textField.setOnMouseClicked(autoTotalPriceEventHandler);
        }

        TextField nameTextField = bottomInputTextFieldArray.get(0);
        nameTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    bottomInputTextFieldArray.get(2).setText(String.valueOf(DatabaseUtil.GetMatUnitPrice(nameTextField.getText())));
                } catch (SQLException e) {
                    bottomInputTextFieldArray.get(2).setText("0");
                    new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                            e.getMessage(), e.getStackTrace(), false);
                }
            }
        });
    }

    /**
     * Add Simple item to the product
     */
    private void addItem() {
        Formula formula = new Formula();
        Method setter;
        boolean empty = true;

        for (int i = 0; i < header.length; i++) {
            TextField currentTextField = bottomInputTextFieldArray.get(i);
            if (i == 0) {
                if (currentTextField.getText() != null && !currentTextField.getText().equals("")) {
                    empty = false;
                    try {
                        setter = Formula.class.getDeclaredMethod("set" + propertyMethodName[i], String.class);
                        setter.invoke(formula, currentTextField.getText());
                    } catch (Exception e) {
                        new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                                e.getMessage(), e.getStackTrace(), false);
                    }
                }
            } else if (i == 1 || i == 2) {
                if (currentTextField.getText() != null && !currentTextField.getText().equals("")) {
                    empty = false;
                    try {
                        setter = Formula.class.getDeclaredMethod("set" + propertyMethodName[i], double.class);
                        setter.invoke(formula, Double.parseDouble(currentTextField.getText()));
                    } catch (Exception e) {
                        new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                                e.getMessage(), e.getStackTrace(), false);
                    }
                }
            }
        }

        formula.setTotalPrice();
        if (!empty) addItemToTableView(formula);
        for (TextField textField : bottomInputTextFieldArray) textField.clear();

        calcBasePrice();
    }

    /**
     * Add formula to the tableView
     *
     * @param newFormula the formula to be added
     */
    public void addItemToTableView(Formula newFormula) {
        currentFormula.addFormula(newFormula);
        formulaTable.getItems().clear();
        formulaTable.getItems().setAll(currentFormula.getFormulaList());
        calcBasePrice();
    }

    /**
     * Set current formula to be default for this product
     */
    private void saveDefaultFormula() {
        try {
            if (DatabaseUtil.CheckIfNameExistsInNewestFormula(productOrder.getName()))
                DatabaseUtil.UpdateNewestFormula(true, productOrder.getName(), productOrder.getFormulaIndex());
            else DatabaseUtil.UpdateNewestFormula(false, productOrder.getName(), productOrder.getFormulaIndex());
            AlertBox.display("成功", "设置成功");
        } catch (SQLException e) {
            AlertBox.display("错误", "设置默认错误");
            new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                    e.getMessage(), e.getStackTrace(), false);
        }
    }

    /**
     * Pop up window to edit/add formula within formula
     */
    private void viewFormula(Formula selectedFormula) {
        try {
            formulaLinkedList.add(selectedFormula);

            FXMLLoader loader = new FXMLLoader();
            InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "ProdFormula.fxml");
            Parent newScene = loader.load(fileInputStream);

            ProdFormula prodFormula = loader.getController();
            prodFormula.init(productOrder, selectedFormula, formulaLinkedList);

            Scene scene = new Scene(newScene);
            scene.getStylesheets().add(getClass().getResource(Main.styleSheetPath).toURI().toString());
            currentStage.setScene(scene);
        } catch (Exception e) {
            AlertBox.display("错误", "窗口错误");
            new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                    e.getMessage(), e.getStackTrace(), false);
        }
    }

    /**
     * Save the formula to the selected order, and push formula to database
     */
    private void saveNewFormula() {
        if (!isNewFormula && !ConfirmBox.display("确认", "确定另存为？之前配方不会被更该。", "是", "否"))
            return;
        try {

            ArrayList<Formula> newFormulaList = new ArrayList<>(formulaTable.getItems());
            currentFormula.setFormulaList(newFormulaList);
            productOrder.setBasePrice(calcBasePrice());

            int index = DatabaseUtil.AddFormula(currentFormula);
            productOrder.setFormulaIndex(index);
            DatabaseUtil.UpdateProdOrder(productOrder);

            formulaLinkedList.removeLast();

            loadParentPage();
        } catch (SQLException e) {
            AlertBox.display("错误", "保存配方错误！");
            new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                    e.getMessage(), e.getStackTrace(), false);
        }
    }

    /**
     * Save the formula to the selected order, and push formula to database
     */
    private void overrideCurrentFormula() {
        if (isNewFormula) {
            saveNewFormula();
            return;
        }
        if (!ConfirmBox.display("确认", "确定更新此配方？所有使用此配方的产品即将被更新。", "是", "否"))
            return;
        if (ConfirmBox.display("确认", "是否设为以后次产品的默认订单？", "是", "否"))
            saveDefaultFormula();
        try {
            double newBasePrice = calcBasePrice();
            ArrayList<Formula> newFormulaList = new ArrayList<>(formulaTable.getItems());
            currentFormula.setFormulaList(newFormulaList);
            productOrder.setBasePrice(newBasePrice);

            DatabaseUtil.UpdateFormula(currentFormula, productOrder.getFormulaIndex());
            DatabaseUtil.UpdateProdOrder(productOrder);
            DatabaseUtil.UpdateAllProdOrderNewBasePrice(productOrder.getFormulaIndex(), newBasePrice);

            formulaLinkedList.removeLast();

            loadParentPage();

        } catch (SQLException e) {
            new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
                    e.getMessage(), e.getStackTrace(), false);
        }
    }

}
