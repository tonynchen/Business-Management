package Main;

// from my other packages
import CustomEditingCells.*;
import Material.*;
import Product.*;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.*;
import javafx.util.Callback;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
			"规格", "数量", "公斤", "单价", "金额", "成本价", "备注"};

	// all prod property listed
	private static final String[] prodProperty = new String[]{"orderDate", "sku", "customer", "name",
			"unitAmount", "amount", "kgAmount", "unitPrice", "totalPrice", "basePrice", "note"};

	private ObservableList<MatOrder> tempQuickSearchMatOrderList;

	private ObservableList<ProductOrder> tempQuickSearchProdOrderList;

	@FXML
	TabPane mainTabPane;
	@FXML
	Tab matTab;
	@FXML
	Tab prodTab;
	@FXML
	TableView<ProductOrder> prodTableView;
	@FXML
	TableView<MatOrder> matTableView;
	@FXML
	TableView inventoryTableView;
	@FXML
	Button searchButton;
	@FXML
	Button addButton;
	@FXML
	Button quitButton;
	@FXML
	Button resetButton;
	@FXML
	Button excelButton;
	@FXML
	Button matUnitPriceButton;
	@FXML
	Button prodUnitPriceButton;
	@FXML
	TextField searchBarTextField;
	@FXML
	ImageView searchImageView;

	/**
	 * Call to fill the table with all orders, set up actions for all buttons, set up search bars, set up image view
	 *
	 * @param url            N/A
	 * @param resourceBundle N/A
	 */
	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		resetButton.setText("重置\n表格");
		searchButton.setText("精确\n搜索");
		addButton.setText("添加\n编辑");
		excelButton.setText("生成\n表格");
		matUnitPriceButton.setText("原料\n单价");
		prodUnitPriceButton.setText("产品\n单价");

		// setting up the image for search bar
		try {
			FileInputStream input = new FileInputStream("searchIcon.png");
			Image searchBarImage = new Image(input);
			searchImageView.setImage(searchBarImage);
		} catch (Exception e) {
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}

		fillMatTable(FinalConstants.updateAllMatOrders());
		fillProdTable(FinalConstants.updateAllProdOrders());

		// filling the mat table
		tempQuickSearchMatOrderList = FXCollections.observableArrayList();
		tempQuickSearchMatOrderList.addAll(FinalConstants.allMatOrders);
		tempQuickSearchProdOrderList = FXCollections.observableArrayList();
		tempQuickSearchProdOrderList.addAll(FinalConstants.allProdOrders);

		// precision search mat/prod orders
		searchButton.setOnAction(event -> {
			// if selected tab is material
			if (mainTabPane.getSelectionModel().getSelectedItem().equals(matTab)) searchMatOrder();
				// if selected tab is product
			else searchProdOrder();
		});

		// add mat/prod orders
		addButton.setOnAction(event -> {
			// if selected tab is material
			if (mainTabPane.getSelectionModel().getSelectedItem().equals(matTab)) addMatOrder();
				// if selected tab is product
			else addProdOrder();
		});

		// quit the application
		quitButton.setOnAction(actionEvent -> {
			Path source = Paths.get("BusinessCashFlow.db");
			Path target = Paths.get(System.getProperty("user.home") + "/BusinessCashFlow.db");
			try {
				if (Files.exists(target)) Files.delete(target);
				Files.copy(source, target);
			} catch (IOException e) {
				new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
						e.getMessage(), e.getStackTrace(), false);
			}
			Main.mainStage.close();
		});

		// reset both table
		resetButton.setOnAction(actionEvent -> resetTable());

		// excel button
		excelButton.setOnAction(event -> {
			// if selected tab is material
			if (mainTabPane.getSelectionModel().getSelectedItem().equals(matTab)) generateMatExcel();
				// if selected tab is product
			else generateProdExcel();
		});


		matUnitPriceButton.setOnAction(event -> loadMatUnitPrice());

		prodUnitPriceButton.setOnAction(event -> loadProdUnitPrice());

		// listener for search bar text field
		searchBarTextField.textProperty().addListener((observableValue, oldValue, newValue) -> {

			// if selected tab is material
			if (mainTabPane.getSelectionModel().getSelectedItem().equals(matTab)) {

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
			}

			// if selected tab is order
			else if (mainTabPane.getSelectionModel().getSelectedItem().equals(prodTab)) {

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
			}
		});

		mainTabPane.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.intValue() == 2) {
				initInventory();
			}
		});
	}

	/**
	 * Fill the inventory page
	 */
	public void initInventory() {
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
				if (formulaIndex != -1) {
					Formula formula = DatabaseUtil.GetFormulaByIndex(formulaIndex);
					inventoryHelper(matOrdersDict, formula, order, 1.0);
				}
			}

			// populating the tableview
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

	/**
	 * Filling of the material table
	 *
	 * @param selectedMatOrders the orders specified
	 */
	public void fillMatTable(ObservableList<MatOrder> selectedMatOrders) {

		// array of columns
		Collection<TableColumn<MatOrder, ?>> orderColumnArrayList = new ArrayList<>();

		Callback<TableColumn<MatOrder, String>, TableCell<MatOrder, String>> stringEditableFactory =
				p -> new EditingCellWithTextFields<>(String.class) {};
		Callback<TableColumn<MatOrder, Double>, TableCell<MatOrder, Double>> doubleEditableFactory =
				p -> new EditingCellWithTextFields<>(Double.class) {};
		Callback<TableColumn<MatOrder, String>, TableCell<MatOrder, String>> comboBoxEditableFactory =
				p -> new EditingCellForMatOfType<>() {};
		Callback<TableColumn<MatOrder, Date>, TableCell<MatOrder, Date>> datePickerEditableFactory =
				p -> new EditingCellWithDatePicker<>() {};
		Callback<TableColumn<MatOrder, String>, TableCell<MatOrder, String>> matSellerEditableFactory =
				p -> new EditingCellForMatSeller<>() {};

		// loop to set up all regular columns
		for (int i = 0; i < matHeaders.length; i++) {
			if (i == 8 || i == 9 || i == 10 || i == 11 || i == 12) {
				// Doubles
				TableColumn<MatOrder, Double> newColumn = new TableColumn<>(matHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(matProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				orderColumnArrayList.add(newColumn);

				if (i != 10 && i != 12) {
					int matPropertyIndex = i;
					newColumn.setCellFactory(doubleEditableFactory);
					newColumn.setOnEditCommit(event -> {

						if (event.getNewValue().equals(Double.MAX_VALUE)) {
							AlertBox.display("错误", "数字输入格式错误！");
							return;
						}

						MatOrder editingOrder = event.getTableView().getItems().get(event.getTablePosition().getRow());
						try {
							Method setter;
							setter = MatOrder.class.getDeclaredMethod("set" +
									Character.toUpperCase(matProperty[matPropertyIndex].charAt(0)) +
									matProperty[matPropertyIndex].substring(1), double.class);
							setter.invoke(editingOrder, event.getNewValue().doubleValue());
							editingOrder.setKgAmount();
							editingOrder.setTotalPrice();
							DatabaseUtil.UpdateMatOrder(editingOrder);
							matTableView.refresh();
						} catch (SQLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
							e.printStackTrace();
							AlertBox.display("错误", "编辑订单错误！(数字）");
							new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
									e.getMessage(), e.getStackTrace(), false);
						}
					});
				}
			} else if (i == 0 || i == 4 || i == 5 || i == 6) {
				// Main.Date
				TableColumn<MatOrder, Date> newColumn = new TableColumn<>(matHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(matProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				newColumn.setMinWidth(110);
				orderColumnArrayList.add(newColumn);

				int matPropertyIndex = i;
				newColumn.setCellFactory(datePickerEditableFactory);
				newColumn.setOnEditCommit(event -> {
					MatOrder editingOrder = event.getTableView().getItems().get(event.getTablePosition().getRow());
					try {
						System.out.println(event.getNewValue());
						Method setter;
						setter = MatOrder.class.getDeclaredMethod("set" +
								Character.toUpperCase(matProperty[matPropertyIndex].charAt(0)) +
								matProperty[matPropertyIndex].substring(1), Date.class);
						setter.invoke(editingOrder, event.getNewValue());
						DatabaseUtil.UpdateMatOrder(editingOrder);
					} catch (SQLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
						AlertBox.display("错误", "编辑订单错误！(日期）");
						e.printStackTrace();
						new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
								e.getMessage(), e.getStackTrace(), false);
					}
				});
			} else if (i == 13) {
				// signed by increase column width
				TableColumn<MatOrder, String> newColumn = new TableColumn<>(matHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(matProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				newColumn.setMinWidth(60);
				orderColumnArrayList.add(newColumn);
				int matPropertyIndex = i;

				// Set up for editable table view
				newColumn.setCellFactory(stringEditableFactory);
				newColumn.setOnEditCommit(event -> {
					MatOrder editingOrder = event.getTableView().getItems().get(event.getTablePosition().getRow());
					try {
						Method setter;
						setter = MatOrder.class.getDeclaredMethod("set" +
								Character.toUpperCase(matProperty[matPropertyIndex].charAt(0)) +
								matProperty[matPropertyIndex].substring(1), String.class);
						setter.invoke(editingOrder, event.getNewValue());
						DatabaseUtil.UpdateMatOrder(editingOrder);
					} catch (SQLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
						AlertBox.display("错误", "编辑订单错误！(文字）");
						new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
								e.getMessage(), e.getStackTrace(), false);
					}
				});
			} else {
				// String
				TableColumn<MatOrder, String> newColumn = new TableColumn<>(matHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(matProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				orderColumnArrayList.add(newColumn);

				// Set up for editable table view
				// NOTES: Not allowing to edit sellers, Mat Of Type and Seller needs combo Box
				if ((i <= 14 || i == 23) && i != 3) {
					int matPropertyIndex = i;
					newColumn.setCellFactory(stringEditableFactory);
					newColumn.setOnEditCommit(event -> {
						MatOrder editingOrder = event.getTableView().getItems().get(event.getTablePosition().getRow());
						try {
							Method setter;
							setter = MatOrder.class.getDeclaredMethod("set" +
									Character.toUpperCase(matProperty[matPropertyIndex].charAt(0)) +
									matProperty[matPropertyIndex].substring(1), String.class);
							setter.invoke(editingOrder, event.getNewValue());
							DatabaseUtil.UpdateMatOrder(editingOrder);
						} catch (SQLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
							AlertBox.display("错误", "编辑订单错误！(文字）");
							new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
									e.getMessage(), e.getStackTrace(), false);
						}
					});
				} else if (i == 15) {
					newColumn.setCellFactory(matSellerEditableFactory);
					newColumn.setOnEditCommit(event -> {
						MatOrder editingOrder = event.getTableView().getItems().get(event.getTablePosition().getRow());

						MatSeller selectedSeller = new MatSeller(SerialNum.getSerialNum(DBOrder.SELLER), "temp");
						for (MatSeller matSeller : FinalConstants.updateAllMatSellers()) {
							if (matSeller.getCompanyName().equals(event.getNewValue()))
								selectedSeller = matSeller;
						}
						editingOrder.setSeller(selectedSeller);
						matTableView.refresh();
					});
				} else if (i == 3) {
					int matPropertyIndex = i;
					newColumn.setCellFactory(comboBoxEditableFactory);
					newColumn.setOnEditCommit(event -> {
						MatOrder editingOrder = event.getTableView().getItems().get(event.getTablePosition().getRow());
						try {
							Method setter;
							setter = MatOrder.class.getDeclaredMethod("set" +
									Character.toUpperCase(matProperty[matPropertyIndex].charAt(0)) +
									matProperty[matPropertyIndex].substring(1), String.class);
							setter.invoke(editingOrder, event.getNewValue());
							DatabaseUtil.UpdateMatOrder(editingOrder);
						} catch (SQLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
							AlertBox.display("错误", "编辑订单错误！(文字）");
							new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
									e.getMessage(), e.getStackTrace(), false);
						}
					});
				}
			}
		}

		// if backspace or delete, delete the order
		matTableView.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.BACK_SPACE || keyEvent.getCode() == KeyCode.DELETE) {
				deleteMatOrder(matTableView.getSelectionModel().getSelectedItem());
			}
		});

		// if double clicked, enable edit
//		matTableView.setRowFactory(tv -> {
//			TableRow<MatOrder> row = new TableRow<>();
//			row.setOnMouseClicked(event -> {
//				if (event.getClickCount() == 2 && (!row.isEmpty())) editFocusedCell();
//			});
//			return row;
//		});


		matTableView.setEditable(true);
		matTableView.getSelectionModel().cellSelectionEnabledProperty().set(true);

		// filling the table
		matTableView.getColumns().setAll(orderColumnArrayList);
		matTableView.getItems().clear();
		matTableView.getItems().setAll(selectedMatOrders);
		matTableView.refresh();

	}

	/**
	 * Function called when cell is selected and user is typing
	 */
	@SuppressWarnings("unchecked")
	private void editFocusedCell() {
		final TablePosition <MatOrder, ?> focusedCell =
				matTableView.focusModelProperty().get().focusedCellProperty().get();
		matTableView.edit(focusedCell.getRow(), focusedCell.getTableColumn());
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
			} else {
				// String
				TableColumn<ProductOrder, String> newColumn = new TableColumn<>(prodHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(prodProperty[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				productColumnArrayList.add(newColumn);
			}
		}

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

		// if delete or backspace, delete order
		prodTableView.setOnKeyReleased(keyEvent -> {
			if (keyEvent.getCode() == KeyCode.BACK_SPACE || keyEvent.getCode() == KeyCode.DELETE)
				deleteProdOrder(prodTableView.getSelectionModel().getSelectedItem());
		});

		// filling the table
		prodTableView.getColumns().setAll(productColumnArrayList);
		prodTableView.getItems().clear();
		prodTableView.getItems().setAll(selectedProdOrders);
		prodTableView.refresh();
	}

	/**
	 * Helper function to set up window to add a mat order
	 */
	private void addMatOrder() {
		try {
			FXMLLoader loader = new FXMLLoader();
			FileInputStream fileInputStream = new FileInputStream(new File(Main.fxmlPath + "MatAddOrderModifySeller.fxml"));
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			MatAddOrderModifySeller matAddOrderModifySeller = loader.getController();
			matAddOrderModifySeller.initData(stage);

			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("添加订单");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add("file:///" + Main.styleSheetPath);
			stage.setScene(scene);
			stage.showAndWait();
			matTableView.getItems().clear();
			matTableView.getItems().setAll(FinalConstants.updateAllMatOrders());
			matTableView.refresh();
		} catch (Exception e) {
			AlertBox.display("错误", "添加原料窗口错误！");
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
			FileInputStream fileInputStream = new FileInputStream(new File(Main.fxmlPath + "ProdAddOrder.fxml"));
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			ProdAddOrder prodAddOrder = loader.getController();
			prodAddOrder.initData(stage);

			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("添加产品");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add("file:///" + Main.styleSheetPath);
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
	 * Helper function set up new window to modify order
	 *
	 * @param selectedOrder the order to be updated
	 */
	private void modifyMatOrder(MatOrder selectedOrder) {
		try {
			FXMLLoader loader = new FXMLLoader();
			FileInputStream fileInputStream = new FileInputStream(new File(Main.fxmlPath + "MatEditOrder.fxml"));
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			MatEditOrder editOrderController = loader.getController();
			editOrderController.initData(selectedOrder, stage);

			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("编辑订单");

			Scene scene = new Scene(newScene);
			scene.getStylesheets().add("file:///" + Main.styleSheetPath);
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
	 * Helper function set up new window to modify order
	 *
	 * @param selectedOrder the order to be updated
	 */
	private void modifyProdOrder(ProductOrder selectedOrder) {
		try {
			FXMLLoader loader = new FXMLLoader();
			FileInputStream fileInputStream = new FileInputStream(new File(Main.fxmlPath + "ProdEditOrder.fxml"));
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			ProdEditOrder prodEditOrder = loader.getController();
			prodEditOrder.initData(selectedOrder, stage);

			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("编辑订单");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add("file:///" + Main.styleSheetPath);
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
			FileInputStream fileInputStream = new FileInputStream(new File(Main.fxmlPath + "MatSearchOrder.fxml"));
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			MatSearchOrder matSearchOrder = loader.getController();
			matSearchOrder.initData(stage, this);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("搜索订单");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add("file:///" + Main.styleSheetPath);
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
			FileInputStream fileInputStream = new FileInputStream(new File(Main.fxmlPath + "ProdSearchOrder.fxml"));
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			ProdSearchOrder prodSearchOrder = loader.getController();
			prodSearchOrder.initData(stage, this);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("搜索产品");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add("file:///" + Main.styleSheetPath);
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
			searchBarTextField.setText("");

			matTableView.getItems().clear();
			matTableView.getItems().setAll(FinalConstants.updateAllMatOrders());
			matTableView.refresh();

			prodTableView.getItems().clear();
			prodTableView.getItems().setAll(FinalConstants.updateAllProdOrders());
			prodTableView.refresh();
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
			FileInputStream fileInputStream = new FileInputStream(new File(Main.fxmlPath + "ProdFormula.fxml"));
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			stage.setHeight(screenSize.height * 0.9);

			ProdFormula prodFormula = loader.getController();
			prodFormula.initData(order, stage);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("配方");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add("file:///" + Main.styleSheetPath);
			stage.setScene(scene);
			stage.showAndWait();
			resetTable();
		} catch (Exception e) {
			AlertBox.display("错误", "配方窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Load FXML for unit price table
	 */
	private void loadMatUnitPrice() {
		try {
			FXMLLoader loader = new FXMLLoader();
			FileInputStream fileInputStream = new FileInputStream(new File(Main.fxmlPath + "MatUnitPriceTable.fxml"));
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			MatUnitPriceTable matUnitPriceTable = loader.getController();
			matUnitPriceTable.initData(stage);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("原料单价系统");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add("file:///" + Main.styleSheetPath);
			stage.setScene(scene);
			stage.showAndWait();
			resetTable();
		} catch (Exception e) {
			AlertBox.display("错误", "原料单价窗口错误！");
			new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
		}
	}

	/**
	 * Load FXML for unit price table
	 */
	private void loadProdUnitPrice() {
		try {
			FXMLLoader loader = new FXMLLoader();
			FileInputStream fileInputStream = new FileInputStream(new File(Main.fxmlPath + "ProdUnitPriceTable.fxml"));
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			ProdUnitPriceTable prodUnitPriceTable = loader.getController();
			prodUnitPriceTable.initData(stage);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("产品单价系统");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add("file:///" + Main.styleSheetPath);
			stage.setScene(scene);
			stage.showAndWait();
			resetTable();
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
			FileInputStream fileInputStream = new FileInputStream(new File(Main.fxmlPath + "MatGenerateExcel.fxml"));
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			MatGenerateExcel matGenerateExcel = loader.getController();
			matGenerateExcel.initData(stage);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("生成表格");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add("file:///" + Main.styleSheetPath);
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
			FileInputStream fileInputStream = new FileInputStream(new File(Main.fxmlPath + "ProdGenerateExcel.fxml"));
			Parent newScene = loader.load(fileInputStream);
			Stage stage = new Stage();

			ProdGenerateExcel prodGenerateExcel = loader.getController();
			prodGenerateExcel.initData(stage);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("生成表格");
			Scene scene = new Scene(newScene);
			scene.getStylesheets().add("file:///" + Main.styleSheetPath);
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
