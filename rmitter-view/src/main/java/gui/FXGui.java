package gui;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import rmi.rmitter.control.RemoteController;
import rmi.rmitter.model.*;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by affo on 04/04/18.
 */
public class FXGui extends Application
        implements FeedObserver, UserObserver, EventHandler<ActionEvent> {
    private RemoteController controller;
    private TextField username, token, post;
    private Button login;
    private TextArea feedArea;
    private Text title;

    private String currentToken;

    /**
     * This is the main for JavaFX applications
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        Registry registry = LocateRegistry.getRegistry();
        UnicastRemoteObject.exportObject(this, 0);

        // gets a reference for the remote controller
        this.controller = (RemoteController) registry.lookup("controller");

        Scene mainScene = draw();
        primaryStage.setTitle("FX-RMItter");
        primaryStage.setScene(mainScene);
        primaryStage.setResizable(false);
        addListeners();
        primaryStage.show();
        primaryStage.setOnCloseRequest((event -> {
            try {
                UnicastRemoteObject.unexportObject(this, true);
            } catch (NoSuchObjectException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }));
    }

    private Scene draw() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        //grid.setGridLinesVisible(true);

        username = new TextField();
        token = new TextField();
        post = new TextField();
        login = new Button("Login");
        feedArea = new TextArea();
        title = new Text("RMItter");

        feedArea.setEditable(false);
        feedArea.setWrapText(true);

        ScrollPane scrollPane = new ScrollPane(feedArea);

        // placing components in the grid
        grid.add(title, 0, 0, 3, 1);
        title.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

        GridPane.setHalignment(title, HPos.CENTER);

        grid.add(username, 0, 1);
        grid.add(token, 1, 1);
        grid.add(login, 2, 1);

        GridPane.setHalignment(login, HPos.CENTER);

        grid.add(scrollPane, 0, 2, 3, 1);
        grid.add(post, 0, 3, 3, 1);

        return new Scene(grid);
    }

    private void addListeners() {
        login.setOnAction(this);
        post.setOnAction(this);
    }

    private void writeToFeed(String message) {
        feedArea.appendText(message + "\n");
    }

    @Override
    public void handle(ActionEvent event) {
        Object source = event.getSource();
        try {
            if (source.equals(login)) {
                String token = this.token.getText();
                String username = this.username.getText();
                controller.observeUser(token, username, this, this);
                login.setDisable(true);
                writeToFeed(">>> [OK]: Logged in as @" + username);
                this.currentToken = token;
            } else if (source.equals(post)) {
                if (currentToken != null) {

                    controller.post(currentToken, post.getText());

                }
                post.setText("");
            } else {
                throw new IllegalStateException("Unknown event source: " + source);
            }
        } catch (RemoteException e) {
            writeToFeed(">>> [ERROR]: " + e.getCause().getMessage());
        }
    }

    // ---------------------------------- Remote events

    @Override
    public void onFolloweePost(Post post) throws RemoteException {
        writeToFeed("@" + post.getPoster().getUsername() + " -> " + post.getContent());
    }

    @Override
    public void onHashtagUse(Hashtag hashtag, Post post) throws RemoteException {
        writeToFeed("Hashtag " + hashtag + " used:");
        writeToFeed(post.getContent());
    }

    @Override
    public void onNewPost(Post post) throws RemoteException {
        writeToFeed(post.getContent());
    }

    @Override
    public void onMention(Post post) throws RemoteException {
        writeToFeed("You were mentioned in:");
        writeToFeed(post.getContent());
    }

    @Override
    public void onFollower(User user) throws RemoteException {
        writeToFeed("New follower: " + user.getUsername());
    }
}
