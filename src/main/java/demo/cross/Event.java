package demo.cross;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", defaultImpl = Event.class)
public class Event {
    public String message;
}
