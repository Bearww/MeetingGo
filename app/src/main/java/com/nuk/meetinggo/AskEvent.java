package com.nuk.meetinggo;

import java.util.ArrayList;

public class AskEvent {

    ArrayList<Question> askList = new ArrayList<>();

    public AskEvent() {
        insertTestAsk();     // insert test data
    }

    public void insertTestAsk() {
        addAsk("Hello", "bearww", "");
        addAsk("Bonyu", "bearww", "");
    }

    public void addAsk(String content, String owner, String question) {
        askList.add(new Question(content, owner, question));
    }

    public void addAsk(String content, String owner, String question, Boolean checked) {
        askList.add(new Question(content, owner, question, checked));
    }

    public String[] getAsks() {
        String[] asks = {};
        ArrayList<String> list = new ArrayList<String>();

        for(Question q : askList) {
            list.add(q.toString());
        }

        return list.toArray(asks);
    }
}
