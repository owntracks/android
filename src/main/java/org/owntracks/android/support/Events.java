package org.owntracks.android.support;

import java.util.Date;

import org.owntracks.android.db.Message;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.messages.CardMessage;
import org.owntracks.android.messages.ConfigurationMessage;
import org.owntracks.android.messages.MsgMessage;
import org.owntracks.android.messages.WaypointMessage;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.messages.LocationMessage;

import android.location.Location;

public class Events {

    public static class ModeChanged extends E {
        final int newModeId;
        final int oldModeId;

        public ModeChanged(int oldModeId, int newModeId) {
            this.newModeId = newModeId;
            this.oldModeId = oldModeId;
        }
        public int getNewModeId() {
            return newModeId;
        }
        public int getOldModeId() {
            return oldModeId;
        }

    }

	public static class WaypointTransition extends E {
		final Waypoint w;
		int transition;

		public WaypointTransition(Waypoint w, int transition) {
			super();
			this.w = w;
			this.transition = transition;
		}

		public Waypoint getWaypoint() {
			return this.w;
		}

		public int getTransition() {
			return this.transition;
		}

	}

	public static class WaypointAddedByUser extends E {
		final Waypoint w;

		public WaypointAddedByUser(Waypoint w) {
			super();
			this.w = w;
		}

		public Waypoint getWaypoint() {
			return this.w;
		}

	}
    public static class WaypointAdded extends E {
        final Waypoint w;

        public WaypointAdded(Waypoint w) {
            super();
            this.w = w;
        }

        public Waypoint getWaypoint() {
            return this.w;
        }

    }


    public static class WaypointUpdated extends E {
        final Waypoint w;

        public WaypointUpdated(Waypoint w) {
            super();
            this.w = w;
        }

        public Waypoint getWaypoint() {
            return this.w;
        }

    }

    public static class WaypointUpdatedByUser extends E {
        final Waypoint w;

        public WaypointUpdatedByUser(Waypoint w) {
            super();
            this.w = w;
        }

        public Waypoint getWaypoint() {
            return this.w;
        }

    }


	public static class WaypointRemoved extends E {
		final Waypoint w;

		public WaypointRemoved(Waypoint w) {
			super();
			this.w = w;
		}

		public Waypoint getWaypoint() {
			return this.w;
		}

	}

	public static abstract class E {
		final Date date;

		public E() {
			this.date = new Date();
		}

		public Date getDate() {
			return this.date;
		}

	}

	public static class Dummy extends E {
		public Dummy() {
		}
	}

	public static class PublishSuccessful extends E {
		final Object extra;
        final boolean wasQueued;

		public PublishSuccessful(Object extra, boolean wasQueued) {
			super();
			this.extra = extra;
            this.wasQueued = wasQueued;
		}

		public Object getExtra() {
			return this.extra;
		}
        public boolean wasQueued() {return  this.wasQueued;}
	}

	public static class CurrentLocationUpdated extends E {
		final GeocodableLocation l;

		public CurrentLocationUpdated(Location l) {
			super();
			this.l = new GeocodableLocation(l);
		}

		public CurrentLocationUpdated(GeocodableLocation l) {
			this.l = l;
		}

		public GeocodableLocation getGeocodableLocation() {
			return this.l;
		}

	}

	public static class ContactAdded extends E {
        final FusedContact contact;

		public ContactAdded(FusedContact f) {
			super();
			this.contact = f;
		}

		public FusedContact getContact() {
			return this.contact;
		}

	}

    public static class FusedContactAdded extends E {
        final FusedContact contact;

        public FusedContactAdded(FusedContact f) {
            super();
            this.contact = f;
        }

        public FusedContact getContact() {
            return this.contact;
        }
    }

    public static class FusedContactUpdated extends E {
        final FusedContact contact;

        public FusedContactUpdated(FusedContact f) {
            super();
            this.contact = f;
        }

        public FusedContact getContact() {
            return this.contact;
        }
    }


