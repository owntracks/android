
package st.alr.mqttitude;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

public class ActivityLauncher extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, ActivityMain.class);
        startActivity(intent);
        finish();
    }
}
