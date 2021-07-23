package networking;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import utils.Logger;
import utils.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class CalendarAPI {
    //region googleQuickstart
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = Utils.getProperty("credentialsFile");

    /**
     * Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = CalendarAPI.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    //endregion googleQuickstart

    private final Calendar service;
    private static final String CALENDAR_NAME = "Postillion";
    private final String calendarId;

    public CalendarAPI() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(Utils.getProperty("applicationName")).build();
        calendarId = getCalendarId(CALENDAR_NAME);
    }

    /**
     * returns calendar id by checking summary (usually calendar name)
     *
     * @param calendarSummary - The calendar name
     * @return - calendar Id
     */
    public String getCalendarId(String calendarSummary) {
        // Iterate through entries in calendar list
        String pageToken = null;
        do {
            try {
                CalendarList calendarList = service.calendarList().list().setPageToken(pageToken).execute();
                List<CalendarListEntry> items = calendarList.getItems();
                for (CalendarListEntry calendarListEntry : items) {
                    if (calendarListEntry.getSummary().equalsIgnoreCase(calendarSummary)) {
                        return calendarListEntry.getId();
                    }
                }
                pageToken = calendarList.getNextPageToken();
            } catch (IOException e) {
                Logger.e("CalendarAPI:getCalendarId", e.getMessage());
            }
        } while (pageToken != null);
        Logger.w("CalendarAPI:getCalendarId", String.format("No calendar found with summary: %s", calendarSummary));
        return "";
    }

    /**
     * removes all events from calendar
     *
     * @param calendarId -
     */
    public void clearSecondaryCalendar(String calendarId) {
        // Iterate over the events in the specified calendar
        String pageToken = null;
        do {
            try {
                Events events = service.events().list(calendarId).setPageToken(pageToken).execute();
                List<Event> items = events.getItems();
                // delete individual events
                for (Event event : items) {
                    try {
                        service.events().delete(calendarId, event.getId()).execute();
                    } catch (IOException e) {
                        Logger.e("CalendarAPI:clearSecondaryCalendar"
                                , String.format("Error deleting event %s", event.toPrettyString()), e);
                    }
                }
                pageToken = events.getNextPageToken();
            } catch (IOException e) {
                Logger.e("CalendarAPI:clearSecondaryCalendar", e);
            }
        } while (pageToken != null);
    }

    /**
     * Adds all events to a calendar
     *
     * @param calendarId -
     * @param eventList  -
     */
    public void insertEvents(String calendarId, List<Event> eventList) {
        for (Event event : eventList) {
            try {
                service.events().insert(calendarId, event).execute();
                Logger.i("CalendarAPI:insertEvents", String.format("Added event: %s %s"
                        , event.getSummary(), event.getStart()));
            } catch (IOException e) {
                Logger.e("CalendarAPI:insertEvents", e);
            }
        }
    }

    /**
     * removes all old events and replaces them with new list
     *
     * @param events -
     */
    public void replaceEvents(List<Event> events) {
        clearSecondaryCalendar(calendarId);
        Logger.i("CalendarAPI:replaceEvents", "Cleared all events");
        insertEvents(calendarId, events);
        Logger.i("CalendarAPI:insertEvents", "Added all events");
    }
}