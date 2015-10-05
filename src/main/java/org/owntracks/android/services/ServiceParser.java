package org.owntracks.android.services;

import android.content.Intent;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.MessageDao;
import org.owntracks.android.messages.CardMessage;
import org.owntracks.android.messages.ConfigurationMessage;
import org.owntracks.android.messages.LocationMessage;
import org.owntracks.android.messages.MsgMessage;
import org.owntracks.android.messages.TransitionMessage;
import org.owntracks.android.model.Contact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;

import de.greenrobot.event.EventBus;


public class ServiceParser implements ProxyableService{
    public static final String TAG = "ServiceParser";
    ObjectMapper mapper;
    ServiceProxy context;

    @Override
    public void onCreate(ServiceProxy c) {
        this.mapper = new ObjectMapper();
        this.context = c;
        ObjectMapper mapper = new ObjectMapper();


    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 0;
    }

    @Override
    public void onEvent(Events.Dummy event) {

    }

    public void parseIncomingBrokerMessage(String topic, MqttMessage message) throws Exception {
        String msg = new String(message.getPayload());

        String type;
        JSONObject json;

        Log.v(TAG, "Received message: " + topic + " : " + msg);

        try {
            json = new JSONObject(msg);
            type = json.getString("_type");
        } catch (Exception e) {
            if(msg.isEmpty()) {
                Log.v(TAG, "Empty message received");

                Contact c = App.getContact(topic);
                if(c != null) {
                    Log.v(TAG, "Clearing contact location");

                    EventBus.getDefault().postSticky(new Events.ClearLocationMessageReceived(c));
                    return;
                }
            }

            Log.e(TAG, "Invalid message received: " + msg);
            return;
        }

        if (type.equals("location")) {
            try {
                LocationMessage lm = new LocationMessage(json);
                lm.setRetained(message.isRetained());
                EventBus.getDefault().postSticky(new Events.LocationMessageReceived(lm, topic));
            } catch (Exception e) {
                Log.e(TAG, "Message hat correct type but could not be handled. Message was: " + msg + ", error was: " + e.getMessage());
            }
        } else if (type.equals("card")) {
            CardMessage card = new CardMessage(json);
            EventBus.getDefault().postSticky(new Events.CardMessageReceived(card, topic));
        } else if (type.equals("transition")) {
            TransitionMessage tm = new TransitionMessage(json);
            tm.setRetained(message.isRetained());
            EventBus.getDefault().postSticky(new Events.TransitionMessageReceived(tm, topic));
        } else if (type.equals("msg")) {
            MsgMessage mm = new MsgMessage(json);
            EventBus.getDefault().post(new Events.MsgMessageReceived(mm, topic));
        } else if(type.equals("cmd") && topic.equals(Preferences.getPubTopicCommands())) {
            String action;
            try {
                action = json.getString("action");
            } catch (Exception e) {
                return;
            }

            switch (action) {
                case "reportLocation":
                    if (!Preferences.getRemoteCommandReportLocation()) {
                        Log.i(TAG, "ReportLocation remote command is disabled");
                        return;
                    }
                    ServiceProxy.getServiceLocator().publishResponseLocationMessage();

                    break;
                default:
                    Log.v(TAG, "Received cmd message with unsupported action (" + action + ")");
                    break;
            }

        } else if (type.equals("configuration") && topic.equals(Preferences.getPubTopicCommands()) ) {
            // read configuration message and post event only if Remote Configuration is enabled and this is a private broker
            if (!Preferences.getRemoteConfiguration() || Preferences.isModePublic()) {
                Log.i(TAG, "Remote Configuration is disabled");
                return;
            }
            ConfigurationMessage cm = new ConfigurationMessage(json);
            cm.setRetained(message.isRetained());
            EventBus.getDefault().post(new Events.ConfigurationMessageReceived(cm, topic));

        } else {
            Log.d(TAG, "Ignoring message (" + type + ") received on topic " + topic);
        }
    }

    public void onEventMainThread(Events.ClearLocationMessageReceived e) {
        App.removeContact(e.getContact());
    }

