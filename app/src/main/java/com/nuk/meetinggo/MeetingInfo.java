package com.nuk.meetinggo;

public class MeetingInfo {

    public final static String TAG_TAB_TOPIC = "topic";
    public final static String TAG_TAB_MEMBER = "member";
    public final static String TAG_TAB_DOCUMENT = "document";
    public final static String TAG_TAB_QUESTION = "question";
    public final static String TAG_TAB_POLL = "poll";
    public final static String TAG_TAB_RECORD = "record";

    //----------------------------------------------------------------------------------------------//

    public static int meetingID;
    public static int groupID;
    public static int topicID;

    public final static String CONTENT_LINK = "link";
    public final static String CONTENT_FORM = "form";
    public final static String CONTENT_ADDRESS = "addr";
    public final static String CONTENT_TOPIC_ID = "topic_id";
    
    public static String GET_MEETING_INFO = "";

    public final static String CONTENT_START = "meeting_start";
    public static String GET_MEETING_START = "";

    public final static String CONTENT_TOPIC = "get_topic_list";
    public static String GET_MEETING_TOPIC = "";

    public final static String CONTENT_MEMBER = "get_member_list";
    public static String GET_MEETING_MEMBER = "";

    public final static String CONTENT_DOCUMENT = "get_doc_list";
    public static String GET_MEETING_DOCUMENT = "";

    public final static String CONTENT_QUESTION = "get_question";
    public static String GET_MEETING_QUESTION = "";

    public final static String CONTENT_ANSWER = "get_answer";
    public static String GET_MEETING_ANSWER = "";

    public final static String CONTENT_POLL = "get_voting_result";
    public static String GET_MEETING_POLL = "";

    public final static String CONTENT_RECORD = "get_voting_result";
    public static String GET_MEETING_RECORD = "";

    public final static String CONTENT_TOPIC_BODY = "get_topic_content";
    public static String GET_TOPIC_BODY = "";

    public final static String CONTENT_TOPIC_DOCUMENT = "get_topic_doc_list";
    public static String GET_TOPIC_DOCUMENT = "";

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
