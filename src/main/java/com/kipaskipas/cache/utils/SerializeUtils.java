package com.kipaskipas.cache.utils;

import java.io.*;

/**
 * thank you to:
 * https://www.programmersought.com/article/6064205150/
 */

public class SerializeUtils {
    public static byte[] serialize(Object obj) throws IOException {
        byte[] bytes = null;
        ByteArrayOutputStream baos=new ByteArrayOutputStream();;
        ObjectOutputStream oos=new ObjectOutputStream(baos);
        oos.writeObject(obj);
        bytes=baos.toByteArray();
        baos.close();
        oos.close();
        return bytes;
    }
    public static Object deSerialize(byte[] bytes) {
        Object obj = null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            obj = ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }
}
