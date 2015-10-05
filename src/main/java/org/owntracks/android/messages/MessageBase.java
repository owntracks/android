package org.owntracks.android.messages;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeResolver;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value=MessageLocation.class, name="location"),
        @JsonSubTypes.Type(value=MessageTransition.class, name="transition"),
        @JsonSubTypes.Type(value=MessageEvent.class, name="event"),
        @JsonSubTypes.Type(value=MessageMsg.class, name="msg"),
        @JsonSubTypes.Type(value=MessageCard.class, name="card")

})
public abstract class MessageBase {

}
