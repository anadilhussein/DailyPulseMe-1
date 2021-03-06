package backend.service;

import backend.Calendar.GoogleCalendar;
import backend.Calendar.OutlookCalendar;
import backend.NLP.NLP;
import backend.entity.AppUser;
import backend.entity.Event;
import backend.entity.Pulse;
import backend.entity.RefreshTokenExpiredException;
import backend.entity.*;
import backend.helperClasses.BandType;
import backend.helperClasses.TwoStrings;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static backend.Calendar.AuxMethods.IsConnectedToGoogleCalendar;
import static backend.Calendar.AuxMethods.IsConnectedToOutlookCalendar;
import static backend.helperClasses.KindOfEvent.GOOGLE_EVENT;
import static backend.helperClasses.KindOfEvent.OUTLOOK_EVENT;

public class UserService {
    private static final String MinInMs = "60000";


    /*
    This function adds an event to  list events of the user
    @param user which for him the event will be added
    @param event which will be added for the user's events
    @return true , if this event is allowed to be added , otherwise false
 */
    public static boolean addEvent(AppUser user, Event event) {
		
		
        long startTime = Long.parseLong(event.getStartTime());
        long endTime = Long.parseLong(event.getEndTime());//check if this event can be added (doesn't interlap) and it's times are legal
        int size = user.getEvents().stream().filter(x ->
                ((Long.parseLong(x.getStartTime()) >=  endTime) && (Long.parseLong(x.getEndTime()) >= endTime))
                || ((Long.parseLong(x.getStartTime()) <=  startTime) && (Long.parseLong(x.getEndTime()) <= startTime))).collect(Collectors.toList()).size();
        if ((user.getEvents().size()-size) != 0) {
            return false;
        }
		//add to user's events  list
        user.addEvent(event);//since events doesn't interlap , we give the event a unique id , it's start time (easier to return it to the front when needed)
        event.setId(event.getStartTime());
        return true;
    }


    /*
 This function removes an event from the list events of user
 @param user which for him the event will be removed from his events' list
 @param eventId of the event which will be removed
 @return true , if the specific event exists in the user list event , otherwise false
  */
    public static boolean deleteEvent(AppUser user, String eventID) {
if(user.getEvents()==null){
    return false;
}
        Event event_ = null;
        boolean isExist=false;
        List<Event> tmp = user.getEvents();//temp list of the user's events
        for (Event event : user.getEvents()) {//event start time is it's id as we agreed with the front , find this event

            if (event.getStartTime().equals(eventID)) {
                event_ = event;
                isExist=true;
                break;
            }
        }

        if(!isExist){
            return  false;
        }

        tmp.remove(event_);//delete event from tmp list
        user.setEvents(tmp);//update user's list
        return true;
    }



    /*
    This function return Events which were taken place between time1 until time2
    ,and each event which will be returned through the list , will includes it's pulses
    @param user which his events will be returned
    @param time which contains the startTiming and endTiming
    @return list of events which were taken place between time1 until time2
 */
    public static List<Event> getEvents(AppUser user, TwoStrings time) {

        List<Event> events = user.getEvents();
        List<Event> filter = new ArrayList<Event>();
        for (Event event : events) {//getting all events within time period
            if (Long.valueOf(event.getStartTime())>=Long.valueOf(time.getFirst())  && Long.valueOf(event.getEndTime())<=Long.valueOf(time.getSecond())) {
                filter.add(event);
            }
        }
        List<Event> result = new ArrayList<Event>(); //filter contains the events in the given time interval
        List<Pulse> eventPulses;

        for (Event event : filter) {//for all the events we should get the pulses


            if(user.getAccessToken()==null || user.getAccessToken().compareTo("")==0 ||
                    user.getAccessToken().compareTo(" ")==0){ //the user is not connected to google API
                break;
            }

            if (event.getPulses().size() == 0) {	//if it's pulses list is empty  , we should ask google to give us the pulses
                try {
                    //getCallParser will return either FitBit or Google callParser
                    eventPulses = user.getCallParser().getPulses(user, event.getStartTime(), event.getEndTime(), MinInMs);//get the pulses in this specific time
                    //Calling NLP here
                    try {
                        if(eventPulses.size()>0){
                            event.setTag(NLP.RunNLP(event.getName())); //TODO: nlp place , call it only when we have pulses
                        }
                    }catch (Exception e){
                        System.out.println("calling NLP +"+e.toString());
                    }
                } catch (RefreshTokenExpiredException e) {
                    return null;
                }
                event.saveAll(eventPulses);//update the pulses for this event
                event.setAverage();
            }
        }

        return filter;
    }

