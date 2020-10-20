package org.owntracks.android.messages;

import androidx.databinding.Bindable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MessageCard extends MessageBase {
    public static final String BASETOPIC_SUFFIX = "/info";
    static final String TYPE = "card";
    private String name;
    private String face;

    @Bindable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFace() {
        return face;
    }

    @JsonSetter
    public void setFace(String face) {
        this.face = face;
    }

    public boolean hasFace() {
        return this.face != null;
    }

    public boolean hasName() {
        return this.name != null;
    }

    public String getBaseTopicSuffix() {
        return BASETOPIC_SUFFIX;
    }
}
