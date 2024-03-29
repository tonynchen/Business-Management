package Main;

// from my other packages

import CustomEditingCells.*;
import Material.*;
import Product.*;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;


public class MainScreen implements Initializable {

	// mat table headers
	private static final String[] matHeaders = new String[]{"订单日期", "订单号", "原料名称", "类别", "付款日期",
			"到达日期", "发票日期", "发票编号", "规格", "数量", "公斤", "单价", "总价", "签收人", "供应商订单编号", "供应商",
			"联系人", "手机", "座机", "传真", "供应商账号", "供应商银行地址", "供应商地址", "备注"};

	// all mat property listed
	private static final String[] matProperty = new String[]{"orderDate", "sku", "name", "type", "paymentDate",
			"arrivalDate", "invoiceDate", "invoice", "unitAmount", "amount", "kgAmount", "unitPrice", "totalPrice",
			"signed", "skuSeller", "company", "contact", "mobile", "land", "fax", "account",
			"bank", "address", "note"};

	// prod table headers
	private static final String[] prodHeaders = new String[]{"订单日期", "送货单号", "客户", "产品名称",
			"规格", "数量", "公斤", "单价", "金额", "成本价", "备注", "张家港生产"};

	// all prod property listed
	private static final String[] prodProperty = new String[]{"orderDate", "sku", "customer", "name",
			"unitAmount", "amount", "kgAmount", "unitPrice", "totalPrice", "basePrice", "note", "remote"};

	private ObservableList<MatOrder> tempQuickSearchMatOrderList;

	private ObservableList<ProductOrder> tempQuickSearchProdOrderList;

	public TabPane mainTabPane;
	public Tab matTab;
	public Tab prodTab;
	public Tab matUnitPriceTab;
	public Tab prodUnitPriceTab;
	public Tab remoteInventoryTab;
	public TableView<ProductOrder> prodTableView;
	public TableView<MatOrder> matTableView;
	public TableView inventoryTableView;
	public ImageView searchButton;
	public ImageView addButton;
	public ImageView quitButton;
	public ImageView resetButton;
	public ImageView excelButton;
	public TextField prodUnitPriceSearchTextField;
	public TextField matUnitPriceSearchTextField;
	public TextField prodSearchTextField;
	public TextField matSearchTextField;
	public Tab matSeller;
	public TextField matSellerSearchTextField;
	public TableView<MatSeller> matSellerTableView;

	public TableView<ProdUnitPrice> prodUnitPriceTableView;
	public TableView<MatUnitPrice> matUnitPriceTableView;

	/**
	 * Call to fill the table with all orders, set up actions for all buttons, set up search bars, set up image view
	 *
	 * @param url            N/A
	 * @param resourceBundle N/A
	 */
	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		initButtons();

		fillMatTable(FinalConstants.updateAllMatOrders());
		fillProdTable(FinalConstants.updateAllProdOrders());
		fillMatSellerTable(FinalConstants.updateAllMatSellers());
		initMatUnitPrice();
		initProdUnitPrice();

		mainTabPane.getSelectionModel().select(FinalConstants.getSelectedTab());

		// filling the mat table
		tempQuickSearchMatOrderList = FXCollections.observableArrayList();
		tempQuickSearchMatOrderList.addAll(FinalConstants.allMatOrders);
		tempQuickSearchProdOrderList = FXCollections.observableArrayList();
		tempQuickSearchProdOrderList.addAll(FinalConstants.allProdOrders);

		// precision search mat/prod orders
		searchButton.setOnMouseClicked(event -> {
			// if selected tab is material
			if (mainTabPane.getSelectionModel().getSelectedItem().equals(matTab)) searchMatOrder();
				// if selected tab is product
			else searchProdOrder();
		});

