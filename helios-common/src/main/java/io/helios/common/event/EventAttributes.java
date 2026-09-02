public record EventAttributes(
        String activityId,
        String activityType,
        String timerId,
        String markerName,
        String signalName,
        String childWorkflowId) {


        public static final EventAttributes EMPTY =
            new EventAttributes(null, null, null, null, null, null);

        public static EventAttributes activity(String activityId, String activityType) {
                return new EventAttributes(activityId, activityType, null, null, null, null);
        }

        public static EventAttributes timer(String timerId) {
                return new EventAttributes(null, null, timerId, null, null, null);
        }

        public static EventAttributes marker(String markerName) {
                return new EventAttributes(null, null, null, markerName, null, null);
        }

        public static EventAttributes signal(String signalName) {
                return new EventAttributes(null, null, null, null, signalName, null);
        }

        public boolean isEmpty() {
                return this.equals(EMPTY);
        }        


}
