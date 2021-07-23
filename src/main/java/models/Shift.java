package models;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.Logger;
import utils.Utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class Shift {
    private final ZonedDateTime start;
    private final ZonedDateTime end;
    private final String description;
    private final String location;

    public Shift(ZonedDateTime start, ZonedDateTime end, String description, String location) {
        this.start = start;
        this.end = end;
        this.description = description;
        this.location = location;
    }

    /**
     * returns a google event with the data of this shift
     */
    public Event getEvent() {
        Event event = new Event()
                .setSummary(description)
                .setLocation(location);

        // set time of the event
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZZZZZ");

        DateTime startDateTime = new DateTime(start.format(format));
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(TimeZone.getDefault().getID());
        event.setStart(start);

        DateTime endDateTime = new DateTime(end.format(format));
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(TimeZone.getDefault().getID());
        event.setEnd(end);

        // set a reminder popup
        int minBefore = Integer.parseInt(Utils.getProperty("minBefore"));
        if (minBefore > 0) {
            EventReminder[] reminderOverrides = new EventReminder[]{
                    new EventReminder().setMethod("popup").setMinutes(minBefore),
            };
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);
        }

        return event;
    }

    /**
     * returns list of shifts from skednet html
     */
    public static List<Shift> parseHTML(Document html) {
        // name of html classes
        String dateName = "shift-date";
        String locationAndDescriptionName = "shift-location";
        String timeName = "shift-time";

        List<Shift> shiftList = new ArrayList<>(); // list containing all events
        Element schedule = html.getElementById("schedule");
        Elements shifts = schedule.getElementsByClass("shift");
        for (Element shift : shifts) {
            // get date
            String[] dateStrings = shift.getElementsByClass(dateName).text().split(" ");
            int day = Integer.parseInt(dateStrings[1]);
            int month = Utils.maandToInt(dateStrings[2]);
            LocalDate now = LocalDate.now();
            int year = now.getYear();
            if (now.getMonthValue() < month - 1) {
                // adds a year if the month of the shift is earlier then the current one (december -> january)
                year++;
            }
            LocalDate date = LocalDate.of(year, month, day);

            // get time
            String[] timeStrings = shift.getElementsByClass(timeName).text().split(" ");
            LocalTime startTime = LocalTime.parse(timeStrings[0]);
            LocalTime endTime = LocalTime.parse(timeStrings[2]);
            // combine date and time and zone
            ZoneId zone = ZoneId.systemDefault();
            ZonedDateTime startDateTime = ZonedDateTime.of(date.atTime(startTime), zone);
            ZonedDateTime endDateTime = ZonedDateTime.of(date.atTime(endTime), zone);
            // checks if end time is before starting time if so it ends the next day
            if (endTime.isBefore(startTime)) {
                endDateTime = endDateTime.plusDays(1);
            }

            // get location and description
            String[] descStrings = shift.getElementsByClass(locationAndDescriptionName).text().split(" ");
            String description = descStrings[2];
            String location = descStrings[0];

            // try to find corresponding address in config file
            String address = Utils.getProperty(location);
            if (address.isEmpty()) {
                // add shift to list with city name
                shiftList.add(new Shift(startDateTime, endDateTime, description, location));
            } else {
                // add shift to list with address
                shiftList.add(new Shift(startDateTime, endDateTime, description, address));
            }
        }
        Logger.i("Shift:parseHTML", String.format("Parsed %d shifts", shiftList.size()));
        return shiftList;
    }
}
