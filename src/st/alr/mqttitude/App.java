package st.alr.mqttitude;

import java.util.HashMap;
import java.util.Map;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.bugsnag.android.Bugsnag;

import st.alr.mqttitude.support.Contact;
import st.alr.mqttitude.support.ContactAdapter;
import st.alr.mqttitude.support.Defaults;


public class App extends Application {
    private static App instance;
    private static Map<String,Contact> contacts = new HashMap<String,Contact>();
    static ContactAdapter contactsAdapter;

    
    
    public static Map<String, Contact> getContacts() {
        return contacts;
    }

    public static void setContacts(Map<String, Contact> contacts) {
        App.contacts = contacts;
    }

    public static ContactAdapter getContactsAdapter() {
        return contactsAdapter;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Bugsnag.register(this, Defaults.BUGSNAG_API_KEY);
        Bugsnag.setNotifyReleaseStages("production", "testing");
        
        contactsAdapter = new ContactAdapter(this, contacts);

    }

    public static Context getContext() {
        return instance;
    }
}
