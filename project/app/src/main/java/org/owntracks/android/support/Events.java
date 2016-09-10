package org.owntracks.android.support;

import java.util.Date;

import org.owntracks.android.db.Waypoint;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.services.ServiceMessage;

import android.location.Location;
import android.support.annotation.Nullable;

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

		public GeocodableLocation getLocation() {
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


	public static class BrokerChanged extends E {
		public BrokerChanged() {}
	}

	public static class EndpointStateChanged extends E {
			private final ServiceMessage.EndpointState state;
			private final Object extra;

			public EndpointStateChanged(ServiceMessage.EndpointState state, @Nullable  Object extra) {
				super();
				this.state = state;
				this.extra = extra;
			}

			public ServiceMessage.EndpointState getState() {
				return this.state;
			}

			@Nullable  public Object getExtra() {
				return this.extra;
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
