package com.nuk.meetinggo;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Poll {

    public static int NONE = 7000;
    public static int MULTICHOICE = 7001;
    public static int OPEN_TEXT = 7002;
    public static int RATING = 7003;

    public static String[] pollType = { "Multiple Choice", "Open Text", "Rating" };

    private String vTitle = "";
    private String vContent = "";
    private Date vDate = new Date();
    private Map<String, Integer> vOptions = new LinkedHashMap<String, Integer>();

    public Poll() { }

    public void setTitle(String title) {
        vTitle = title;
    }

    public void setContent(String content) {
        vContent = content;
    }

    // TODO not check duplicated option
    public void addOption(String option) {
        vOptions.put(option, 0);
    }

    public String getTitle() {
        return vTitle;
    }

    public String getContent() {
        return vContent;
    }

    public String[] getOptions() {
        String[] options = {};
        ArrayList<String> list = new ArrayList<String>();

        Iterator it = vOptions.keySet().iterator();
        while(it.hasNext()) {
            list.add((String) it.next());
        }

        return list.toArray(options);
    }

    public Integer[] getNumbers() {
        Integer[] numbers = {};
        ArrayList<Integer> list = new ArrayList<Integer>();

        Iterator it = vOptions.keySet().iterator();
        while(it.hasNext()) {
            String key = (String) it.next();
            list.add((Integer) vOptions.get(key));
        }

        return list.toArray(numbers);
    }
}
