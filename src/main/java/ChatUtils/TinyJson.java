package ChatUtils;


import ChatSchema.GroupMessage;
import ChatSchema.User;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiny json: a minimum implementation of Json
 *
 */
public class TinyJson implements Serializable {
    private Map<String, Object> members;

    private TinyJson(Map<String, Object> members) {
        this.members = members;
    }


    public TinyJson(String[] keys) {
        this.members = new HashMap<>();
        Arrays.stream(keys).forEach(str -> members.put(str, null));
    }

    /**
     * if this json object has some field identical to class T, then the values of the input object will be replaced
     *
     * @param emptyObj the object to be filled
     * @param <T>
     * @return the filled object, which is the same object as emptyObj
     */
    public <T> T fillObject(T emptyObj) {
        var type = emptyObj.getClass();
        var fields = type.getFields();
        try {
            emptyObj = (T) type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Casting failed" + type);
        }


        for (Field field : fields) {
            boolean canAccess = field.isAccessible();
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Object inJson = get(fieldName);
                if (inJson != null) {
                    try {
                        field.set(emptyObj, inJson);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e.getCause());
                    }
                }

                field.setAccessible(canAccess);
            }

        }

        return emptyObj;
    }


    public TinyJson(Object obj) {
        this.members = new HashMap<>();
        var objClass = obj.getClass();
        var fields = objClass.getFields();
        Arrays.stream(fields).forEach(field -> {
            boolean flag = field.isAccessible();
            if (!Modifier.isStatic(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    Object fieldObject = field.get(obj);

                    try {
                        if (fieldObject instanceof String || fieldObject == null) {
                            put(fieldName, fieldObject);
                        } else
                            putObject(fieldName, field.getType().cast(fieldObject));
                    } catch (ClassCastException classCastException) {
                        // it is a primitive
                        put(fieldName, fieldObject);
                    }

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                field.setAccessible(flag);
            }

        });
    }


    public TinyJson() {
        members = new HashMap<>();
    }




    public <T> TinyJson put(String key, T data) {
        members.put(key, data);
        return this;
    }

    public <T> TinyJson put(String key, T[] arr) {
        members.put(key, arr);
        return this;
    }

    private Object get(String key) {
        return members.get(key);
    }

    private TinyJson putObject(String key, Object data) {
        System.out.println("Last Put is Called!");
        members.put(key, new TinyJson(data));

        return this;
    }

    public TinyJson addEntry(String entryName) {
        this.members.put(entryName, null);
        return this;
    }


    public Object getPrimitive(String key) {
        return members.getOrDefault(key, null);
    }

    public TinyJson getObject(String key) {
        return  (TinyJson) members.getOrDefault(key, null);
    }


    @Override
    public String toString() {
        return members.toString();
    }


    public static void main(String[] args) {
        class test {
            public int id;
            public User user;
        }
        User user = new User(1);
        user.username = "hello-world";
        user.password = "password";
        var tes1 = new test();
        tes1.id = 888;
        tes1.user = user;


       TinyJson tinyJson = new TinyJson(new GroupMessage());


//        myJson.put("username", "User001");
        System.out.println(tinyJson);
//        System.out.println(myJson.getObject("user").getPrimitive("username"));
    }


}


