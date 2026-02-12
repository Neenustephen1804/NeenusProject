import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CalendarManager {

    private static final String APPLICATION_NAME = "Google Calendar Utility";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    // Emails for the meeting
    private static final String TEACHER_EMAIL = "test@gmail.com";
    private static final String YOUR_EMAIL = "neenustephen@gmail.com";

    public static void main(String... args) {
        try {
            // 1. Authenticate and get the service
            Calendar service = getCalendarService();

            // ==========================================
            //       CHOOSE YOUR ACTION BELOW
            // ==========================================

            //--- ACTION A: Create a New Meeting ---
            createWeeklyMeeting(service);

            // --- ACTION B: List Upcoming Events (Find the ID) ---
           //listUpcomingEvents(service);

            // --- ACTION C: Cancel a Meeting (Paste ID from Action B) ---
            //cancelEvent(service, "01p4m27nlb9c2d5a2d9c1u2o64_20260223T090000Z");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================================================
    // UTILITY METHOD 1: CREATE EVENT
    // ==================================================
    public static void createWeeklyMeeting(Calendar service) throws IOException {
        Event event = new Event()
                .setSummary("Weekly Check-in Reminder")
                .setDescription("Automated weekly reminder.");

        DateTime startDateTime = new DateTime("2026-02-16T10:00:00Z"); // UTC time
        event.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone("CST"));

        DateTime endDateTime = new DateTime("2026-02-16T10:30:00Z");
        event.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone("CST"));

        event.setRecurrence(Arrays.asList("RRULE:FREQ=WEEKLY;BYDAY=MO"));

        EventAttendee[] attendees = new EventAttendee[]{
                new EventAttendee().setEmail(TEACHER_EMAIL),
                new EventAttendee().setEmail(YOUR_EMAIL),
        };
        event.setAttendees(Arrays.asList(attendees));

        event = service.events().insert("primary", event)
                .setSendUpdates("all")
                .execute();

        System.out.printf("-> SUCCESS: Event created: %s\n", event.getHtmlLink());
    }

    // ==================================================
    // UTILITY METHOD 2: LIST EVENTS (To find IDs)
    // ==================================================
    public static void listUpcomingEvents(Calendar service) throws IOException {
        System.out.println("\n--- UPCOMING EVENTS ---");
        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = service.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        List<Event> items = events.getItems();
        if (items.isEmpty()) {
            System.out.println("No upcoming events found.");
        } else {
            for (Event event : items) {
                System.out.printf("Event: %s\nDate: %s\nID: %s\n\n",
                        event.getSummary(),
                        event.getStart().getDateTime(),
                        event.getId()); // <--- COPY THIS ID
            }
        }
    }

    // ==================================================
    // UTILITY METHOD 3: CANCEL EVENT
    // ==================================================
    public static void cancelEvent(Calendar service, String eventId) {
        try {
            service.events().delete("primary", eventId)
                    .setSendUpdates("all") // Sends cancellation email
                    .execute();
            System.out.println("-> SUCCESS: Event " + eventId + " has been cancelled.");
        } catch (IOException e) {
            System.out.println("-> ERROR: Could not find event with ID: " + eventId);
        }
    }

    // ==================================================
    // HELPER: AUTHENTICATION
    // ==================================================
    private static Calendar getCalendarService() throws IOException, GeneralSecurityException {
        final com.google.api.client.http.HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        InputStream in = CalendarManager.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}