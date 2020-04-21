package sample;

import map.Map;
import map.Place;
import social.CheckIn;
import social.User;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Utils {
    private static boolean isFieldStatic(Field f) {
        return Modifier.isStatic(f.getModifiers());
    }

    //полуачаем приватное поле (расскажу в дисе)
    public static Object getPrivateField(Object object, String name) {
        Object v = null;
        try{
            var f = object.getClass().getDeclaredField(name);
            var accessibility = f.canAccess(isFieldStatic(f) ? null : object);
            f.setAccessible(true);
            v = f.get(isFieldStatic(f) ? null : object);
            f.setAccessible(accessibility);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return v;
    }

    // получаем метод, по его имени и аргументам в классе объекта Object
    @SuppressWarnings("UnusedReturnValue")
    public static Object invokeMethod(Object object, String name, Object... args) {
        Object v = null;
        try{
            var f = object.getClass().getDeclaredMethod(name, Arrays.stream(args).map(Object::getClass).toArray(Class[]::new)); //NoSuchFieldException
            var accessibility = f.canAccess(object);
            f.setAccessible(true);
            v = f.invoke(object, args);
            f.setAccessible(accessibility);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return v;
    }

    public static List<User> getPeopleInThisPlaceRecently(Map map, Place place) {
        return map.getCheckins().stream().
                filter(x -> x.getPlace().equals(place)).
                filter(x -> TimeUnit.MILLISECONDS.toSeconds(new Date().getTime() - x.getDate().getTime()) <= 15).
                map(CheckIn::getUser).
                collect(Collectors.toList());
    }

    public static void checkIn(User user, Map map, Place place) {
        var checkIn = new CheckIn(place, user, new Date());
        map.getCheckins().add(checkIn);
        Utils.invokeMethod(user, "sentNotification", checkIn);
        Utils.invokeMethod(user, "addInLog", place);
    }
}
