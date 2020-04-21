package sample;

import com.google.common.collect.Streams;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import map.Map;
import map.Place;
import map.buildings.Edge;
import map.buildings.Point;
import map.buildings.Polygon;
import map.buildings.Type;
import org.javatuples.Pair;
import social.User;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

public class Render {
    private static final double FRAME_RATE = 24;
    private static final Random RANDOM = new Random();

    private final GraphicsContext gc;
    private final Map map;
    private final Thread renderingTask;
    private final PlaceRenderer placeRenderer;
    private final People2ColourResolver colourResolver;

    public People2ColourResolver getColourResolver() {
        return this.colourResolver;
    }

    public Render(GraphicsContext gc, Map map) {
        this.gc = gc;
        this.map = map;
        this.renderingTask = new Thread(this::renderingLoop);
        this.renderingTask.setDaemon(true);
        this.placeRenderer = new PlaceRenderer();
        this.colourResolver = new People2ColourResolver();

        this.gc.setStroke(Color.BLACK);
        this.gc.setFill(Color.BLACK);
        this.gc.setLineWidth(3.0);
        this.gc.setFont(new Font(Font.getDefault().getName(), 10));
    }

    private void renderingLoop() {
        var ms = System.currentTimeMillis();
        try
        {
            Thread.sleep(3000);
            while (true) {
                this.draw();
                var toSleep = (long) Math.max((1000 / FRAME_RATE) - (System.currentTimeMillis() - ms), 0);
                //noinspection BusyWait
                Thread.sleep(toSleep);
                ms = System.currentTimeMillis();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void draw() {
        this.gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        this.map.getPlaces().forEach(placeRenderer::pass);
    }

    public void startRendering() {
        this.renderingTask.start();
    }

    public void stopRendering() { this.renderingTask.interrupt(); }

    public static class People2ColourResolver {
        private final HashMap<User, Color> peopleColours;

        private People2ColourResolver() {
            this.peopleColours = new HashMap<>();
        }

        public Color resolve(User people) {
            var rndClr = Color.color(0.25 + RANDOM.nextDouble() / 2, 0.25 + RANDOM.nextDouble() / 2, 0.25 + RANDOM.nextDouble() / 2);
            return this.peopleColours.containsKey(people) ? this.peopleColours.get(people) : (this.peopleColours.put(people, rndClr) == null ? rndClr : rndClr);
        }
    }

    private class PlaceRenderer {
        private final HashMap<Class<? extends Type>, BiFunction<GraphicsContext, Place, Pair<Double, Double>>> actionMap;

        private PlaceRenderer() {
            this.actionMap = new HashMap<>();

            this.actionMap.put(Point.class, (g, p) -> {
                var point = (Point)p.getType();
                var x = (double) Utils.getPrivateField(point, "x") * 96;
                var y = (double) Utils.getPrivateField(point, "y") * 54;
                g.fillOval(x, y, 15, 15);
                return Pair.with(x, y);
            });

            this.actionMap.put(Polygon.class, (g, p) -> {
                var poly = (Polygon)p.getType();
                //noinspection unchecked
                var edges = (List<Edge>) Utils.getPrivateField(poly, "edges");
                var avg = edges.stream().map(x -> {
                    var p1 = x.getP1();
                    var p2 = x.getP2();
                    var x0 = (double) Utils.getPrivateField(p1, "x") * 96;
                    var y0 = (double) Utils.getPrivateField(p1, "y") * 54;
                    var x1 = (double) Utils.getPrivateField(p2, "x") * 96;
                    var y1 = (double) Utils.getPrivateField(p2, "y") * 54;
                    g.strokeLine(x0, y0, x1, y1);
                    return Pair.with((x0 + x1) / 2, (y0 + y1) / 2);
                }).reduce(
                        Pair.with((double)0, (double)0), (prev, curr) ->
                                Pair.with(prev.getValue0() + curr.getValue0(), prev.getValue1() + curr.getValue1())
                );
                var x = avg.getValue0() / edges.size();
                var y = avg.getValue1() / edges.size();
                return Pair.with(x, y);
            });
        }

        private void pass(Place place) {
            this.actionMap.
                    entrySet().
                    stream().
                    filter(x -> x.getKey().isInstance(place.getType())).
                    map(java.util.Map.Entry::getValue).
                    findAny().
                    ifPresentOrElse(
                            fu -> {
                                var center = fu.apply(Render.this.gc, place);
                                var x = center.getValue0();
                                var y = center.getValue1();
                                Render.this.gc.fillText(place.getName(), x, y);
                                //noinspection UnstableApiUsage,ResultOfMethodCallIgnored
                                Streams.mapWithIndex(
                                        Utils.getPeopleInThisPlaceRecently(Render.this.map, place).
                                                stream(),
                                        (k, v) -> {
                                            Render.this.gc.setFill(Render.this.colourResolver.resolve(k));
                                            Render.this.gc.fillOval(x + v * 15, y - 15, 12, 9);
                                            Render.this.gc.setFill(Color.BLACK);
                                            return v;
                                        }
                                ).toArray();
                            },
                            () -> System.out.printf("cannot find action of %s\n", place.getType())
                    );
        }
    }
}
