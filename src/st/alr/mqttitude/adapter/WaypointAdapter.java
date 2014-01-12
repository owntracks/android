package st.alr.mqttitude.adapter;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.R;
import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.db.WaypointDao;
import st.alr.mqttitude.services.ServiceProxy;
import st.alr.mqttitude.support.Events;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class WaypointAdapter extends BaseAdapter {
    private WaypointDao dao;
    private Activity context;
    private ArrayList<Waypoint> list;
    
    public WaypointAdapter(Activity c) {
        super();
        this.context = c;
        this.dao = ServiceProxy.getServiceApplication().getWaypointDao();
        this.list = new ArrayList<Waypoint>(dao.loadAll());    
        notifyDataSetChanged();
    }
    
    static class ViewHolder {
        
        public TextView description;
        public TextView transitionType;
        public TextView location;
        public TextView radius;

      }

    
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Waypoint getItem(int position) {
        return list.get(position);
        }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
          LayoutInflater inflater = context.getLayoutInflater();
          rowView = inflater.inflate(R.layout.row_waypoint, null);
          ViewHolder viewHolder = new ViewHolder();
          viewHolder.description = (TextView) rowView.findViewById(R.id.description);
        //  viewHolder.transitionType = (TextView) rowView.findViewById(R.id.transitionType);
          viewHolder.location = (TextView) rowView.findViewById(R.id.location);

          rowView.setTag(viewHolder);
        }

        Waypoint w = getItem(position);
        
        ViewHolder holder = (ViewHolder) rowView.getTag();
        holder.description.setText(w.getDescription());
       // holder.transitionType.setText(w.getTransitionType());
        holder.location.setText(w.getLatitude() + ":" + w.getLongitude());
        //holder.radius.setText(w.getRadius().toString());

        return rowView;

    }
    
    public void update(Waypoint w) {
        dao.update(w);
        this.list = new ArrayList<Waypoint>(dao.loadAll());
        notifyDataSetChanged();
        EventBus.getDefault().post(new Events.WaypointUpdated(w));
    }

    public void add(Waypoint w) {
        dao.insert(w);       
        this.list = new ArrayList<Waypoint>(dao.loadAll());
        notifyDataSetChanged();
        EventBus.getDefault().post(new Events.WaypointAdded(w));
    }

    public void remove(Waypoint w) {
        dao.delete(w);
        this.list = new ArrayList<Waypoint>(dao.loadAll());
        notifyDataSetChanged();
        EventBus.getDefault().post(new Events.WaypointRemoved(w));
    }

}
