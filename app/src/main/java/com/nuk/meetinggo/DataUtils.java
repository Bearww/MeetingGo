package com.nuk.meetinggo;

import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static com.nuk.meetinggo.MainFragment.getBackupPath;
import static com.nuk.meetinggo.MainFragment.getLocalPath;

    /*
    *   JSON file structure:
    *
    *   root_OBJ:{
    *       notes_ARR:[
    *           newNote_OBJ:{
    *             "title":"", "body":"", "colour":"", "favoured":true/false,
    *                   "fontSize":14/18/22, "hideBody":true/false},
    *           newNote_OBJ:{
    *             "title":"", "body":"", "colour":"", "favoured":true/false,
    *                   "fontSize":14/18/22, "hideBody":true/false}, etc
    *       ]
    *   };
    */


public class DataUtils {

    public static final String NOTES_FILE_NAME = "notes.json"; // Local notes file name
    public static final String NOTES_ARRAY_NAME = "notes"; // Root object name

    public static final String POLLS_FILE_NAME = "polls.json"; // Local polls file name

    public static final String MEMBERS_FILE_NAME = "members.json"; // Local members file name

    public static final String DOCUMENTS_FILE_NAME = "documents.json"; // Local polls file name

    public static final String QUESTIONS_FILE_NAME = "questions.json"; // Local polls file name

    public static final String RECORDS_FILE_NAME = "records.json"; // Local records file name

    public static final String BACKUP_FOLDER_PATH = "/Swiftnotes"; // Backup folder path
    public static final String BACKUP_FILE_NAME = "swiftnotes_backup.json"; // Backup file name

    // Cloud data constants used in key-value store
    public static final String CLOUD_UPDATE_CODE = "updateCode";

    // Note data constants used in intents and in key-value store
    public static final int NEW_NOTE_REQUEST = 60000;
    public static final String NOTE_REQUEST_CODE = "requestCode";
    public static final String NOTE_ID = "id";
    public static final String NOTE_TITLE = "title";
    public static final String NOTE_BODY = "body";
    public static final String NOTE_LINK = "link";
    public static final String NOTE_COLOUR = "colour";
    public static final String NOTE_FAVOURED = "favoured";
    public static final String NOTE_FONT_SIZE = "fontSize";
    public static final String NOTE_HIDE_BODY = "hideBody";
    public static final String NOTE_RECEIVER = "receiver";

    // Poll data constants used in intents and in key-value store
    public static final int NEW_POLL_REQUEST = 70000;
    public static final String POLL_REQUEST_CODE = "requestCode";
    public static final String POLL_ID = "id";
    public static final String POLL_TOPIC = "topic";
    public static final String POLL_TITLE = "title";
    public static final String POLL_BODY = "body";
    public static final String POLL_CHECK = "check";
    public static final String POLL_COLOUR = "colour";
    public static final String POLL_FAVOURED = "favoured";
    public static final String POLL_FONT_SIZE = "fontSize";
    public static final String POLL_HIDE_BODY = "hideBody";
    public static final String POLL_RECEIVER = "receiver";
    public static final String POLL_ENABLED = "enabled";

    public static final String OPTION_ARRAY = "array";
    public static final String OPTION_ID = "id";
    public static final String OPTION_CONTENT = "content";
    public static final String OPTION_VOTES = "votes";

    // Member data constants used in intents and in key-value store
    public static final int NEW_MEMBER_REQUEST = 80000;
    public static final int LIST_MEMBER_REQUEST = 70001;
    public static final String MEMBER_REQUEST_CODE = "requestCode";
    public static final String MEMBER_ID = "id";
    public static final String MEMBER_NAME = "name";
    public static final String MEMBER_EMAIL = "email";
    public static final String MEMBER_ONLINE = "online";
    public static final String MEMBER_COLOUR = "colour";

    // Document data constants used in intents and in key-value store
    public static final int NEW_DOCUMENT_REQUEST = 90000;
    public static final String DOCUMENT_REQUEST_CODE = "requestCode";
    public static final String DOCUMENT_TITLE = "title";
    public static final String DOCUMENT_REFERENCE = "reference";
    public static final String DOCUMENT_LINK = "link";
    public static final String DOCUMENT_FAVOURED = "favoured";
    public static final String DOCUMENT_RECEIVER = "receiver";

    // Ask data constants used in intents and in key-value store
    public static final int NEW_QUESTION_REQUEST = 100000;
    public static final String QUESTION_REQUEST_CODE = "requestCode";
    public static final String QUESTION_ID = "id";
    public static final String QUESTION_TOPIC = "topic";
    public static final String QUESTION_TITLE = "title";
    public static final String QUESTION_BODY = "body";
    public static final String QUESTION_COLOUR = "colour";
    public static final String QUESTION_FAVOURED = "favoured";
    public static final String QUESTION_FONT_SIZE = "fontSize";
    public static final String QUESTION_RECEIVER = "receiver";

