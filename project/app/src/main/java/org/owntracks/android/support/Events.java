package org.owntracks.android.support;

import java.util.Date;

import org.owntracks.android.db.Waypoint;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.services.MessageProcessor;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class Events {
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


	public static class BrokerChanged extends E {
		public BrokerChanged() {}
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

	public static class ServiceStarted extends E {
	}
}
