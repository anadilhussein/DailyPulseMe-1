package backend.controller;

import backend.CallParser.FitBitCallParser;
import backend.CallParser.GoogleCallParser;
import backend.DailyPulseApp;
import backend.NLP.NLP;
import backend.emailSend.EmailSender;
import backend.entity.AppUser;
import backend.entity.Event;
import backend.entity.Subscription;
import backend.helperClasses.BandType;
import backend.Calendar.AuxMethods;
import backend.Calendar.GoogleCalendar;
import backend.Calendar.OutlookCalendar;
import backend.DailyPulseApp;
import backend.entity.*;
import backend.googleSignIn.SignUpGoogle;
import backend.helperClasses.KindOfEvent;
import backend.helperClasses.TwoStrings;
import backend.repository.SubscribedRepository;
import backend.repository.UserRepository;
import backend.service.UserService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.http.auth.AUTH;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.jws.soap.SOAPBinding;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static backend.Calendar.AuxMethods.isUserConnectedToCalendar;
import static backend.helperClasses.BandType.FITBIT_BAND;
import static backend.helperClasses.BandType.GOOGLEFIT_BAND;
import static backend.Calendar.AuxMethods.IsConnectedToGoogleCalendar;
import static backend.Calendar.AuxMethods.IsConnectedToOutlookCalendar;
import static backend.helperClasses.KindOfEvent.GOOGLE_EVENT;
import static backend.helperClasses.KindOfEvent.OUR_EVENT;
import static backend.helperClasses.KindOfEvent.OUTLOOK_EVENT;
import static backend.security.SecurityConstants.EXPIRATION_TIME;
import static backend.security.SecurityConstants.SECRET;


@RestController
@RequestMapping("/users")
public class UserController {
    private UserRepository appUserRepository;             //Repository that contains the users who  registered  for websites
    private BCryptPasswordEncoder bCryptPasswordEncoder; //this is used for encoding passwords
    private static final String MinInMs = "60000";        // constant which "stores" one minunte in millis
    private SubscribedRepository subscribedUserRepository; //Repository that contains weekly-mail subscribed users

    /*
    Constructor which gets two params , the appUserRepository and  bCryptPasswordEncoder
     */
    public UserController(UserRepository applicationUserRepository,
                          BCryptPasswordEncoder bCryptPasswordEncoder,
                          SubscribedRepository subscribedRepository) {
        this.appUserRepository = applicationUserRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.subscribedUserRepository = subscribedRepository;
    }


    /*
    singUp method adds new user fot the repository
    @param user which will be added
    @return true if the user doesn't exist in the repo, otherwise false
     */
    @PostMapping("/sign-up")
    public boolean signUp(AppUser user) {
        if(user.getUsername()==null){
            user=AppUser.getUserforTesting();
        }
        try {
            if (appUserRepository.findByUsername(user.getUsername()) != null) { //checking if the username already exist
                return false;
            }

            user.setPassword(bCryptPasswordEncoder.encode(user.getPassword())); //decoding the password
            user.setEvents(new ArrayList<>()); //initializing the events list for an empty one
            user.setAccessToken(" ");
            user.setRefreshToken(" ");

            //default is googleFit
            user.setActiveBandType(BandType.GOOGLEFIT_BAND);
            user.setCallParser(new GoogleCallParser());

            appUserRepository.save(user); //saving the user in the user's repository
            subscribedUserRepository.save(new Subscription(user.getUsername(),user.getName()));
            return true;
        }
        catch (Exception e){
            DailyPulseApp.LOGGER.info("error from backend " + e.toString());
            return false;
        }
    }

