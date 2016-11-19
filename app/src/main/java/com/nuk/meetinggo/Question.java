package com.nuk.meetinggo;

import java.util.Date;

public class Question {

    private String qContent;
    private String qOwner;
    private String qProblem;
    private Boolean qOwnerPublic;
    private Date qDate;
    private String qReply;

    // Content limits
    private static int MIN_CONTENT_LENGTH = 5;
    private static int MAX_CONTENT_LENGTH = 100;

    //TODO change english name
    // Success code
    public static int SUCCESS = 300;

    // Error code
    public static int CONTENT_LENGTH_TOO_SHORT = 401;
    public static int CONTENT_LENGTH_TOO_LONG = 402;

    public Question(String content, String owner, String problem) {
        qContent = content;
        qOwner = owner;
        qProblem = problem;
        qOwnerPublic = true;
        qDate = new Date();
        qReply = "";
    }

    public Question(String content, String owner, String problem, Boolean ownerPublic) {
        qContent = content;
        qOwner = owner;
        qProblem = problem;
        qOwnerPublic = ownerPublic;
        qDate = new Date();
        qReply = "";
    }

    public Question(String content, String owner, String problem, Boolean ownerPublic, Date date, String reply) {
        qContent = content;
        qOwner = owner;
        qProblem = problem;
        qOwnerPublic = ownerPublic;
        qDate = date;
        qReply = reply;
    }

    @Override
    public String toString() {
        return String.format("%s, %s, %s, %s, %s, %s", qContent, qOwner, qProblem,qOwnerPublic, qDate, qReply);
    }

    public int setContent(String content) {
        if(content.length() < MIN_CONTENT_LENGTH) return CONTENT_LENGTH_TOO_SHORT;
        if(content.length() > MAX_CONTENT_LENGTH) return CONTENT_LENGTH_TOO_LONG;
        return SUCCESS;
    }
}