		// add mat/prod orders
		addButton.setOnMouseClicked(event -> {
			// if selected tab is material
			if (mainTabPane.getSelectionModel().getSelectedItem().equals(matTab)) addMatOrder();
				// if selected tab is product
			else if (mainTabPane.getSelectionModel().getSelectedItem().equals(matSeller)) addMatSeller();
			else addProdOrder();
		});

		mainTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(matUnitPriceTab) || newValue.equals(prodUnitPriceTab) || newValue.equals(remoteInventoryTab)) {
				addButton.setDisable(true);
				excelButton.setDisable(true);
				searchButton.setDisable(true);
			} else {
				addButton.setDisable(false);
				excelButton.setDisable(false);
				searchButton.setDisable(false);
			}
		});

		// quit the application and save backup database
		quitButton.setOnMouseClicked(actionEvent -> {
			Path source = Paths.get("BusinessCashFlow.db");
			Path target = Paths.get(System.getProperty("user.home") + "/BusinessCashFlow.db");
			try {
				if (DatabaseUtil.GetAllMatOrders().size() > 0) {
					if (Files.exists(target)) Files.delete(target);
					Files.copy(source, target);
				}
			} catch (IOException | SQLException e) {
				new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
						e.getMessage(), e.getStackTrace(), false);
			}
			if (ConfirmBox.display("确认", "确认关闭？", "是", "否"))
				Main.mainStage.close();
		});

		// reset both table
		resetButton.setOnMouseClicked(actionEvent -> resetTable());

		// excel button
		excelButton.setOnMouseClicked(event -> {
			// if selected tab is material
			if (mainTabPane.getSelectionModel().getSelectedItem().equals(matTab)) generateMatExcel();
				// if selected tab is product
			else generateProdExcel();
		});

		// listener for mat search
		matSearchTextField.textProperty().addListener((observableValue, oldValue, newValue) -> {
			// if the text field is updated to be empty
			if (newValue == null || newValue.equals("")) {
				matTableView.getItems().clear();
				tempQuickSearchMatOrderList = FXCollections.observableArrayList(FinalConstants.allMatOrders);
			} else {
				// if user deleted char, copying original array
				if (newValue.length() < oldValue.length()) {
					matTableView.getItems().clear();
					tempQuickSearchMatOrderList = FXCollections.observableArrayList(FinalConstants.allMatOrders);
				}
				// removing orders that doesn't contain key word
				tempQuickSearchMatOrderList.removeIf(matOrder -> !matOrder.toString().contains(newValue));
			}
			matTableView.setItems(tempQuickSearchMatOrderList);
		});

		// listener for prod search
		prodSearchTextField.textProperty().addListener((observableValue, oldValue, newValue) -> {
			// if the text field is updated to be empty
			if (newValue == null || newValue.equals("")) {
				prodTableView.getItems().clear();
				tempQuickSearchProdOrderList = FXCollections.observableArrayList(FinalConstants.allProdOrders);
			} else {
				// if user deleted char, copying original array
				if (newValue.length() < oldValue.length()) {
					prodTableView.getItems().clear();
					tempQuickSearchProdOrderList = FXCollections.observableArrayList(FinalConstants.allProdOrders);
				}
				// removing orders that doesn't contain key word
				tempQuickSearchProdOrderList.removeIf(productOrder -> !productOrder.toString().contains(newValue));
			}
			prodTableView.setItems(tempQuickSearchProdOrderList);

		});

		// inventory page
		mainTabPane.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			FinalConstants.setSelectedTab(newValue.intValue());
			if (newValue.intValue() == 3) {
				initInventory();
			}
		});
	}

	/**
	 * Filling of the material table
	 *
	 * @param selectedMatSeller the orders specified
	 */
	public void fillMatSellerTable(ObservableList<MatSeller> selectedMatSeller) {

		// array of columns
		Collection<TableColumn<MatSeller, ?>> matSellerColumnArrayList = new ArrayList<>();

		// loop to set up all regular columns
		for (int i = 0; i < FinalConstants.matSellerTableHeaders.length; i++) {
			// String
			TableColumn<MatSeller, String> newColumn = new TableColumn<>(FinalConstants.matSellerTableHeaders[i]);
			newColumn.setCellValueFactory(new PropertyValueFactory<>(FinalConstants.matSellerPropertyHeaders[i]));
			newColumn.setStyle("-fx-alignment: CENTER;");
			matSellerColumnArrayList.add(newColumn);
		}

		// if backspace or delete, delete the order
		matSellerTableView.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.BACK_SPACE || keyEvent.getCode() == KeyCode.DELETE)
				deleteMatSeller(matSellerTableView.getSelectionModel().getSelectedItem());
		});

		// filling the table
		matSellerTableView.getColumns().setAll(matSellerColumnArrayList);
		matSellerTableView.getItems().clear();
		matSellerTableView.getItems().setAll(selectedMatSeller);
		matSellerTableView.refresh();
	}

	/**
	 * Fill the inventory page
	 */
	private void initInventory() {

		try {
			ArrayList<MatOrder> allMatOrders = new ArrayList<>(DatabaseUtil.GetAllMatOrders());
			ArrayList<ProductOrder> allProdOrders = new ArrayList<>(DatabaseUtil.GetAllProdOrders());
			Hashtable<String, Double> matOrdersDict = new Hashtable<>();

			for (MatOrder order : allMatOrders) {
				if (order.getSigned().contains("贾"))
					if (!matOrdersDict.contains(order.getName()))
						matOrdersDict.put(order.getName(), order.getKgAmount());
					else {
						double currentVal = matOrdersDict.get(order.getName());
						matOrdersDict.put(order.getName(), order.getKgAmount() + currentVal);
					}
			}

			for (ProductOrder order : allProdOrders) {
				int formulaIndex = order.getFormulaIndex();
				if (formulaIndex != -1 && order.getRemoteInt() == 1) {
					Formula formula = DatabaseUtil.GetFormulaByIndex(formulaIndex);
					inventoryHelper(matOrdersDict, formula, order, 1.0);
				}
			}

			// populating the table view
			TableColumn<Map.Entry<String, Double>, String> matNameCol = new TableColumn<>("原料名称");
			matNameCol.setStyle("-fx-alignment: CENTER;");
			matNameCol.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getKey()));

			TableColumn<Map.Entry<String, Double>, Double> amountLeft = new TableColumn<>("剩余总量");
			amountLeft.setStyle("-fx-alignment: CENTER;");
			amountLeft.setMinWidth(100);
			amountLeft.setCellValueFactory(param -> new SimpleDoubleProperty(
					Math.round(param.getValue().getValue() * 100.0) / 100.0).asObject());

			ObservableList<Map.Entry<String, Double>> items = FXCollections.observableArrayList(matOrdersDict.entrySet());
			inventoryTableView.setItems(items);
			inventoryTableView.getColumns().setAll(matNameCol, amountLeft);


		} catch (SQLException e) {
			AlertBox.display("错误", "张家港库存错误");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Recursive inventory helper function
	 *
	 * @param dict       the hashtable where changes are made
	 * @param formula    the current formula
	 * @param order      the original order
	 * @param percentage the current percentage from the top
	 */
	private void inventoryHelper(Hashtable<String, Double> dict, Formula formula, ProductOrder order, double percentage) {
		for (Formula item : formula.getFormulaList()) {
			inventoryHelper(dict, item, order, getPercentageOfFormula(item, formula));
			if (dict.containsKey(item.getName()) && item.getFormulaList().isEmpty()) {
				double currentVal = dict.get(item.getName());
				double newVal = getPercentageOfFormula(item, formula) * percentage * order.getKgAmount();
				dict.put(item.getName(), currentVal - newVal);
			}
		}
	}

	public void fillMatTable(ObservableList<MatOrder> orders) {

		Collection<TableColumn<MatOrder, ?>> orderColumnArrayList = new ArrayList<>();

		for (int i = 0; i < matHeaders.length; i++) {

			if (i == 8 || i == 9 || i == 10 || i == 11 || i == 12) {
				// Doubles
				TableColumn<MatOrder, Double> newColumn = new TableColumn<>(matHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(matProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				orderColumnArrayList.add(newColumn);
			} else if (i == 0 || i == 4 || i == 5 || i == 6) {
				// Main.Date
				TableColumn<MatOrder, Date> newColumn = new TableColumn<>(matHeaders[i]);
				newColumn.setMinWidth(110);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(matProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				orderColumnArrayList.add(newColumn);

			} else {
				TableColumn<MatOrder, String> newColumn = new TableColumn<>(matHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(matProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				orderColumnArrayList.add(newColumn);
			}
		}

		// if backspace or delete, delete the order
		matTableView.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.BACK_SPACE || keyEvent.getCode() == KeyCode.DELETE) {
				deleteMatOrder(matTableView.getSelectionModel().getSelectedItem());
			}
		});

		// if double clicked, enable edit
		matTableView.setRowFactory(tv -> {
			TableRow<MatOrder> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (!row.isEmpty())) {
					MatOrder order = row.getItem();
					modifyMatOrder(order);
				}
			});
			return row;
		});

		// filling the table
		matTableView.getColumns().setAll(orderColumnArrayList);
		matTableView.getItems().clear();
		matTableView.getItems().setAll(orders);
		matTableView.refresh();
	}

	/**
	 * Helper function set up new window to modify order
	 *
	 * @param selectedOrder the order to be updated
	 */
	private void modifyMatOrder(MatOrder selectedOrder) {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = Main.class.getResourceAsStream(Main.fxmlPath + "MatEditOrder.fxml");
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			MatEditOrder editOrderController = loader.getController();
			editOrderController.initData(selectedOrder, stage);

			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("编辑订单");

			Scene scene = new Scene(newScene);
			scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
			stage.setScene(scene);
			stage.showAndWait();

			matTableView.getItems().clear();
			matTableView.getItems().setAll(FinalConstants.updateAllMatOrders());
			matTableView.refresh();
		} catch (Exception e) {
			AlertBox.display("错误", "编辑订单窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Filling of the product table
	 *
	 * @param selectedProdOrders the orders specified
	 */
	public void fillProdTable(ObservableList<ProductOrder> selectedProdOrders) {
		// array of columns
		Collection<TableColumn<ProductOrder, ?>> productColumnArrayList = new ArrayList<>();

		// loop to set up all regular columns
		for (int i = 0; i < prodHeaders.length; i++) {
			if (i == 4 || i == 5 || i == 6 || i == 7 || i == 8 || i == 9) {
				// Doubles
				TableColumn<ProductOrder, Double> newColumn = new TableColumn<>(prodHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(prodProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				productColumnArrayList.add(newColumn);
			} else if (i == 0) {
				// Main.Date
				TableColumn<ProductOrder, Date> newColumn = new TableColumn<>(prodHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(prodProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				newColumn.setMinWidth(110);
				productColumnArrayList.add(newColumn);
			} else if (i == 11) {
				// remote
				TableColumn<ProductOrder, String> newColumn = new TableColumn<>(prodHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(prodProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				productColumnArrayList.add(newColumn);

			} else {
				// String
				TableColumn<ProductOrder, String> newColumn = new TableColumn<>(prodHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(prodProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				productColumnArrayList.add(newColumn);

			}
		}

		// if delete or backspace, delete order
		prodTableView.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.BACK_SPACE || keyEvent.getCode() == KeyCode.DELETE)
				deleteProdOrder(prodTableView.getSelectionModel().getSelectedItem());
		});

		// formula window
		prodTableView.setOnMouseClicked(keyEvent -> {
			if (keyEvent.getButton() == MouseButton.SECONDARY) {
				prodFormula(prodTableView.getSelectionModel().getSelectedItem());
			}
		});

		// if double clicked, enable edit
		prodTableView.setRowFactory(tv -> {
			TableRow<ProductOrder> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getButton() == MouseButton.SECONDARY) {
					if (row.getItem() != null) prodFormula(row.getItem());
				} else if (event.getClickCount() == 2 && (!row.isEmpty())) modifyProdOrder(row.getItem());
			});
			return row;
		});

		// filling the table
		prodTableView.getColumns().setAll(productColumnArrayList);
		prodTableView.getItems().clear();
		prodTableView.getItems().setAll(selectedProdOrders);
		prodTableView.refresh();
	}

	/**
	 * Helper function set up new window to modify order
	 *
	 * @param selectedOrder the order to be updated
	 */
	private void modifyProdOrder(ProductOrder selectedOrder) {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = Main.class.getResourceAsStream(Main.fxmlPath + "ProdEditOrder.fxml");
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			ProdEditOrder prodEditOrder = loader.getController();
			prodEditOrder.initData(selectedOrder, stage);

			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("编辑订单");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
			stage.setScene(scene);
			stage.showAndWait();
			prodTableView.getItems().clear();
			prodTableView.getItems().setAll(FinalConstants.updateAllProdOrders());
			prodTableView.refresh();
		} catch (Exception e) {
			AlertBox.display("错误", "窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Change all button's image and behaviour
	 */
	private void initButtons() {
		FinalConstants.setButtonImagesAndCursor(resetButton, FinalConstants.resetWhite, FinalConstants.resetBlack);
		FinalConstants.setButtonImagesAndCursor(searchButton, FinalConstants.searchWhite, FinalConstants.searchBlack);
		FinalConstants.setButtonImagesAndCursor(addButton, FinalConstants.addWhite, FinalConstants.addBlack);
		FinalConstants.setButtonImagesAndCursor(excelButton, FinalConstants.excelWhite, FinalConstants.excelBlack);
		FinalConstants.setButtonImagesAndCursor(quitButton, FinalConstants.quitWhite, FinalConstants.quitBlack);
	}

	/**
	 * Helper function to set up window to add a mat order
	 */
	private void addMatOrder() {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "MatAddOrder.fxml");
			Parent newScene = loader.load(fileInputStream);

			MatAddOrder matAddOrder = loader.getController();
			matAddOrder.initData(Main.mainStage);

			Main.mainStage.setTitle("添加订单");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
			Main.mainStage.setScene(scene);
			Main.mainStage.show();
		} catch (Exception e) {
			AlertBox.display("错误", "添加原料窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Helper function to set up window to add a mat seller
	 */
	private void addMatSeller() {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "MatAddSeller.fxml");
			Parent newScene = loader.load(fileInputStream);

			MatAddSeller matAddSeller = loader.getController();
			matAddSeller.initData(Main.mainStage);

			Main.mainStage.setTitle("添加供应商");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
			Main.mainStage.setScene(scene);
			Main.mainStage.show();
		} catch (Exception e) {
			AlertBox.display("错误", "添加供应商窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Helper function to set up window to add a prod order
	 */
	private void addProdOrder() {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "ProdAddOrder.fxml");
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			ProdAddOrder prodAddOrder = loader.getController();
			prodAddOrder.initData(stage);

			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("添加产品");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
			stage.setScene(scene);
			stage.showAndWait();
			prodTableView.getItems().clear();
			prodTableView.getItems().setAll(FinalConstants.updateAllProdOrders());
			prodTableView.refresh();
		} catch (Exception e) {
			AlertBox.display("错误", "添加产品订单窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Helper function to delete order
	 *
	 * @param selectedOrder the order to be deleted
	 */
	private void deleteMatOrder(MatOrder selectedOrder) {
		if (ConfirmBox.display("确认", "确定删除？", "是", "否")) {
			try {
				DatabaseUtil.DeleteMatOrder(selectedOrder.getSerialNum());
				matTableView.getItems().setAll(FinalConstants.updateAllMatOrders());
			} catch (SQLException e) {
				AlertBox.display("错误", "无法删除原料订单！");
				new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
						e.getMessage(), e.getStackTrace(), false);
			}
		}
	}

	/**
	 * Helper function to delete mat seller
	 *
	 * @param selectedSeller the order to be deleted
	 */
	private void deleteMatSeller(MatSeller selectedSeller) {
		if (ConfirmBox.display("确认", "确定删除？", "是", "否")) {
			try {
				DatabaseUtil.DeleteMatSeller(selectedSeller.getSellerId());
				matSellerTableView.getItems().setAll(FinalConstants.updateAllMatSellers());
			} catch (SQLException e) {
				AlertBox.display("错误", "无法删除原料供应商！");
				new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
						e.getMessage(), e.getStackTrace(), false);
			}
		}
	}

	/**
	 * Helper function to delete order
	 *
	 * @param selectedOrder the order to be deleted
	 */
	private void deleteProdOrder(ProductOrder selectedOrder) {
		if (ConfirmBox.display("确认", "确定删除？", "是", "否")) {
			try {
				DatabaseUtil.DeleteProdOrder(selectedOrder.getSerialNum());
				prodTableView.getItems().clear();
				prodTableView.getItems().setAll(FinalConstants.updateAllProdOrders());
			} catch (SQLException e) {
				AlertBox.display("错误", "无法删除产品订单！");
				new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
						e.getMessage(), e.getStackTrace(), false);
			}
		}
	}

	/**
	 * Helper function to set up window for advance/precision searching of mat order
	 */
	private void searchMatOrder() {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "MatSearchOrder.fxml");
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			MatSearchOrder matSearchOrder = loader.getController();
			matSearchOrder.initData(stage, this);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("搜索订单");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
			stage.setScene(scene);
			stage.show();
		} catch (Exception e) {
			AlertBox.display("错误", "原料搜索窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Helper function to set up window for advance/precision searching of prod order
	 */
	private void searchProdOrder() {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "ProdSearchOrder.fxml");
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			ProdSearchOrder prodSearchOrder = loader.getController();
			prodSearchOrder.initData(stage, this);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("搜索产品");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
			stage.setScene(scene);
			stage.show();
		} catch (Exception e) {
			AlertBox.display("错误", "产品搜索窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Helper function to reset the table to all orders
	 */
	private void resetTable() {
		try {
			matSearchTextField.clear();
			prodSearchTextField.clear();
			matUnitPriceSearchTextField.clear();
			prodUnitPriceSearchTextField.clear();

			matTableView.getItems().clear();
			matTableView.getItems().setAll(FinalConstants.updateAllMatOrders());
			matTableView.refresh();

			matSellerTableView.getItems().clear();
			matSellerTableView.getItems().setAll(FinalConstants.updateAllMatSellers());
			matSellerTableView.refresh();

			prodTableView.getItems().clear();
			prodTableView.getItems().setAll(FinalConstants.updateAllProdOrders());
			prodTableView.refresh();

			matUnitPriceTableView.getItems().clear();
			matUnitPriceTableView.getItems().setAll(DatabaseUtil.GetAllMatUnitPrice());
			matUnitPriceTableView.refresh();

			prodUnitPriceTableView.getItems().clear();
			prodUnitPriceTableView.getItems().setAll(DatabaseUtil.GetAllProdUnitPrice());
			prodUnitPriceTableView.refresh();

		} catch (Exception e) {
			AlertBox.display("错误", "重置表格错误，无法读取数据库！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Public function for other controller to call, to set the table with new list
	 *
	 * @param newList the search result list
	 */
	public void setMatSearchList(ObservableList<MatOrder> newList) {
		ObservableList<MatOrder> searchList = FXCollections.observableArrayList(newList);
		matTableView.getItems().clear();
		matTableView.getItems().setAll(searchList);
	}

	/**
	 * Public function for other controller to call, to set the table with new list
	 *
	 * @param newList the search result list
	 */
	public void setProdSearchList(ObservableList<ProductOrder> newList) {
		ObservableList<ProductOrder> searchList = FXCollections.observableArrayList(newList);
		prodTableView.getItems().clear();
		prodTableView.getItems().setAll(searchList);
	}

	/**
	 * Load the formula screen for a product
	 *
	 * @param order the order selected
	 */
	public void prodFormula(ProductOrder order) {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "ProdFormula.fxml");
			Parent newScene = loader.load(fileInputStream);

			ProdFormula prodFormula = loader.getController();
			prodFormula.init(order, null, null);
			Main.mainStage.setTitle("编辑配方");

			Scene scene = new Scene(newScene);
			scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
			Main.mainStage.setScene(scene);
		} catch (Exception e) {
			AlertBox.display("错误", "配方窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Load FXML for unit price table
	 */
	private void initMatUnitPrice() {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "MatUnitPriceTable.fxml");
			Parent vbox = loader.load(fileInputStream);

			MatUnitPriceTable matUnitPriceTable = loader.getController();
			matUnitPriceSearchTextField = matUnitPriceTable.getSearchBarTextField();
			matUnitPriceTableView = matUnitPriceTable.getMatTable();

			matUnitPriceTab.setContent(vbox);
		} catch (Exception e) {
			AlertBox.display("错误", "原料单价窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Load FXML for unit price table
	 */
	private void initProdUnitPrice() {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "ProdUnitPriceTable.fxml");
			Parent vbox = loader.load(fileInputStream);

			ProdUnitPriceTable prodUnitPriceTable = loader.getController();
			prodUnitPriceSearchTextField = prodUnitPriceTable.getSearchBarTextField();
			prodUnitPriceTableView = prodUnitPriceTable.getProdTable();

			prodUnitPriceTab.setContent(vbox);
		} catch (Exception e) {
			AlertBox.display("错误", "产品单价窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Function to generate excel file with material
	 */
	private void generateMatExcel() {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "MatGenerateExcel.fxml");
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			MatGenerateExcel matGenerateExcel = loader.getController();
			matGenerateExcel.initData(stage);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("生成表格");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
			stage.setScene(scene);
			stage.show();
		} catch (Exception e) {
			AlertBox.display("错误", "原料生产表格窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Function to generate excel file with product
	 */
	private void generateProdExcel() {
		try {
			FXMLLoader loader = new FXMLLoader();
			InputStream fileInputStream = getClass().getResourceAsStream(Main.fxmlPath + "ProdGenerateExcel.fxml");
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			ProdGenerateExcel prodGenerateExcel = loader.getController();
			prodGenerateExcel.initData(stage);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("生成表格");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add(Main.class.getResource(Main.styleSheetPath).toURI().toString());
			stage.setScene(scene);
			stage.show();
		} catch (Exception e) {
			AlertBox.display("错误", "产品生产表格窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Get percentage of current formula
	 *
	 * @param item          the current item
	 * @param entireFormula the current formula
	 * @return the percentage
	 */
	private double getPercentageOfFormula(Formula item, Formula entireFormula) {
		return item.getAmount() / getFormulaTotalAmount(entireFormula);
	}

	/**
	 * Adds all the amount
	 *
	 * @param formula the formula
	 * @return the total amount within the formula given
	 */
	private double getFormulaTotalAmount(Formula formula) {
		double formulaTotalAmount = 0;
		for (Formula item : formula.getFormulaList()) {
			formulaTotalAmount += item.getAmount();
		}
		return formulaTotalAmount;
	}


}