package application.main;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import application.extend.AlertExtend;
import application.extend.ListViewExtend;
import application.logic.Compress;
import application.logic.CompressObserver;
import application.logic.MainLogic;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController implements Initializable {

	/* 全局的Stage对象 */
	private static Stage primaryStage;

	/* 当前ListView选中项 */
	private File currentFile;

	/* 文件选择器返回的文件列表 */
	private List<File> fileList;

	/* ListView的一些扩展方法类 */
	private static ListViewExtend<File> listViewExtend = new ListViewExtend<File>();

	/* 保存路径 */
	private File savePath;

	/* 覆盖原文件按钮是否选中 */
	private Boolean isOverWriteCheckBoxSelected = false;

	/* 开始压缩按钮点击计数 */
	private int clickCount = 0;

	@FXML
	private ListView<File> fileListView;

	@FXML
	private TextField savePathTextField;

	@FXML
	private CheckBox overWriteCheckBox;

	@FXML
	private Button savePathChooseButton;

	@FXML
	private Button startCompressButton;

	@FXML
	private ProgressBar compressProgressBar;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		primaryStage = new Main().getPrimaryStage();
		fileListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			currentFile = newValue;
		});
	}

	/**
	 * 打开文件选择器
	 * 
	 * @param event
	 */
	public void showFileChooseDialog(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("选择文件");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("所有文件", "*.*"),
				new FileChooser.ExtensionFilter("JPG", "*.jpg"), new FileChooser.ExtensionFilter("JPEG", "*.jpeg"),
				new FileChooser.ExtensionFilter("PNG", "*.png"), new FileChooser.ExtensionFilter("BMP", "*.bmp"));
		List<File> fList = fileChooser.showOpenMultipleDialog(primaryStage);

		if (fList != null && fList.size() > 0) {
			fileList = fList;
			if (MainLogic.hasNonImage(fileList)) {
				fileList = MainLogic.nonImageFilte(fileList);
				AlertExtend.showInformation(null, "存在不支持的文件格式，已自动过滤");
			}
			listViewExtend.setItems(fileListView, fileList);
		}
	}

	/**
	 * 打开文件保存选择器
	 * 
	 * @param event
	 */
	public void showFileSaveDialog(ActionEvent event) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("保存");
		File sPath = directoryChooser.showDialog(primaryStage);
		if (sPath != null && sPath.exists() && sPath.isDirectory()) {
			savePath = sPath;
			savePathTextField.setText(savePath.getPath());
		}
	}

	/**
	 * 打开文件夹选择器
	 * 
	 * @param event
	 */
	public void showDirectoryChooseDialog(ActionEvent event) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("选择文件夹");
		File file = directoryChooser.showDialog(primaryStage);

		if (file != null && file.exists() && file.isDirectory()) {
			List<File> fList = MainLogic.getFilelist(file);
			if (fList != null && fList.size() > 0) {
				fileList = fList;
				if (MainLogic.hasNonImage(fileList)) {
					fileList = MainLogic.nonImageFilte(fileList);
					AlertExtend.showInformation(null, "存在不支持的文件格式，已自动过滤");
				}
				listViewExtend.setItems(fileListView, fileList);
			}
		}
	}

	/**
	 * 覆盖原文件
	 * 
	 * @param event
	 */
	public void overWriteAction(ActionEvent event) {
		clickCount = 0;
		startCompressButton.setText("开始压缩");
		if (overWriteCheckBox.isSelected()) {
			isOverWriteCheckBoxSelected = true;
			savePathTextField.disableProperty().set(true);
			savePathChooseButton.disableProperty().set(true);
		} else {
			isOverWriteCheckBoxSelected = false;
			savePathTextField.disableProperty().set(false);
			savePathChooseButton.disableProperty().set(false);
		}
	}

	/**
	 * 开始压缩
	 * 
	 * @param event
	 */
	public void startCompress(ActionEvent event) {
		if (fileList != null && fileList.size() > 0) {
			// 判断覆盖或者另存为
			if (isOverWriteCheckBoxSelected) {
				clickCount++;
				if (clickCount == 1) {
					startCompressButton.setText("请再次点击按钮开始压缩");
				} else if (clickCount == 2) {
					clickCount = 0;
					startCompressButton.setText("正在压缩...");
					startCompressButton.disableProperty().set(true);
					CompressObserver pgbCompressObs = new CompressObserver() {
						@Override
						public void onProgressChange(double progress) {
							compressProgressBar.setProgress(progress);
						}
					};
					CompressObserver btnCompressObs = new CompressObserver() {
						@Override
						public void onProgressChange(double progress) {
							if (progress == 1) {
								startCompressButton.setText("开始压缩");
								startCompressButton.disableProperty().set(false);
								AlertExtend.showInformation(null, "压缩完成");
							}
						}
					};
					Compress compress = new Compress(fileList, 1f, 0.3f, null);
					compress.addObserver(pgbCompressObs);
					compress.addObserver(btnCompressObs);
					new Thread(compress).start();
				}
			} else {
				if (savePath == null) {
					AlertExtend.showInformation(null, "请选择目标文件夹");
				} else {
					if (!savePath.exists()) {
						savePath.mkdirs();
					}
					startCompressButton.setText("正在压缩...");
					startCompressButton.disableProperty().set(true);
					CompressObserver pgbCompressObs = new CompressObserver() {
						@Override
						public void onProgressChange(double progress) {
							compressProgressBar.setProgress(progress);
						}
					};
					CompressObserver btnCompressObs = new CompressObserver() {
						@Override
						public void onProgressChange(double progress) {
							if (progress == 1) {
								startCompressButton.setText("开始压缩");
								startCompressButton.disableProperty().set(false);
								AlertExtend.showInformation(null, "压缩完成");
							}
						}
					};
					Compress compress = new Compress(fileList, 1f, 0.3f, savePath);
					compress.addObserver(pgbCompressObs);
					compress.addObserver(btnCompressObs);
					new Thread(compress).start();
				}
			}
		} else {
			AlertExtend.showInformation(null, "请先添加文件");
		}
	}

	/**
	 * 清空列表
	 * 
	 * @param event
	 */
	public void clearList(ActionEvent event) {
		listViewExtend.clear();
		fileListView.setItems(null);
		fileList = null;
	}

	/**
	 * 移除选中
	 * 
	 * @param event
	 */
	public void removeSelectedItem(ActionEvent event) {
		listViewExtend.removeItem(fileListView, currentFile);
	}

	/**
	 * 高级设置
	 */
	public void advancedConfig() {
		AlertExtend.showInformation(null, "高级功能尚开发");
	}

}