    public static final String ANSWER_ARRAY = "array";
    public static final String ANSWER_CONTENT = "content";
    public static final String ANSWER_OWNER = "owner";
    public static final String ANSWER_FAVOURED = "favoured";

    // Record data constants used in intents and in key-value store
    public static final int NEW_RECORD_REQUEST = 110000;
    public static final String RECORD_REQUEST_CODE = "requestCode";
    public static final String RECORD_TITLE = "title";
    public static final String RECORD_REFERENCE = "reference";
    public static final String RECORD_BODY = "body";
    public static final String RECORD_FAVOURED = "favoured";
    public static final String RECORD_RECEIVER = "receiver";

    /**
     * Wrap 'notes' array into a root object and store in file 'toFile'
     * @param toFile File to store notes into
     * @param notes Array of notes to be saved
     * @return true if successfully saved, false otherwise
     */
    public static boolean saveData(File toFile, JSONArray notes) {
        Boolean successful = false;

        JSONObject root = new JSONObject();

        // If passed notes not null -> wrap in root JSONObject
        if (notes != null) {
            try {
                root.put(NOTES_ARRAY_NAME, notes);

            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        }

        // If passed notes null -> return false
        else
            return false;

        // If file is backup and it doesn't exist -> create file
        if (toFile == getBackupPath()) {
            if (isExternalStorageReadable() && isExternalStorageWritable()) {
                if (!toFile.exists()) {
                    try {
                        Boolean created = toFile.createNewFile();

                        // If file failed to create -> return false
                        if (!created)
                            return false;

                    } catch (IOException e) {
                        e.printStackTrace();
                        return false; // If file creation threw exception -> return false
                    }
                }
            }

            // If external storage not readable/writable -> return false
            else
                return false;
        }

        // If file is local and it doesn't exist -> create file
        else if (toFile == getLocalPath() && !toFile.exists()) {
            try {
                Boolean created = toFile.createNewFile();

                // If file failed to create -> return false
                if (!created)
                    return false;

            } catch (IOException e) {
                e.printStackTrace();
                return false; // If file creation threw exception -> return false
            }
        }


        BufferedWriter bufferedWriter = null;

        try {
            // Initialize BufferedWriter with FileWriter and write root object to file
            bufferedWriter = new BufferedWriter(new FileWriter(toFile));
            bufferedWriter.write(root.toString());

            // If we got to this stage without throwing an exception -> set successful to true
            successful = true;

        } catch (IOException e) {
            // If something went wrong in try block -> set successful to false
            successful = false;
            e.printStackTrace();

        } finally {
            // Finally, if bufferedWriter not null -> flush and close it
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.flush();
                    bufferedWriter.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return successful;
    }


    /**
     * Read from file 'fromFile' and return parsed JSONArray of notes
     * @param fromFile File we are reading from
     * @return JSONArray of notes
     */
    public static JSONArray retrieveData(File fromFile) {
        JSONArray notes = null;

        // If file is backup and it doesn't exist -> return null
        if (fromFile == getBackupPath()) {
            if (isExternalStorageReadable() && !fromFile.exists()) {
                return null;
            }
        }

        /*
         * If file is local and it doesn't exist ->
         * Initialize notes JSONArray as new and save into local file
         */
        else if (fromFile == getLocalPath() && !fromFile.exists()) {
            notes = new JSONArray();

            Boolean successfulSaveToLocal = saveData(fromFile, notes);

            // If save successful -> return new notes
            if (successfulSaveToLocal) {
                return notes;
            }

            // Else -> return null
            return null;
        }


        JSONObject root = null;
        BufferedReader bufferedReader = null;

        try {
            // Initialize BufferedReader, read from 'fromFile' and store into root object
            bufferedReader = new BufferedReader(new FileReader(fromFile));

            StringBuilder text = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                text.append(line);
            }

            root = new JSONObject(text.toString());

        } catch (IOException | JSONException e) {
            e.printStackTrace();

        } finally {
            // Finally, if bufferedReader not null -> close it
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // If root is not null -> get notes array from root object
        if (root != null) {
            try {
                notes = root.getJSONArray(NOTES_ARRAY_NAME);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Return fetches notes < May return null! >
        return notes;
    }


    /**
     * Create new JSONArray of notes from 'from' without the notes at positions in 'selectedNotes'
     * @param from Main notes array to delete from
     * @param selectedNotes ArrayList of Integer which represent note positions to be deleted
     * @return New JSONArray of notes without the notes at positions 'selectedNotes'
     */
    public static JSONArray deleteNotes(JSONArray from, ArrayList<Integer> selectedNotes) {
        // Init new JSONArray
        JSONArray newNotes = new JSONArray();

        // Loop through main notes
        for (int i = 0; i < from.length(); i++) {
            // If array of positions to delete doesn't contain current position -> put in new array
            if (!selectedNotes.contains(i)) {
                try {
                    newNotes.put(from.get(i));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Finally, return the new notes
        return newNotes;
    }

    /**
     * Create new JSONArray of polls from 'from' without the notes at positions in 'selectedPolls'
     * @param from Main polls array to delete from
     * @param selectedPolls ArrayList of Integer which represent poll positions to be deleted
     * @return New JSONArray of polls without the polls at positions 'selectedPolls'
     */
    public static JSONArray deletePolls(JSONArray from, ArrayList<Integer> selectedPolls) {
        // Init new JSONArray
        JSONArray newPolls = new JSONArray();

        // Loop through main polls
        for (int i = 0; i < from.length(); i++) {
            // If array of positions to delete doesn't contain current position -> put in new array
            if (!selectedPolls.contains(i)) {
                try {
                    newPolls.put(from.get(i));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Finally, return the new polls
        return newPolls;
    }

    /**
     * Create new JSONArray of options from 'from' without the options at positions in 'selectedOptions'
     * @param from Main options array to delete from
     * @param selectedOptions ArrayList of Integer which represent option positions to be deleted
     * @return New JSONArray of options without the options at positions 'selectedOptions'
     */
    public static JSONArray deleteOptions(JSONArray from, ArrayList<Integer> selectedOptions) {
        // Init new JSONArray
        JSONArray newOptions = new JSONArray();

        // Loop through main options
        for (int i = 0; i < from.length(); i++) {
            // If array of positions to delete doesn't contain current position -> put in new array
            if (!selectedOptions.contains(i)) {
                try {
                    newOptions.put(from.get(i));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Finally, return the new options
        return newOptions;
    }

    /**
     * Create new JSONArray of members from 'from' without the members at positions in 'selectedMembers'
     * @param from Main members array to delete from
     * @param selectedMembers ArrayList of Integer which represent member positions to be deleted
     * @return New JSONArray of members without the members at positions 'selectedMembers'
     */
    public static JSONArray deleteMembers(JSONArray from, ArrayList<Integer> selectedMembers) {
        // Init new JSONArray
        JSONArray newMembers = new JSONArray();

        // Loop through main members
        for (int i = 0; i < from.length(); i++) {
            // If array of positions to delete doesn't contain current position -> put in new array
            if (!selectedMembers.contains(i)) {
                try {
                    newMembers.put(from.get(i));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Finally, return the new members
        return newMembers;
    }

    /**
     * Create new JSONArray of documents from 'from' without the documents at positions in 'selectedDocuments'
     * @param from Main documents array to delete from
     * @param selectedDocuments ArrayList of Integer which represent document positions to be deleted
     * @return New JSONArray of documents without the documents at positions 'selectedDocuments'
     */
    public static JSONArray deleteDocuments(JSONArray from, ArrayList<Integer> selectedDocuments) {
        // Init new JSONArray
        JSONArray newDocuments = new JSONArray();

        // Loop through main documents
        for (int i = 0; i < from.length(); i++) {
            // If array of positions to delete doesn't contain current position -> put in new array
            if (!selectedDocuments.contains(i)) {
                try {
                    newDocuments.put(from.get(i));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Finally, return the new documents
        return newDocuments;
    }

    /**
     * Create new JSONArray of questions from 'from' without the questions at positions in 'selectedQuestions'
     * @param from Main questions array to delete from
     * @param selectedQuestions ArrayList of Integer which represent question positions to be deleted
     * @return New JSONArray of questions without the questions at positions 'selectedQuestions'
     */
    public static JSONArray deleteQuestions(JSONArray from, ArrayList<Integer> selectedQuestions) {
        // Init new JSONArray
        JSONArray newQuestions = new JSONArray();

        // Loop through main questions
        for (int i = 0; i < from.length(); i++) {
            // If array of positions to delete doesn't contain current position -> put in new array
            if (!selectedQuestions.contains(i)) {
                try {
                    newQuestions.put(from.get(i));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Finally, return the new questions
        return newQuestions;
    }

    /**
     * Create new JSONArray of records from 'from' without the records at positions in 'selectedRecords'
     * @param from Main records array to delete from
     * @param selectedRecords ArrayList of Integer which represent record positions to be deleted
     * @return New JSONArray of records without the records at positions 'selectedRecords'
     */
    public static JSONArray deleteRecords(JSONArray from, ArrayList<Integer> selectedRecords) {
        // Init new JSONArray
        JSONArray newRecords = new JSONArray();

        // Loop through main records
        for (int i = 0; i < from.length(); i++) {
            // If array of positions to delete doesn't contain current position -> put in new array
            if (!selectedRecords.contains(i)) {
                try {
                    newRecords.put(from.get(i));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Finally, return the new records
        return newRecords;
    }

    /**
     * Check if external storage is writable or not
     * @return true if writable, false otherwise
     */
    public static boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * Check if external storage is readable or not
     * @return true if readable, false otherwise
     */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
