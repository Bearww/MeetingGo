package com.nuk.meetinggo;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;

import static com.nuk.meetinggo.DataUtils.NOTES_FILE_NAME;
import static com.nuk.meetinggo.MeetingInfo.meetingID;


/**
 * A simple {@link Fragment} subclass.
 */
public class FutureMeetingFragment extends Fragment {

    private static File localPath, backupPath;

    // Layout components
    private static ListView listView;
    private ImageButton newNote;
    private TextView noNotes;
    private Toolbar toolbar;
    private MenuItem searchMenu;

    private static JSONArray notes; // Main notes array
    private static NoteAdapter adapter; // Custom ListView notes adapter

    // Array of selected positions for deletion
    public static ArrayList<Integer> checkedArray = new ArrayList<Integer>();
    public static boolean deleteActive = false; // True if delete mode is active, false otherwise

    // For disabling long clicks, favourite clicks and modifying the item click pattern
    public static boolean searchActive = false;
    private ArrayList<Integer> realIndexesOfSearchResults; // To keep track of real indexes in searched notes

    private int lastFirstVisibleItem = -1; // Last first item seen in list view scroll changed
    private float newNoteButtonBaseYCoordinate; // Base Y coordinate of newNote button

    private DetachableResultReceiver mReceiver;

    //private LinkCloudTask linkTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize local file path and backup file path
        localPath = new File(getContext().getFilesDir() + "/" + meetingID + NOTES_FILE_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_future_meeting, container, false);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarMain);
        listView = (ListView) view.findViewById(R.id.listView);
        newNote = (ImageButton) view.findViewById(R.id.newNote);
        noNotes = (TextView) view.findViewById(R.id.noNotes);

        return view;
    }

}
