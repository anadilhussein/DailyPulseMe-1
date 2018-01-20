package backend.entity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class AppUserTest {

    AppUser user = new AppUser();

    @Before
    public void setUp() throws Exception {
        user.setId("DailyPulseUser");
        user.setName("DailyPulseMe@gmail.com");
        user.setUsername("AnadilMuhammedMuhammedNageebRoberPelegYotam");
        user.setEvents(new ArrayList<Event>());

    }

    @Test
    public void getId() throws Exception {
        assertTrue(user.getId().equalsIgnoreCase("DailyPulseUser"));
    }

    @Test
    public void setId() throws Exception {
        user.setId("fake");
        assertTrue(user.getId().equalsIgnoreCase("fake"));
        user.setId("DailyPulseUser");
    }

    @Test
    public void getName() throws Exception {
        assertTrue(user.getName().equalsIgnoreCase("DailyPulseMe@gmail.com"));
    }

    @Test
    public void setName() throws Exception {
        user.setName("fake");
        assertTrue(user.getName().equalsIgnoreCase("fake"));
        user.setName("DailyPulseMe@gmail.com");
    }


    @Test
    public void getUsername() throws Exception {
        assertTrue(user.getUsername().equalsIgnoreCase("AnadilMuhammedMuhammedNageebRoberPelegYotam"));
    }

    @Test
    public void setUsername() throws Exception {
        user.setUsername("fake");
        assertTrue(user.getUsername().equalsIgnoreCase("fake"));
        user.setUsername("AnadilMuhammedMuhammedNageebRoberPelegYotam");
    }
}
