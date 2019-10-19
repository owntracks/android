package org.owntracks.android.support;

import java.util.Date;

import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.model.FusedContact;

public class Events {
    public static abstract class E {
        final Date date;
        E() {
            this.date = new Date();
        }
        public Date getDate() {
            return this.date;
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

    public static class MonitoringChanged extends E {
        final int newModeId;

        public MonitoringChanged(int newModeId) {
            this.newModeId = newModeId;
        }
        public int getNewModeId() {
            return newModeId;
        }
    }

	static class WaypointTransition extends E {
		final WaypointModel w;
		final int transition;
        //TODO
		public WaypointTransition(WaypointModel w, int transition) {
			super();
			this.w = w;
			this.transition = transition;
		}

		public WaypointModel getWaypoint() {
			return this.w;
		}

		public int getTransition() {
			return this.transition;
		}

	}

	public static class EndpointChanged extends E {
		public EndpointChanged() {}
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

    public static class QueueChanged extends E {
        int length;

        public QueueChanged() {
        }
        public QueueChanged withNewLength(int length) {
            this.length = length;
            return this;
        }
        public int getNewLength() {
            return length;
        }

    }
    public static class WaypointEvent extends E {
        private final WaypointModel m;

        WaypointEvent(WaypointModel m) {
            this.m = m;
        }
        public WaypointModel getWaypointModel() {
            return this.m;
        }
    }

    public static class WaypointAdded extends WaypointEvent {
        public WaypointAdded(WaypointModel m) {
            super(m);
        }
    }
    public static class WaypointUpdated extends WaypointEvent {
        public WaypointUpdated(WaypointModel m) {
            super(m);
        }
    }
    public static class WaypointRemoved extends WaypointEvent {
        public WaypointRemoved(WaypointModel m) {
            super(m);
        }
    }

    public static class FusedContactAdded extends E {
        private final FusedContact fusedContact;

        public FusedContactAdded(FusedContact c) {
            this.fusedContact = c;
        }
        public FusedContact getContact() {
            return this.fusedContact;
        }
    }

    public static class FusedContactRemoved extends E {
        private final FusedContact fusedContact;

        public FusedContactRemoved(FusedContact c) {
            this.fusedContact = c;
        }
        public FusedContact getContact() {
            return this.fusedContact;
        }
    }

}
