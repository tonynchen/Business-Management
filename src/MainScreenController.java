import javafx.collections.*;
import javafx.fxml.*;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Callback;

import java.io.FileInputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.function.Predicate;


public class MainScreenController implements Initializable {

	// table headers
	private static final String[] tableHeaders = new String[] {"订单号", "原料名称", "类别", "订单日期", "付款日期",
			"到达日期", "发票日期", "发票编号", "规格", "数量", "公斤", "单价", "总价", "签收人", "供应商订单编号", "供应商",
			"联系人", "手机", "座机", "传真", "供应商账号", "供应商银行地址", "供应商地址", "备注"};

	// all property listed
	private static final String[] propertyHeaders = new String[]{"sku", "name", "type", "orderDate", "paymentDate",
			"arrivalDate", "invoiceDate", "invoice", "unitAmount", "amount", "kgAmount", "unitPrice", "totalPrice",
			"signed", "skuSeller", "company", "contact", "mobile", "land", "fax", "account",
			"bank", "address", "note"};

	private ObservableList<MatOrder> allMatOrderList;
	private ObservableList<MatOrder> tempQuickSearchMatOrderList;
	private ObservableList<MatOrder> searchList;
	public boolean alreadyInitialize = false;

	@FXML TableView<ProductOrder> orderTableView;
	@FXML TableView<MatOrder> matTableView;
	@FXML Button searchButton;
	@FXML Button addButton;
	@FXML Button quitButton;
	@FXML Button resetButton;
	@FXML TextField searchBarTextField;
	@FXML ImageView searchImageView;

	/**
	 * Call to fill the table with all orders, set up actions for all buttons, set up search bars, set up image view
	 * @param url N/A
	 * @param resourceBundle N/A
	 */
	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		// setting up the image for search bar
		try {
			FileInputStream input = new FileInputStream("src/iconmonstr-magnifier-4-240.png");
			Image searchBarImage = new Image(input);
			searchImageView.setImage(searchBarImage);
		} catch (Exception e) {
			e.printStackTrace();
			HandleError error = new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
			error.WriteToLog();
		}

		// filling the table
		try {
			allMatOrderList = DatabaseUtil.GetAllMatOrders();
			tempQuickSearchMatOrderList = FXCollections.observableArrayList();
			tempQuickSearchMatOrderList.addAll(allMatOrderList);
			fillMatTable(allMatOrderList);
		} catch (SQLException e) {
			e.printStackTrace();
			HandleError error = new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
			error.WriteToLog();
			AlertBox.display("错误", "无法读取数据！");
			return;
		}

		searchButton.setOnAction(event -> {
			searchOrder();
		});

		addButton.setOnAction(event -> {
			addOrder();
		});

		quitButton.setOnAction(actionEvent -> {
			System.exit(1);
		});

		resetButton.setOnAction(actionEvent -> {
			resetTable();
		});

