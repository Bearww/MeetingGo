package com.nuk.meetinggo;

public class MeetingInfo {

    public static int meetingID;
    public static int groupID;

    public static String controller = MemberInfo.memberID;
    public static String chairman = "";
    public static String presenter = "";

    public static Boolean isController(String id) {
        return (controller.compareTo(id) == 0);
    }

    public static Boolean isChairman(String id) {
        return (chairman.compareTo(id) == 0);
    }

    public static Boolean isPresenter(String id) {
        return (presenter.compareTo(id) == 0);
    }

    public static Boolean getControlable(String id) { return isPresenter(id) || isController(id); }
}
