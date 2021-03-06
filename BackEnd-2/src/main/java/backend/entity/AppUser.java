package backend.entity;
import backend.CallParser.CallParser;
import backend.CallParser.GoogleCallParser;
import backend.helperClasses.BandType;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document
public class AppUser {
    @Id
    private String id;
    private String username; //email
    private String password;
    private String name;
    private String accessToken;
    private String refreshToken;

    //A value from BandTypes class.
    private BandType activeBandType;
    private CallParser callParser;
    private String outlookToken;
    private List<Event> events;



    public  String getOutlookToken(){
        return outlookToken;
    }

    public AppUser() {
        this.callParser=new GoogleCallParser();
    }

    public void setOutlookToken(String outlookToken) {
        this.outlookToken = outlookToken;
    }

    public AppUser(String id, String username, String password, String name, String accessToken, String refreshToken) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.events=new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void addEvent(Event event){
        //TODO: CHECK IF EVENT ALREADY EXITS

        events.add(event);
    }
    public void addEvents(ArrayList<Event> events){

        events.addAll(events);
    }

    public String getAccessToken (){
        return accessToken;
    }

    public void setAccessToken(String googleFitAccessToken) {
        this.accessToken = googleFitAccessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String googleFitRefreshToken) {
        this.refreshToken = googleFitRefreshToken;
    }

    public Event getEvent(String id) {
        if(events==null){
            return null;
        }
       for(Event event : events){

           if(event.getId().compareTo(id)==0){
               return event;
           }
       }
       return null;
    }

    public List<Event> getEvents() {
        return events;
    }
    public void saveAll(List<Event> e){
        events.addAll(e);
    }

    public void deleteEvent(String id){
        if(events==null){
            return ;
        }
        for(Event event:events){
            if(event.getId().compareTo(id) == 0){
                events.remove(event);
                return;
            }

        }
    }

    public void setEvents(List<Event> e) {
        events=e;
    }


    public BandType getActiveBandType() {
        return activeBandType;
    }

    public void setActiveBandType(BandType activeBandType) {
        this.activeBandType = activeBandType;
    }

    public CallParser getCallParser() {
        return callParser;
    }

    public void setCallParser(CallParser callParser) {
        this.callParser = callParser;
    }
    public static AppUser getUserforTesting(){
        AppUser user= new AppUser();
        user.setPassword("123");
        user.setUsername("abd");
        return user;
    }
}