    /*
    @author:Anadil
     */
    @PostMapping("/sign-up-google")
    public String signUpViaGoogle(String authToken) {
        AppUser user=null;
        try {
            user = SignUpGoogle.getGoogleUser(authToken);
        }
        catch(Exception e){
            return "sign up via google faild :( see get GoogleUser "+e.toString();
        }

        //System.out.println("heeeeere"+user.getUsername());
        if(user==null){ //sign in via google failed
            return "user here is NULL";
        }

        try {
            AppUser appUsr=appUserRepository.findByUsername(user.getUsername());
            if (appUsr == null) { // user already exists
                user.setPassword(bCryptPasswordEncoder.encode(user.getPassword())); //decoding the password
                user.setEvents(new ArrayList<>()); //initializing the events list for an empty one
                user.setActiveBandType(BandType.GOOGLEFIT_BAND);
                user.setCallParser(new GoogleCallParser());
                subscribedUserRepository.save(new Subscription(user.getUsername(),user.getName()));
                appUserRepository.save(user); //first time --> add new user to the Repo.
            }
            else{
                appUsr.setAccessToken(user.getAccessToken());
                appUsr.setRefreshToken(user.getRefreshToken());
                subscribedUserRepository.save(new Subscription(user.getUsername(),user.getName()));
                appUserRepository.save(appUsr);
            }
        }
        catch (Exception e){
            return "o0ops maybe caused by mongo"+e.toString();
        }

        return JSONObject.quote(Jwts.builder()
                .setSubject(user.getUsername())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET.getBytes())
                .compact());

    }

    /*
    @auother: Anadil
    update outlookToken's access token and refresh token of Microsoft outlook ,
    @param auth which by it the user will be retrieved
    @param accessToken which contains the new access token and refresh token
    @return true
     */
    @PostMapping("/getOutlookToken")
    public boolean getOutLookToken(Authentication auth, TwoStrings accessTokens) {
        try {
            AppUser user = appUserRepository.findByUsername(auth.getName());
            if(user == null){
                return  false;
            }
            UserService.updateOutLookTokens(user,accessTokens); //calling for Service function
            appUserRepository.save(user);
            return true;
        }
        catch (Exception e){
            DailyPulseApp.LOGGER.info("error from backend " + e.toString());
            return false;
        }
    }


    /*@auother: Anadil
       this function return true if the user sign in to one calender at least
    */
    @GetMapping("/isConnectedToGoogleCalendar")
    public boolean isConnectedToGoogleCalendar(Authentication auth) {
        AppUser user = appUserRepository.findByUsername(auth.getName());
        if(user == null){
            return  false;
        }
        return  IsConnectedToGoogleCalendar(user);
    }
    /*
    @param auth which by it the user will be retrieved
    @param accessToken which contains the new access token and refresh token
    @return true
     */
    @PostMapping("/updateTokens")
    public boolean updateTokens(Authentication auth,TwoStrings tokens) {
        AppUser user = appUserRepository.findByUsername(auth.getName());
        if(user == null){
            return  false;
        }
        UserService.updateTokens(user,tokens); //calling for Service function
        appUserRepository.save(user);
        return true;
    }


    /*(Frontend Team requested this)
      when calling this method with an invalid token, an error will be returned,
      else, nothing will be returned
     */
    @GetMapping("/authenticateToken")
    public Boolean authenticateToken() {
        //authenticates JWT token.
        return true;
    }


    /*Aux Testing Method
     @param auth , which by it the user will be retrieved
     @return user name
     */
    @GetMapping("/username")
    public String getUsername(Authentication auth) {
        String str = appUserRepository.findByUsername(auth.getName()).getName();
        return JSONObject.quote(str);
    }

    /*
    addEvent adds new event to list user of specific user
    @param auth , which by it the user will be retrieved
    @param event which will be added
    @return true if the adding process passed OK ,otherwise false
     */
    @PostMapping("/addEvent")
    public boolean addEvent(Authentication auth,  Event event) {
        event.setKindOfEvent(OUR_EVENT);
        AppUser user = appUserRepository.findByUsername(auth.getName());
        if(event.getStartTime()!=null) {
            if (!UserService.addEvent(user, event)) {
                return false;
            }
        }
        appUserRepository.save(user);
        return true;
    }


    /*Aux Testing Method. @testing
    @param auth , which by it the user will be retrieved
    @return list of events of specific user
    */
    @GetMapping("/getAllEvents")
    public List<Event> getAllEvents(Authentication auth) {
        return appUserRepository.findByUsername(auth.getName()).getEvents();
    }

    @PostMapping("/changePassword")
    public boolean ChangePassword(Authentication auth,String newPassword){
        AppUser user=appUserRepository.findByUsername(auth.getName());
        if(user==null){
            return false;
        }
        user.setPassword(bCryptPasswordEncoder.encode(newPassword)); //decoding the password
        appUserRepository.save(user);
        return true;
    }

    /*
     Getting Events between Interval Time and with pulses between Interval
     in case time ='0' it means return all and dont care about the given timeInterval
     */
    @PostMapping("/getAllEventsWhichHavePulses")
    public List<Event> getAllEventsWhichHavePulses(Authentication auth,TwoStrings time){
if(time.getFirst()==null){
      time=new TwoStrings("0","11220");
}

        AppUser user=appUserRepository.findByUsername(auth.getName());
        List<Event> filter_=new ArrayList<Event>();
        ArrayList<Event> tmp=new ArrayList<Event>();
        filter_.addAll(user.getEvents());
        filter_=filter_.stream().filter(event -> !(event.getPulses() == null ||
                event.getPulses().size() == 0 )).collect(Collectors.toList());
        if(time.getFirst().compareTo("0")!=0){ //that means return all the events
            for (Event event : filter_) {//getting all events within time period
                if (Long.valueOf(event.getStartTime()) >= Long.valueOf(time.getFirst()) && Long.valueOf(event.getEndTime()) <= Long.valueOf(time.getSecond())) {
                    tmp.add(event);
                }
            }
            filter_=tmp;
        }
        return  filter_.stream().sorted(new Comparator<Event>() {
            @Override
            public int compare(Event r1, Event r2) {
                return (r1.getId().compareTo(r2.getId()));
            }

        }).collect(Collectors.toList());

    }

    /*
    deleteEvent deletes an event which exist in list of event of specific user
    @param auth , which by it the user will be retrieved
    @param evenId which is the id of the event to be deleted
    @return true if the deleting event process passed OK ,otherwise false
     */
    @PostMapping("/deleteEvent")
    public Boolean deleteEvent(Authentication auth, String eventId){
        AppUser user = appUserRepository.findByUsername(auth.getName());
        UserService.deleteEvent(user,eventId);
        appUserRepository.save(user);
        return true;
    }


    /*
    * this methods get all events from user Calendars
    * */
    @GetMapping("/getEvents")
    public List<Event> getEvents(Authentication auth) {
        TwoStrings time=new TwoStrings();
        time.setFirst("0");
        return getEventsBetweenInterval(auth,time);
    }


    /*
     Events which were taken place between time1 until time2 (including events which
    ,and each event which will be returned through the list , will includes it's pulses
     @param auth , which by it the user will be retrieved
     @param time which contains the startTiming and endTiming
     @return list of events which were taken place between time1 until time2
     */

    @PostMapping("/getEventsBetweenInterval")
    public List<Event> getEventsBetweenInterval(Authentication auth, TwoStrings time) {

        getCalendarsEvents(auth); //TODO :Refresh (we dont need to bring what is Already Exist)
        AppUser user = appUserRepository.findByUsername(auth.getName());
        List<Event> filter = UserService.getEvents(user,time);
        appUserRepository.save(user);
        List<Event> toReturn=new ArrayList<Event>();
        if(time.getFirst().compareTo("0")==0){
            toReturn = user.getEvents().stream().sorted(new Comparator<Event>() {
                @Override
                public int compare(Event r1, Event r2) {
                    return (r1.getId().compareTo(r2.getId()));
                }

            }).collect(Collectors.toList());
        }
        else{
                if (filter == null) {
                    return null;
                }
                    toReturn = filter.stream().sorted(new Comparator<Event>() {
                    @Override
                    public int compare(Event r1, Event r2) {
                        return (r1.getId().compareTo(r2.getId()));
                    }
                }).collect(Collectors.toList());
            }
            //for(Event event : toReturn){
            //    System.out.println("here is an event : "+ event.getStartTime());
            //}
            return toReturn;
    }

    @RequestMapping("/GoogleCalendarEvents")
    public ArrayList<Event> getEventsGoogleCalendar(Authentication auth)  {
        ArrayList<Event> tmp=null;
        AppUser user = appUserRepository.findByUsername(auth.getName());
        try{
            tmp=GoogleCalendar.getEvents(user);
            user.addEvents(tmp);
            appUserRepository.save(user);
        }catch (Exception e){
         // System.out.println("line 188 user cON");
        }
        return tmp;
    }


    /*
    testing method
     */
    @RequestMapping("/OutlookCalendarEvents")
    public ArrayList<Event> getEventsOutlookCalendar(Authentication auth)  {
        ArrayList<Event> tmp=null;
        AppUser user = appUserRepository.findByUsername(auth.getName());
        try{
            tmp=OutlookCalendar.getEvents(user);
        }catch (Exception e){
            // System.out.println("line 188 user cON");
        }
        return tmp;
    }


    /*
    @author :Anadil
    getting Calendars Events Outlook&Google Calendar
     */
    @RequestMapping("/GetCalendarsEvents")
    public ArrayList<Event> getCalendarsEvents(Authentication auth)  {

        AppUser user = appUserRepository.findByUsername(auth.getName());
        ArrayList<Event> tmp= UserService.getCalendarsEvents_(user);
        user.setEvents(tmp);
        appUserRepository.save(user);
        return tmp;
    }



    /*
    getEvent return an Event,
    @param auth , which by it the user will be retrieved
    @param id of the event which will be returned
    @return an event from the user's list of event which it's id is same to the given id
    */
    @PostMapping("/getEvent")
    public Event getEvent(Authentication auth, String id) {
        return appUserRepository.findByUsername(auth.getName()).getEvent(id);
    }

    @GetMapping("/verifyAccessToken")
    public boolean verifyAccessToken(Authentication auth){
        AppUser user = appUserRepository.findByUsername(auth.getName());
        boolean response = UserService.verifyAndRefresh(user);
        appUserRepository.save(user);
        return response;
    }

    @GetMapping("/refreshAccessToken")
    public boolean refreshAccessToken(Authentication auth){
        AppUser user = appUserRepository.findByUsername(auth.getName());
        UserService.refreshToken(user);
        return true;
    }
    //sets the FitBit band as the active band
    @PostMapping("/setFitBitBand")
    public void updateToFitBit(Authentication auth){
        AppUser user = appUserRepository.findByUsername(auth.getName());
        user.setCallParser(new FitBitCallParser());
        user.setActiveBandType(FITBIT_BAND);
    }
    //sets the GoogleFit as the active band (this is the default)
    @PostMapping("/setGoogleFitBand")
    public void updateToGoogleFit(Authentication auth){
        AppUser user = appUserRepository.findByUsername(auth.getName());
        user.setCallParser(new GoogleCallParser());
        user.setActiveBandType(GOOGLEFIT_BAND);
    }

    /*returns the number associated with the band type
    0 - GoogleFit
    1 - FitBit
    */
    @GetMapping("/getActiveBand")
    public int getActiveBand(Authentication auth) {
        AppUser user = appUserRepository.findByUsername(auth.getName());
        return user.getActiveBandType().getBandTypeAsInt();
    }

    //adds the user to the subscribedUsersRepo
    @PostMapping("/subscribe")
    public boolean subscribeUser(Authentication auth){
        AppUser user = appUserRepository.findByUsername(auth.getName());
        if(user == null || subscribedUserRepository.findByEmail(user.getUsername()) != null){
            //user already subscribed
            return false;
        }
        subscribedUserRepository.save(new Subscription(user.getUsername(),user.getName()));
        return true;
    }

    //removes the user from the subscribedUsersRepo
    @PostMapping("unsubscribe")
    public boolean unsubscribeUser(Authentication auth){
        AppUser user = appUserRepository.findByUsername(auth.getName());
        if(user == null || subscribedUserRepository.findByEmail(user.getUsername()) == null){
            //user already unsubscribed
            return false;
        }
        subscribedUserRepository.deleteByEmail(user.getUsername());
        return true;
    }
    //returns all the subscribed users
    //TODO: find a way to make this safe (added to public calls)
    @GetMapping("/getSubscribedUsers")
    public List<Subscription> getSubscribedUsers(){
        return subscribedUserRepository.findAll();
    }

    @GetMapping("/sendEmails")
    public void sendMail() {
        ArrayList<Subscription> subcribers = new ArrayList<>();
        subcribers.addAll(subscribedUserRepository.findAll());
        EmailSender.sendMail(subcribers);
    }

    @GetMapping("/isSubscribed")
    private boolean isSubscribed(Authentication auth){
        AppUser user = appUserRepository.findByUsername(auth.getName());
        if(user == null || subscribedUserRepository.findByEmail(user.getUsername()) == null){
            return false;
        }
        return true;
    }


    @PostMapping("/NLP")
    private String getNLPres(@RequestBody String name) {
        try {
            return NLP.TestingRunNLP(name);
        } catch (Exception e) {
        }
        return "FYUCJl";
    }


    @GetMapping("/eventsCount")
    private int getEventsCount(Authentication auth) {
        AppUser user = appUserRepository.findByUsername(auth.getName());
        if(user.getEvents()==null){
                return 0;
           }
       return  user.getEvents().size();
    }


    /*@auother: Anadil
   this function return true if the user sign in to his outlook calender
       */
    @GetMapping("/isConnectedToOutlookCalendar")
    public boolean isConnectedToOutlookCalendar(Authentication auth) {
        AppUser user = appUserRepository.findByUsername(auth.getName());
        if(user == null){
            return  false;
        }
        return  IsConnectedToOutlookCalendar(user);
    }

}