    /*
        This function updates the tokens
        @param user which for him the token will be updated
        @param accessTokens which contains both access token and refresh token
        @return true , if the updating process passed okey, otherwise false
     */
    public static boolean updateTokens(AppUser user, TwoStrings accessTokens){
        //update token fields 
        user.setAccessToken(accessTokens.getFirst());
        user.setRefreshToken(accessTokens.getSecond());
        return true;
    }

    public static boolean verifyAndRefresh(AppUser user) {
        return user.getCallParser().verifyAndRefresh(user);
    }

    public static void refreshToken(AppUser user) {
        user.getCallParser().refreshToken(user);
    }
    /*
    @auother: Anadil
    update outlookToken's access token and refresh token of Microsoft outlook ,
    @param auth which by it the user will be retrieved
    @param accessToken which contains the new access token and refresh token
    @return true
    */
    public static boolean updateOutLookTokens(AppUser user, TwoStrings accessTokens){
        user.setOutlookToken(accessTokens.getFirst());
        return true;
    }

    public static  ArrayList<Event> getCalendarsEvents_(AppUser user) {
        ArrayList<Event> tmp_=new ArrayList<>();
        ArrayList<Event> tmp=new ArrayList<>();
        try {
            if (IsConnectedToGoogleCalendar(user)) {
                tmp_ = GoogleCalendar.getEvents(user);
            }
            if (IsConnectedToOutlookCalendar(user)) {
                tmp_.addAll(OutlookCalendar.getEvents(user));
            }

            boolean isNewEvent = true;
            for (Event event : tmp_) {
                for (Event userEvent : user.getEvents()) {
                    if (userEvent.getId().compareTo(event.getId()) == 0 && userEvent.getEndTime().compareTo(event.getEndTime()) == 0
                            && userEvent.getKindOfEvent() == event.getKindOfEvent()) {
                        isNewEvent = false;
                        break;
                    }
                }
                if (isNewEvent) {
                    //event.setTag(NLP.RunNLP(event.getName())); //TODO: nlp place , call it only when we have pulses
                    tmp.add(event);
                }
                isNewEvent = true;
            }
            ////// mohamad abd code
            boolean isconnectedToGoogle = IsConnectedToGoogleCalendar(user);
            boolean isconnectedToOutlook = IsConnectedToOutlookCalendar(user);
            List<Event> userEvents = user.getEvents();
            List<Event> userEventsToremove = new ArrayList<>();
            boolean eventDeleted = true;
            for (Event userEvent : userEvents) {// check if the event has been deleted
                if ((userEvent.getKindOfEvent() == GOOGLE_EVENT && isconnectedToGoogle) ||
                        (userEvent.getKindOfEvent() == OUTLOOK_EVENT && isconnectedToOutlook)) {
                    for (Event event : tmp_) {
                        if (userEvent.getId().equals(event.getId()) && userEvent.getEndTime().equals(event.getEndTime()) && userEvent.getKindOfEvent()==event.getKindOfEvent()) {
                            eventDeleted = false;
                            break;
                        }
                    }
                    if (eventDeleted) {
                        userEventsToremove.add(userEvent);
                    }
                }
                eventDeleted = true;
            }

            for (Event j : userEventsToremove) {
                userEvents.remove(j);
            }
            ///////// end of mohamad abd code
            tmp.addAll(userEvents);
        }
        catch (Exception e){
             e.printStackTrace();
        }
        return tmp;
    }
}
