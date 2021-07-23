package utils;


import main.SkednetCalendar;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utils {
    public final static String[] MONTHS = new String[]{"januari", "februari", "maart", "april", "mei", "juni", "juli",
            "augustus", "september", "oktober", "november", "december"};

    /**
     * returns corresponding int (march = 3, september = 9, ect.)
     */
    public static int maandToInt(String maand) {
        for (int i = 0; i < MONTHS.length; i++) {
            if (maand.equalsIgnoreCase(MONTHS[i])) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * returns property from config.properties
     */
    public static String getProperty(String key){
        try (InputStream inputStream = Utils.class.getResourceAsStream(SkednetCalendar.PROPERTIES_FILE)){
            Properties prop = new Properties();
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException();
            }
            return prop.getProperty(key);
        } catch (FileNotFoundException e) {
            Logger.e("Utils:getProperty", "Property file not found, did you rename the default?");
        } catch (IOException e) {
            Logger.e("Utils:getProperty", e);
        }
        return "";
    }
}
