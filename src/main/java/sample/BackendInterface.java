package sample;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import map.Map;
import map.Place;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import social.User;
import xmlSaver.XmlDeserializer;
import xmlSaver.XmlSerializer;
import xmlSaver.XmlSerializerRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class BackendInterface {
    private final GraphicsContext context;
    private Render render;
    private Object ui_inst;
    private Map map;
    private List<User> users;
    private User user1;

    public static BackendInterface getBackendInterface(GraphicsContext context) {
        var backendInterface = new BackendInterface(context);
        try {
            backendInterface.init(null);
        } catch (ClassNotFoundException |
                NoSuchMethodException |
                NoSuchFieldException |
                IllegalAccessException |
                InvocationTargetException |
                InstantiationException e) {
            e.printStackTrace();
            return null;
        }
        return backendInterface;
    }

    public static BackendInterface getBackendInterfaceFromFile(BackendInterface otherInterface, GraphicsContext context) {
        otherInterface.render.stopRendering();
        var backendInterface = new BackendInterface(context);
        var loadedUiInstance = otherInterface.loadUiInstance();
        try {
            backendInterface.init(loadedUiInstance);
        } catch (ClassNotFoundException |
                NoSuchMethodException |
                NoSuchFieldException |
                IllegalAccessException |
                InvocationTargetException |
                InstantiationException e) {
            e.printStackTrace();
            return null;
        }
        return backendInterface;
    }

    private BackendInterface(GraphicsContext context) {
        this.context = context;
    }

    private void init(Object loadedUiInstance) throws ClassNotFoundException,
            NoSuchMethodException,
            NoSuchFieldException,
            IllegalAccessException,
            InvocationTargetException,
            InstantiationException
    {
        var ui = Class.forName("ConsoleUI"); //получаем класс консольUI
        // создаем объект класса consoleUI
        this.ui_inst = loadedUiInstance != null ? loadedUiInstance : ui.getDeclaredConstructor().newInstance();
        this.map = (Map)ui.getDeclaredField("map").get(this.ui_inst); // получаем карту (поле map)
        //noinspection unchecked
        this.users = (List<User>)ui.getDeclaredField("users").get(this.ui_inst);
        this.user1 = (User)Utils.getPrivateField(this.ui_inst, "user1");
        this.render = new Render(this.context, this.map);
        this.render.startRendering(); // запускаем рендер
    }

    //вызываем через рефлексию метод live() из бэкенда
    public void invokeLive() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        var ui = Class.forName("ConsoleUI");
        var live = ui.getDeclaredMethod("live");

        live.setAccessible(true);
        live.invoke(ui_inst);
    }

    public Stream<Triplet<User, Color, List<Pair<Place, String>>>> getAliveUsers() {
        return this.users.
                stream().
                filter(User::isAlive).
                map(x -> Triplet.with(x, render.getColourResolver().resolve(x), x.getLog()));
    }

    private Object loadUiInstance() {
        return XmlDeserializer.loadXml("test.xml", (XmlSerializerRegistry) Utils.getPrivateField(this.ui_inst, "registry"));
    }

    public void saveUiInstance() {
        XmlSerializer.saveXml(this.ui_inst, "test.xml", (XmlSerializerRegistry) Utils.getPrivateField(this.ui_inst, "registry"));
    }

    public Consumer<Place> getCheckInCallback() {
        return (place) -> Utils.checkIn(user1, this.map, place);
    }

    public List<Place> getPlaces() {
        return map.getPlaces();
    }
}