    public void onEvent(Events.MsgMessageReceived e) {
        MsgMessage mm = e.getMessage();
        String externalId = e.getTopic() + "$" + mm.getTst();

        org.owntracks.android.db.Message m = Dao.getMessageDao().queryBuilder().where(MessageDao.Properties.ExternalId.eq(externalId)).unique();
        if (m == null) {
            m = new org.owntracks.android.db.Message();
            m.setIcon(mm.getIcon());
            m.setPriority(mm.getPrio());
            m.setIcon(mm.getIcon());
            m.setIconUrl(mm.getIconUrl());
            m.setUrl(mm.getUrl());
            m.setExternalId(externalId);
            m.setDescription(mm.getDesc());
            m.setTitle(mm.getTitle());
            m.setTst(mm.getTst());
            if (mm.getMttl() != 0)
                m.setExpiresTst(mm.getTst() + mm.getMttl());

            if (e.getTopic() == Preferences.getBroadcastMessageTopic())
                m.setChannel("broadcast");
            else if (e.getTopic().startsWith(Preferences.getPubTopicBase(true)))
                m.setChannel("direct");
            else
                try {
                    m.setChannel(e.getTopic().split("/")[1]);
                } catch (IndexOutOfBoundsException exception) {
                    m.setChannel("undefined");
                }

            Dao.getMessageDao().insert(m);
            EventBus.getDefault().post(new Events.MessageAdded(m));
        }
    }



    private Contact lazyUpdateContactFromMessage(String topic, GeocodableLocation l, String trackerId) {
        Log.v(TAG, "lazyUpdateContactFromMessage for: " + topic);
        org.owntracks.android.model.Contact c = App.getContact(topic);

        if (c == null) {
            c = App.getInitializingContact(topic);


            if (c == null) {
                Log.v(TAG, "creating new contact without card: " + topic);
                c = new org.owntracks.android.model.Contact(topic);
            } else {
                Log.v(TAG, "creating unintialized contact with card: " + topic);
            }
            Contact.resolveContact(context,c);
            c.setLocation(l);
            c.setTrackerId(trackerId);
            App.addContact(c);
        } else {
            c.setLocation(l);
            c.setTrackerId(trackerId);
            EventBus.getDefault().post(new Events.ContactUpdated(c));
        }
        return c;
    }

    private String getBaseTopic(String forStr, String topic) {
        if (topic.endsWith(forStr))
            return topic.substring(0, (topic.length() - forStr.length()));
        else
            return topic;
    }

    public String getBaseTopicForEvent(String topic) {
        return getBaseTopic(Preferences.getPubTopicEventsPart(), topic);
    }

    private String getBaseTopicForInfo(String topic) {
        return getBaseTopic(Preferences.getPubTopicInfoPart(), topic);
    }


    public void onEventMainThread(Events.CardMessageReceived e) {
        String topic = getBaseTopicForInfo(e.getTopic());
        Contact c = App.getContact(topic);
        Log.v(TAG, "card message received for: " + topic);

        if (App.getInitializingContact(topic) != null) {
            Log.v(TAG, "ignoring second card for uninitialized contact " + topic);
            return;
        }
        if (c == null) {
            Log.v(TAG, "initializing card for: " + topic);

            c = new Contact(topic);
            c.setCardFace(e.getCardMessage().getFace());
            c.setCardName(e.getCardMessage().getName());

            App.addUninitializedContact(c);
        } else {

            Log.v(TAG, "updating card for existing contact: " + topic);
            c.setCardFace(e.getCardMessage().getFace());
            c.setCardName(e.getCardMessage().getName());
            EventBus.getDefault().post(new Events.ContactUpdated(c));
        }
    }

    public void onEventMainThread(Events.LocationMessageReceived e) {
        lazyUpdateContactFromMessage(e.getTopic(), e.getGeocodableLocation(), e.getLocationMessage().getTrackerId());
    }

    public void onEventMainThread(Events.ConfigurationMessageReceived e) {

        Preferences.fromJsonObject(e.getConfigurationMessage().toJSONObject());

        // Reconnect to broker after new configuration has been saved.
        Runnable r = new Runnable() {

            @Override
            public void run() {
                ServiceProxy.getServiceBroker().reconnect();
            }
        };
        new Thread(r).start();

    }

}