    public static class ContactRemoved extends E{
        final FusedContact contact;

        public ContactRemoved(FusedContact f) {
            super();
            this.contact = f;
        }

        public FusedContact getContact() {
            return this.contact;
        }
    }

    public static class ClearLocationMessageReceived extends E{
        final FusedContact c;
        public ClearLocationMessageReceived(FusedContact c) {
            super();
            this.c = c;
        }

        public FusedContact getContact() {
            return c;
        }
    }

    public static class MsgMessageReceived {
        final MsgMessage message;
        final String topic;
        public MsgMessageReceived(MsgMessage message, String topic) {
            super();
            this.message = message;
            this.topic = topic;

        }
        public MsgMessage getMessage() {
            return this.message;
        }
        public String getTopic() {
            return this.topic;
        }

    }

    public static class CardMessageReceived extends E{
        final String topic;
        final CardMessage message;
        public CardMessageReceived(CardMessage m, String topic) {
            super();
            this.message = m;
            this.topic = topic;

        }

        public String getTopic() {
            return this.topic;
        }

        public CardMessage getCardMessage() {
            return this.message;
        }
    }




    public static class LocationMessageReceived extends E {
		private final String t;
		private final LocationMessage m;

		public LocationMessageReceived(LocationMessage m, String t) {
			super();
			this.t = t;
			this.m = m;
		}

		public String getTopic() {
			return this.t;
		}

		public LocationMessage getLocationMessage() {
			return this.m;
		}

		public GeocodableLocation getGeocodableLocation() {
			return this.m.getLocation();
		}
	}

    public static class WaypointMessageReceived extends E {
        private final String t;
        private final WaypointMessage m;

        public WaypointMessageReceived(WaypointMessage m, String t) {
            super();
            this.t = t;
            this.m = m;
        }

        public String getTopic() {
            return this.t;
        }

        public WaypointMessage getLocationMessage() {
            return this.m;
        }
    }

    public static class ConfigurationMessageReceived extends E {
        private final String t;
        private final ConfigurationMessage m;

        public ConfigurationMessageReceived(ConfigurationMessage m, String t) {
            super();
            this.t = t;
            this.m = m;
        }

        public String getTopic() {
            return this.t;
        }

        public ConfigurationMessage getConfigurationMessage() {
            return this.m;
        }
    }




    public static class ContactUpdated extends E {
		private final FusedContact c;

		public ContactUpdated(FusedContact c) {
			super();
			this.c = c;
		}

		public FusedContact getContact() {
			return this.c;
		}
	}

    public static class MessageAdded extends E{
        private final Message m;
        public MessageAdded(Message m) {
            this.m = m;
        }
        public Message getMessage(){
            return m;
        }
    }
	
	public static class BrokerChanged extends E {
		public BrokerChanged() {}
	}

	public static class StateChanged {
		public static class ServiceBroker extends E {
			private final org.owntracks.android.services.ServiceBroker.State state;
			private final Object extra;

			public ServiceBroker(org.owntracks.android.services.ServiceBroker.State state) {
				this(state, null);
			}

			public ServiceBroker(org.owntracks.android.services.ServiceBroker.State state,
					Object extra) {
				super();
				this.state = state;
				this.extra = extra;
			}

			public org.owntracks.android.services.ServiceBroker.State getState() {
				return this.state;
			}

			public Object getExtra() {
				return this.extra;
			}

		}


        public static class ServiceBeacon extends E {
            ///private org.owntracks.android.services.ServiceBeacon.State state;

            //public ServiceBeacon(org.owntracks.android.services.ServiceBeacon.State state) {
             //   this.state = state;
            //}

           // public org.owntracks.android.services.ServiceBeacon.State getState() {
            //    return this.state;
            //}

        }



    }
    public static class PermissionGranted extends E {
        private final String permission;
        public PermissionGranted(String p) {
            this.permission = p;
        }
        public String getPermission() {
            return permission;
        }
    }

}
