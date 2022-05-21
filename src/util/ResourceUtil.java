package util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import main.entry;

public class ResourceUtil {
    //load resource
    public static String loadStringData(String resourceName) {
        InputStream is = entry.class.getResourceAsStream(resourceName);
        Scanner scanner = new Scanner(is);
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNextLine()) {
            sb.append(scanner.nextLine());
        }
        scanner.close();
        return sb.toString();
    }
    //load lines
    public static List<String> loadLines(String resourceName) {
        InputStream is = inputStreamFromResource(resourceName);
        Scanner scanner = new Scanner(is);
        List<String> lines = new ArrayList<String>();
        while (scanner.hasNextLine()) {
            lines.add(scanner.nextLine());
        }
        scanner.close();
        return lines;
    }
    //load image as byte array
    public static byte[] loadImageData(String resourceName) {
        InputStream is = entry.class.getResourceAsStream(resourceName);
        int size = 0;
        try {
            size = is.available();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] data = new byte[size];
        try {
            is.read(data);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
    //input stream from resource
    public static InputStream inputStreamFromResource(String resourceName) {
        return entry.class.getResourceAsStream(resourceName);
    }
}
