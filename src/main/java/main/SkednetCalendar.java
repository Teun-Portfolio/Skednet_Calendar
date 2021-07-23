package main;

import com.google.api.services.calendar.model.Event;
import models.Shift;
import networking.CalendarAPI;
import networking.SkednetAPI;
import org.apache.http.auth.AuthenticationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import utils.Logger;
import utils.Utils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class SkednetCalendar {
    public static final String PROPERTIES_FILE = "/config.properties";

    public static void main(String[] args) {
        // get user and pass from property
        String user = Utils.getProperty("skednetUsername");
        String pass = Utils.getProperty("skednetPassword");
        if(user.isEmpty() && pass.isEmpty()){
            // user and pass not retrieved
            return;
        }

        // login to skednet and get schedule
        Document schedule = null;
        try{
            SkednetAPI skednetAPI = new SkednetAPI(user, pass);
            String rawResponse = skednetAPI.getSchedule();
            schedule = Jsoup.parse(rawResponse);
        } catch (AuthenticationException | IOException e) {
            Logger.e("SkednetCalendar", e.getMessage());
            return;
        }

        // parse events
        List<Event> shifts = new ArrayList<>();
        for (Shift shift : Shift.parseHTML(schedule)) {
            shifts.add(shift.getEvent());
        }

        // replace events in calendar with new schedule
        try {
            new CalendarAPI().replaceEvents(shifts);
        } catch (IOException | GeneralSecurityException e) {
            Logger.e("SkednetCalendar", e.getMessage());
        }
    }
}