		// listener for search bar text field
		searchBarTextField.textProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue == null || newValue.equals("")) {
				matTableView.getItems().clear();
				tempQuickSearchMatOrderList = FXCollections.observableArrayList(allMatOrderList);
				matTableView.setItems(tempQuickSearchMatOrderList);
			} else {

				// if user deleted char
				if (newValue.length() < oldValue.length()) {
					matTableView.getItems().clear();
					tempQuickSearchMatOrderList = FXCollections.observableArrayList(allMatOrderList);
				}

				tempQuickSearchMatOrderList.removeIf(new Predicate<MatOrder>() {
					@Override
					public boolean test(MatOrder matOrder) {

						// remove if order doesn't contain the new value
						if (!matOrder.toString().contains(newValue)) return true;
						else return false;
					}
				});
				matTableView.setItems(tempQuickSearchMatOrderList);
			}
		});
	}

	/**
	 * Filling of the material table
	 * @param selectedMatOrders the orders specified
	 */
	public void fillMatTable(ObservableList<MatOrder> selectedMatOrders) {

		// array of columns
		Collection<TableColumn<MatOrder, ?>> orderColumnArrayList = new ArrayList<>();

		// setting up first action column
		TableColumn actionColumn = new TableColumn("动作");
		actionColumn.setSortable(false);
		orderColumnArrayList.add(actionColumn);

		// loop to set up all regular columns
		for (int i = 0; i < tableHeaders.length; i++) {
			if (i == 8 || i == 9 || i == 10 || i == 11 || i == 12) {
				// Doubles
				TableColumn<MatOrder, Double> newColumn = new TableColumn<>(tableHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(propertyHeaders[i]));
				newColumn.setStyle( "-fx-alignment: CENTER;");
				orderColumnArrayList.add(newColumn);
			} else if (i == 3 || i == 4 || i == 5 || i == 6) {
				// Date
				TableColumn<MatOrder, Date> newColumn = new TableColumn<>(tableHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(propertyHeaders[i]));
				newColumn.setStyle("-fx-alignment: CENTER;");
				newColumn.setMinWidth(100);
				orderColumnArrayList.add(newColumn);
			} else if (i == 13) {
				// signed by increase column width
				TableColumn<MatOrder, String> newColumn = new TableColumn<>(tableHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(propertyHeaders[i]));
				newColumn.setStyle( "-fx-alignment: CENTER;");
				newColumn.setMinWidth(60);
				orderColumnArrayList.add(newColumn);
			}
			else {
				// String
				TableColumn<MatOrder, String> newColumn = new TableColumn<>(tableHeaders[i]);
				newColumn.setCellValueFactory(new PropertyValueFactory<>(propertyHeaders[i]));
				newColumn.setStyle( "-fx-alignment: CENTER;");
				orderColumnArrayList.add(newColumn);
			}
		}

		// Setting a call back to handle the first column of action buttons
		Callback<TableColumn<MatOrder, String>, TableCell<MatOrder, String>> cellFactory = new Callback<>() {
					@Override
					public TableCell<MatOrder, String> call(TableColumn<MatOrder, String> matOrderStringTableColumn) {
						TableCell<MatOrder, String> cell = new TableCell<>() {
							// define new buttons
							Button edit = new Button("编辑");
							Button delete = new Button("删除");
							HBox actionButtons = new HBox(edit, delete);

							@Override
							public void updateItem(String item, boolean empty) {
								super.updateItem(item, empty);
								if (empty) {
									setGraphic(null);
								} else {
									edit.setOnAction(event -> modifyMatOrder(getTableView().getItems().get(getIndex())));
									delete.setOnAction(event -> deleteMatOrder(getTableView().getItems().get(getIndex())));
									edit.getStyleClass().add("actionButtons");
									delete.getStyleClass().add("actionButtons");
									actionButtons.setSpacing(5);
									actionButtons.setAlignment(Pos.CENTER);
									setGraphic(actionButtons);
								}
								setText(null);
							}
						};
						return cell;
					}
				};

		actionColumn.setCellFactory(cellFactory);

		// filling the table
		matTableView.setItems(selectedMatOrders);
		matTableView.getColumns().setAll(orderColumnArrayList);

		// if double clicked, enable edit
		matTableView.setRowFactory( tv -> {
			TableRow<MatOrder> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
					MatOrder order = row.getItem();
					modifyMatOrder(order);
				}
			});
			return row;
		});
	}

	/**
	 * Helper function to delete order
	 * @param selectedOrder the order to be deleted
	 */
	private void deleteMatOrder(MatOrder selectedOrder) {
		if (ConfirmBox.display("确认", "确定删除？", "确定", "取消")) {
			try {
				DatabaseUtil.DeleteMatOrder(selectedOrder.getSerialNum());
				allMatOrderList = DatabaseUtil.GetAllMatOrders();
				matTableView.getItems().clear();
				matTableView.getItems().setAll(allMatOrderList);
			} catch (SQLException e) {
				AlertBox.display("错误", "无法删除");
				e.printStackTrace();
				HandleError error = new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
						e.getMessage(), e.getStackTrace(), false);
				error.WriteToLog();
			}
		}
	}

	/**
	 * Helper function set up new window to modify order
	 * @param selectedOrder the order to be updated
	 */
	private void modifyMatOrder(MatOrder selectedOrder) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("MatEditOrder.fxml"));
			Parent newScene = loader.load();
			Stage stage = new Stage();

			MatEditOrderController editOrderController = loader.getController();
			editOrderController.initData(selectedOrder, stage);

			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("编辑订单");
			stage.setScene(new Scene(newScene));
			stage.showAndWait();
			matTableView.getItems().clear();
			allMatOrderList = DatabaseUtil.GetAllMatOrders();
			matTableView.getItems().setAll(allMatOrderList);
			matTableView.refresh();
		} catch (Exception e) {
			AlertBox.display("错误", "窗口错误");
			e.printStackTrace();
			HandleError error = new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
			error.WriteToLog();
		}
	}

	/**
	 * Helper function to set up window to add an order
	 */
	private void addOrder() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("MatAddOrder.fxml"));
			Parent newScene = loader.load();
			Stage stage = new Stage();

			MatAddOrderController matAddOrderController = loader.getController();
			matAddOrderController.initData(stage);

			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("添加订单");
			stage.setScene(new Scene(newScene));
			stage.showAndWait();
			matTableView.getItems().clear();
			allMatOrderList = DatabaseUtil.GetAllMatOrders();
			matTableView.getItems().setAll(allMatOrderList);
			matTableView.refresh();
		} catch (Exception e) {
			AlertBox.display("错误", "窗口错误");
			e.printStackTrace();
			HandleError error = new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
			error.WriteToLog();
		}
	}

	/**
	 * Helper function to set up window for advance/precision searching
	 */
	private void searchOrder() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("MatSearchOrder.fxml"));
			Parent newScene = loader.load();
			Stage stage = new Stage();

			MatSearchOrderController matSearchOrderController = loader.getController();
			matSearchOrderController.initData(stage, this);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("搜索订单");
			stage.setScene(new Scene(newScene));
			stage.show();
		} catch (Exception e) {
			AlertBox.display("错误", "窗口错误");
			e.printStackTrace();
			HandleError error = new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
			error.WriteToLog();
		}
	}

	/**
	 * Helper function to reset the table to all orders
	 */
	private void resetTable() {
		try {
			searchBarTextField.setText("");
			matTableView.getItems().clear();
			allMatOrderList = DatabaseUtil.GetAllMatOrders();
			matTableView.getItems().setAll(allMatOrderList);
			matTableView.refresh();
			alreadyInitialize = false;
		} catch (Exception e) {
			AlertBox.display("错误", "无法摘取信息");
			e.printStackTrace();
			HandleError error = new HandleError(getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(),
					e.getMessage(), e.getStackTrace(), false);
			error.WriteToLog();
		}
	}

	/**
	 * Public function for other controller to call, to set the table with new list
	 * @param newList
	 */
	public void setSearchList(ObservableList<MatOrder> newList) {
		searchList = FXCollections.observableArrayList(newList);
		matTableView.getItems().clear();
		matTableView.getItems().setAll(searchList);
	}

}