package org.owntracks.android.support;

import java.util.Date;

import org.owntracks.android.db.Waypoint;

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
		final int transition;

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

}
