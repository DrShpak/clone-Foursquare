package sample;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import map.Place;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import social.User;
import xmlSaver.XmlSerializerRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
        primaryStage.setTitle("gui");
        primaryStage.setScene(new Scene(root, 800, 600));
        //noinspection Convert2Lambda
        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<>() {
            @Override
            public void handle(WindowEvent window) {
                serve(root);
            }
        });
        primaryStage.show();
    }

    /*
    окошко с логом посещений кокнреетго юзера
    вся логика выполнена  методами джавафх
    то есть тут просто инициализируем окошко и все
     */
    public void startLogModal(Stream<String> logs, Window win) throws Exception {
        AnchorPane root = FXMLLoader.load(getClass().getResource("/logModal.fxml"));
        VBox vBox = (VBox) root.getChildren().get(0);
        Stage primaryStage = new Stage();
        primaryStage.setTitle("user log");
        primaryStage.setScene(new Scene(root, 600, 400));
        //noinspection Convert2Lambda
        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<>() {
            @Override
            public void handle(WindowEvent window) {
                vBox.getChildren().addAll(logs.map(x -> {
                    var label = new Label(x);
                    label.setMinWidth(600);
                    label.setPrefWidth(600);
                    label.setMaxWidth(600);
                    return label;
                }).toArray(Label[]::new));
            }
        });
        primaryStage.initModality(Modality.WINDOW_MODAL);
        primaryStage.initOwner(win);
        primaryStage.show();
    }

    /*
    окошко с юзерами
    выводим юзеров с их цветами и с вомзожностью прсомотра лога
     */
    public void startUsersModal(Stream<Triplet<User, Color, List<Pair<Place, String>>>> aliveUsers, Window win) throws Exception {
        AnchorPane root = FXMLLoader.load(getClass().getResource("/usersModal.fxml"));
        VBox vBox = (VBox) root.getChildren().get(0);
        Stage primaryStage = new Stage();
        primaryStage.setTitle("user list");
        primaryStage.setScene(new Scene(root, 600, 400));
        //noinspection Convert2Lambda
        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<>() {
            @Override
            public void handle(WindowEvent window) {
                vBox.getChildren().addAll(aliveUsers.map(x -> {
                    var label = new Label(x.getValue0().getName());
                    label.setTextFill(x.getValue1());
                    label.onMouseClickedProperty().setValue(ex -> {
                        try {
                            startLogModal(
                                x.getValue2().
                                    stream().
                                    map(y -> String.format("%s at %s", y.getValue0().getName(), y.getValue1())),
                                ((Node) ex.getSource()).getScene().getWindow()
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    return label;
                }).toArray(Label[]::new));
            }
        });
        primaryStage.initModality(Modality.WINDOW_MODAL);
        primaryStage.initOwner(win);
        primaryStage.show();
    }

    /*
    делаем чекин
    выводим окошко с доступными для чекина местами
    тут в 1 части иницализируем окшоко, сцену и стэйдж
    во второй части в методе handle() выводим все места и вызываем код метода функционального инфтерфейса Consumer
    в этом методе accept() мы написали код метода checkIn() из бэкенда
     */
    public void startCheckInModal(Consumer<Place> callback, List<Place> places, Window win) throws Exception {
        AnchorPane root = FXMLLoader.load(getClass().getResource("/checkInModal.fxml"));
        VBox vBox = (VBox) root.getChildren().get(0);
        Stage primaryStage = new Stage();
        primaryStage.setTitle("check in");
        primaryStage.setScene(new Scene(root, 600, 400));
        //noinspection Convert2Lambda
        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<>() {
            @Override
            public void handle(WindowEvent window) {
                vBox.getChildren().addAll(places.stream().map(x -> {
                    var label = new Label(x.getName());
                    label.onMouseClickedProperty().setValue(ex -> {
                        callback.accept(x);
                        primaryStage.close();
                    });
                    return label;
                }).toArray(Label[]::new));
            }
        });
        primaryStage.initModality(Modality.WINDOW_MODAL);
        primaryStage.initOwner(win);
        primaryStage.show();
    }

    /*
    метод в котором инициализируем все кнопочки и их логику
    логику для всех методов (чекин, сейв, лоад и тд) получаем с помощью РЕФЛЕКСИИ в классе BackendInterface
     */
    private void serve(Parent root) {
        var buttons = ((HBox) ((VBox) ((ScrollPane) root).getContent()).getChildren().get(0)).getChildren();
        var canvas = (Canvas) ((VBox) ((ScrollPane) root).getContent()).getChildren().get(1);
        var context = canvas.getGraphicsContext2D();
        //noinspection FieldMayBeFinal
        var backendReference = new Object() {
            BackendInterface backend = BackendInterface.getBackendInterface(context); // создаем объект класса BackendInterface
        };

        var button_addUser = (Button) buttons.get(0); // добавить юзера
        button_addUser.onActionProperty().setValue(ex -> {
            var t = new Thread(() -> {
                try {
                    assert backendReference.backend != null;
                    // логику метода live вызываем через рефлексию
                    // во вспомоагтельном классе BackendInterface
                    backendReference.backend.invokeLive();
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
            t.setDaemon(true);
            t.start();
        });

        var button_getUsers = (Button) buttons.get(1); // получить всех юзеров
        button_getUsers.onActionProperty().setValue(ex -> {
            try {
                assert backendReference.backend != null;
                // логика получения живых юзеров реализована в бэкенде
                // вызывае эти методы посредством вспомогательного класса BackendInterface
                var aliveUsers = backendReference.backend.getAliveUsers(); // получаем живых юзеров
                startUsersModal(aliveUsers, ((Node) ex.getSource()).getScene().getWindow());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


        // так же метод для чекина получаем с помощью рефлексии (см класс BackendInterface)
        var button_checkIn = (Button) buttons.get(2);
        button_checkIn.onActionProperty().setValue(ex -> {
            try {
                assert backendReference.backend != null;
                startCheckInModal(backendReference.backend.getCheckInCallback(), backendReference.backend.getPlaces(), ((Node) ex.getSource()).getScene().getWindow());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        var button_load = (Button) buttons.get(3);
        button_load.onActionProperty().setValue(
            ex -> backendReference.backend =
                BackendInterface.getBackendInterfaceFromFile(backendReference.backend, context)
        );

        var button_save = (Button) buttons.get(4);
        button_save.onActionProperty().setValue(ex -> {
            assert backendReference.backend != null;
            backendReference.backend.saveUiInstance();
        });
    }

    public static final XmlSerializerRegistry registry;

    static {
        registry = new XmlSerializerRegistry();
        try {
            registry.addClass(
                Pair.class,
                () -> Pair.with(new Object(), new Object()),
                Pair.class.getDeclaredField("val0"),
                Pair.class.getDeclaredField("val1")
            );
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        try {
            registry.addClass(
                Date.class,
                Date::new,
                Date.class.getDeclaredField("fastTime")
            );
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
