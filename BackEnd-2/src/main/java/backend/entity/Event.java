package backend.entity;

import backend.repository.PulseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.annotation.Generated;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Document
public class Event {
    @Id
    private String id;
    private String name;
    private String startTime;
    private String endTime;
    private String description;
    private EventTag tag;
    @DBRef
     private List<Pulse> pulses;
    // private PulseRepository pulses;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventTag getTag() {
        return tag;
    }

    public void setTag(EventTag tag) {
        this.tag = tag;
    }

    public List<Pulse> getPulses() {
        return pulses;
    }

    public void addPulse(Pulse pulse) {
        //TODO: check if already in database?
        this.pulses.add(pulse);
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String  endTime) {
        this.endTime = endTime;
    }
    public void saveAll(List<Pulse> p){
        pulses.addAll(p);
    }
    public void setPulses(List<Pulse> p){
        this.pulses=p;
    }

}