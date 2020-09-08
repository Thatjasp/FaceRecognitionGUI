package application;
	
import java.io.File;
import java.net.URL;

import org.opencv.core.Core;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;


public class Main extends Application {
	FXMLLoader loader = new FXMLLoader(getClass().getResource("JavaFXGUI.fxml"));
	GUIController control;
	
	
	@Override
	public void start(Stage primaryStage) {
		String iconPath = "resources/icon/eyeIcon.png";
		File file = new File(iconPath);
		Image icon = new Image(file.toURI().toString());
		
		try {
			ImageView currentFrame = new ImageView();
			Pane pane = new Pane();
			
			BorderPane root = loader.load();
			pane = (Pane)root.getCenter();
			for(Node frame:pane.getChildren()) {
				currentFrame = (ImageView) frame;
			}
			currentFrame.fitHeightProperty().bind(pane.heightProperty());
			currentFrame.fitWidthProperty().bind(pane.widthProperty());
			
			
			
			Scene scene = new Scene(root,600,600);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
			primaryStage.setTitle("Face Recognition App");
			primaryStage.getIcons().add(icon);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void stop() {
		control = (GUIController) loader.getController();
		control.endCamera();
		Platform.exit();
	}
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		launch(args);
	}
}
