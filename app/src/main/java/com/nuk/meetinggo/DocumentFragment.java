package com.nuk.meetinggo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static com.nuk.meetinggo.DataUtils.DOCUMENTS_FILE_NAME;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_FAVOURED;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_LINK;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_RECEIVER;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_REFERENCE;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_REQUEST_CODE;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_TITLE;
import static com.nuk.meetinggo.DataUtils.NEW_DOCUMENT_REQUEST;
import static com.nuk.meetinggo.DataUtils.deleteDocuments;
import static com.nuk.meetinggo.DataUtils.retrieveData;
import static com.nuk.meetinggo.DataUtils.saveData;
import static com.nuk.meetinggo.MeetingInfo.meetingID;

public class DocumentFragment extends Fragment implements AdapterView.OnItemClickListener,
        Toolbar.OnMenuItemClickListener, AbsListView.MultiChoiceModeListener,
        SearchView.OnQueryTextListener, DetachableResultReceiver.Receiver {

    private static File localPath;

    // Layout components
    private static ListView listView;
    private ImageButton newDocument;
    private TextView noDocuments;
    private Toolbar toolbar;
    private MenuItem searchMenu;

    private static JSONArray documents; // Main documents array
    private static DocumentAdapter adapter; // Custom ListView documents adapter

    // Array of selected positions for deletion
    public static ArrayList<Integer> checkedArray = new ArrayList<Integer>();
    public static boolean deleteActive = false; // True if delete mode is active, false otherwise

    // For disabling long clicks, favourite clicks and modifying the item click pattern
    public static boolean searchActive = false;
    private ArrayList<Integer> realIndexesOfSearchResults; // To keep track of real indexes in searched documents

    private int lastFirstVisibleItem = -1; // Last first item seen in list view scroll changed
    private float newDocumentButtonBaseYCoordinate; // Base Y coordinate of newDocument button

    private AlertDialog addDocumentDialog;

    private DetachableResultReceiver mReceiver;

    private LinkCloudTask linkTask;

    private static int GET_DOCUMENTS = 60001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get document file from cloud
        linkTask = new LinkCloudTask(GET_DOCUMENTS);
        linkTask.execute((Void) null);

        // Initialize local file path and backup file path
        localPath = new File(getContext().getFilesDir() + "/" + meetingID + DOCUMENTS_FILE_NAME);

        // Init documents array
        documents = new JSONArray();

        // Retrieve from local path
        JSONArray tempDocuments = retrieveData(localPath);

        // If not null -> equal main documents to retrieved documents
        if (tempDocuments != null)
            documents = tempDocuments;

        mReceiver = new DetachableResultReceiver(new Handler());
        mReceiver.setReceiver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_document, container, false);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarMain);
        listView = (ListView) view.findViewById(R.id.listView);
        newDocument = (ImageButton) view.findViewById(R.id.newDocument);
        noDocuments = (TextView) view.findViewById(R.id.noDocuments);

        if (toolbar != null)
            initToolbar();

        newDocumentButtonBaseYCoordinate = newDocument.getY();

        // Initialize DocumentAdapter with documents array
        adapter = new DocumentAdapter(getContext(), documents);
        listView.setAdapter(adapter);

        // Set item click, multi choice and scroll listeners
        listView.setOnItemClickListener(this);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(this);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // If last first visible item not initialized -> set to current first
                if (lastFirstVisibleItem == -1)
                    lastFirstVisibleItem = view.getFirstVisiblePosition();

                // If scrolled up -> hide newDocument button
                if (view.getFirstVisiblePosition() > lastFirstVisibleItem)
                    newDocumentButtonVisibility(false);

                    // If scrolled down and delete/search not active -> show newDocument button
                else if (view.getFirstVisiblePosition() < lastFirstVisibleItem &&
                        !deleteActive && !searchActive) {

                    newDocumentButtonVisibility(true);
                }

                // Set last first visible item to current
                lastFirstVisibleItem = view.getFirstVisiblePosition();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                                 int totalItemCount) {}
        });


        // If newDocument button clicked -> Start EditDocumentFragment intent with NEW_DOCUMENT_REQUEST as request
        newDocument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addDocumentDialog.show();
            }
        });

        // TODO invisible new document button
        newDocument.setVisibility(View.GONE);

        // If no documents -> show 'Press + to add new document' text, invisible otherwise
        if (documents.length() == 0)
            noDocuments.setVisibility(View.VISIBLE);

        else
            noDocuments.setVisibility(View.INVISIBLE);

        initDialogs(getContext());

        return view;
    }

    /**
     * Initialize toolbar with required components such as
     * - title, menu/OnMenuItemClickListener and searchView -
     */
    protected void initToolbar() {
        toolbar.setTitle(R.string.app_name);

        // Inflate menu_main to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.menu_main);

        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(this);

        Menu menu = toolbar.getMenu();

        if (menu != null) {
            // Get 'Search' menu item
            searchMenu = menu.findItem(R.id.action_search);

            if (searchMenu != null) {
                // If the item menu not null -> get it's support action view
                SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenu);

                if (searchView != null) {
                    // If searchView not null -> set query hint and open/query/close listeners
                    searchView.setQueryHint(getString(R.string.action_search));
                    searchView.setOnQueryTextListener(this);

                    MenuItemCompat.setOnActionExpandListener(searchMenu,
                            new MenuItemCompat.OnActionExpandListener() {

                                @Override
                                public boolean onMenuItemActionExpand(MenuItem item) {
                                    searchActive = true;
                                    newDocumentButtonVisibility(false);
                                    // Disable long-click on listView to prevent deletion
                                    listView.setLongClickable(false);

                                    // Init realIndexes array
                                    realIndexesOfSearchResults = new ArrayList<Integer>();
                                    for (int i = 0; i < documents.length(); i++)
                                        realIndexesOfSearchResults.add(i);

                                    adapter.notifyDataSetChanged();

                                    return true;
                                }

                                @Override
                                public boolean onMenuItemActionCollapse(MenuItem item) {
                                    searchEnded();
                                    return true;
                                }
                            });
                }
            }
        }
    }

    /**
     * Implementation of AlertDialogs such as
     * - backupCheckDialog, backupOKDialog, restoreCheckDialog, restoreFailedDialog -
     * @param context The Activity context of the dialogs; in this case MainFragment context
     */
    protected void initDialogs(final Context context) {
        /*
         * Add document dialog
         *  If not sure -> dismiss
         *  If yes -> check if document title length > 0
         *    If yes -> save current document
         */
        LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_add_poll, null);
    }

    /**
     * If item clicked in list view -> Start EditDocumentFragment intent with position as requestCode
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Create fragment and give it an argument
        ViewDocumentFragment nextFragment = new ViewDocumentFragment();
        Bundle args = new Bundle();
        args.putInt(DOCUMENT_REQUEST_CODE, NEW_DOCUMENT_REQUEST);
        args.putParcelable(DOCUMENT_RECEIVER, mReceiver);
        Log.i("[MF]", "Put receiver " + mReceiver.toString());

        // If search is active -> use position from realIndexesOfSearchResults for EditDocumentFragment
        if (searchActive) {
            int newPosition = realIndexesOfSearchResults.get(position);

            try {
                // Package selected document content and send to EditDocumentFragment
                args.putString(DOCUMENT_TITLE, documents.getJSONObject(newPosition).getString(DOCUMENT_TITLE));
                args.putString(DOCUMENT_LINK, documents.getJSONObject(newPosition).getString(DOCUMENT_LINK));
                args.putString(DOCUMENT_REFERENCE, documents.getJSONObject(newPosition).getString(DOCUMENT_REFERENCE));

            } catch (JSONException e) {
                e.printStackTrace();
            }

            args.putInt(DOCUMENT_REQUEST_CODE, newPosition);
        }

        // If search is not active -> use normal position for EditDocumentFragment
        else {
            try {
                // Package selected document content and send to EditDocumentFragment
                args.putString(DOCUMENT_TITLE, documents.getJSONObject(position).getString(DOCUMENT_TITLE));
                args.putString(DOCUMENT_LINK, documents.getJSONObject(position).getString(DOCUMENT_LINK));
                args.putString(DOCUMENT_REFERENCE, documents.getJSONObject(position).getString(DOCUMENT_REFERENCE));

            } catch (JSONException e) {
                e.printStackTrace();
            }

            args.putInt(DOCUMENT_REQUEST_CODE, position);
        }

        nextFragment.setArguments(args);

        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.layout_container, nextFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    /**
     * Item clicked in Toolbar menu callback method
     * @param menuItem Item clicked
     * @return true if click detected and logic finished, false otherwise
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return false;
    }

    /**
     * During multi-choice menu_delete selection mode, callback method if items checked changed
     * @param mode ActionMode of selection
     * @param position Position checked
     * @param id ID of item, if exists
     * @param checked true if checked, false otherwise
     */
    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        // If item checked -> add to array
        if (checked)
            checkedArray.add(position);

            // If item unchecked
        else {
            int index = -1;

            // Loop through array and find index of item unchecked
            for (int i = 0; i < checkedArray.size(); i++) {
                if (position == checkedArray.get(i)) {
                    index = i;
                    break;
                }
            }

            // If index was found -> remove the item
            if (index != -1)
                checkedArray.remove(index);
        }

        // Set Toolbar title to 'x Selected'
        mode.setTitle(checkedArray.size() + " " + getString(R.string.action_delete_selected_number));
        adapter.notifyDataSetChanged();
    }

    /**
     * Callback method when 'Delete' icon pressed
     * @param mode ActionMode of selection
     * @param item MenuItem clicked, in our case just action_delete
     * @return true if clicked, false otherwise
     */
    @Override
    public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.dialog_delete)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Pass documents and checked items for deletion array to 'deleteDocuments'
                            documents = deleteDocuments(documents, checkedArray);

                            // Create and set new adapter with new documents array
                            adapter = new DocumentAdapter(getContext(), documents);
                            listView.setAdapter(adapter);

                            // Attempt to save documents to local file
                            Boolean saveSuccessful = saveData(localPath, documents);

                            // If save successful -> toast successfully deleted
                            if (saveSuccessful) {
                                Toast toast = Toast.makeText(getContext(),
                                        getResources().getString(R.string.toast_deleted),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }

                            // Smooth scroll to top
                            listView.post(new Runnable() {
                                public void run() {
                                    listView.smoothScrollToPosition(0);
                                }
                            });

                            // If no documents -> show 'Press + to add new document' text, invisible otherwise
                            if (documents.length() == 0)
                                noDocuments.setVisibility(View.VISIBLE);

                            else
                                noDocuments.setVisibility(View.INVISIBLE);

                            mode.finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();

            return true;
        }

        return false;
    }

    // Long click detected on ListView item -> start selection ActionMode (delete mode)
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_delete, menu); // Inflate 'menu_delete' menu
        deleteActive = true; // Set deleteActive to true as we entered delete mode
        newDocumentButtonVisibility(false); // Hide newDocument button
        adapter.notifyDataSetChanged(); // Notify adapter to hide favourite buttons

        return true;
    }

    // Selection ActionMode finished (delete mode ended)
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        checkedArray = new ArrayList<Integer>(); // Reset checkedArray
        deleteActive = false; // Set deleteActive to false as we finished delete mode
        newDocumentButtonVisibility(true); // Show newDocument button
        adapter.notifyDataSetChanged(); // Notify adapter to show favourite buttons
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    /**
     * Method to show and hide the newDocument button
     * @param isVisible true to show button, false to hide
     */
    protected void newDocumentButtonVisibility(boolean isVisible) {
        if (isVisible) {
            newDocument.animate().cancel();
            newDocument.animate().translationY(newDocumentButtonBaseYCoordinate);
        } else {
            newDocument.animate().cancel();
            newDocument.animate().translationY(newDocumentButtonBaseYCoordinate + 500);
        }
    }


    /**
     * Callback method for 'searchView' menu item widget text change
     * @param s String which changed
     * @return true if text changed and logic finished, false otherwise
     */
    @Override
    public boolean onQueryTextChange(String s) {
        s = s.toLowerCase(); // Turn string into lowercase

        // If query text length longer than 0
        if (s.length() > 0) {
            // Create new JSONArray and reset realIndexes array
            JSONArray documentsFound = new JSONArray();
            realIndexesOfSearchResults = new ArrayList<Integer>();

            // Loop through main documents list
            for (int i = 0; i < documents.length(); i++) {
                JSONObject document = null;

                // Get document at position i
                try {
                    document = documents.getJSONObject(i);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If document not null and title/body contain query text
                // -> Put in new documents array and add i to realIndexes array
                if (document != null) {
                    try {
                        if (document.getString(DOCUMENT_TITLE).toLowerCase().contains(s) ||
                                document.getString(DOCUMENT_REFERENCE).toLowerCase().contains(s)) {

                            documentsFound.put(document);
                            realIndexesOfSearchResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Create and set adapter with documentsFound to refresh ListView
            DocumentAdapter searchAdapter = new DocumentAdapter(getContext(), documentsFound);
            listView.setAdapter(searchAdapter);
        }

        // If query text length is 0 -> re-init realIndexes array (0 to length) and reset adapter
        else {
            realIndexesOfSearchResults = new ArrayList<Integer>();
            for (int i = 0; i < documents.length(); i++)
                realIndexesOfSearchResults.add(i);

            adapter = new DocumentAdapter(getContext(), documents);
            listView.setAdapter(adapter);
        }

        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    /**
     * When search mode is finished
     * Collapse searchView widget, searchActive to false, reset adapter, enable listView long clicks
     * and show newDocument button
     */
    protected void searchEnded() {
        searchActive = false;
        adapter = new DocumentAdapter(getContext(), documents);
        listView.setAdapter(adapter);
        listView.setLongClickable(true);
        newDocumentButtonVisibility(true);
    }

    /**
     * Favourite or un-favourite the document at position
     * @param context application context
     * @param favourite true to favourite, false to un-favourite
     * @param position position of document
     */
    public static void setFavourite(Context context, boolean favourite, int position) {
        JSONObject newFavourite = null;

        // Get document at position and store in newFavourite
        try {
            newFavourite = documents.getJSONObject(position);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (newFavourite != null) {
            if (favourite) {
                // Set favoured to true
                try {
                    newFavourite.put(DOCUMENT_FAVOURED, true);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // If favoured document is not at position 0
                // Sort documents array so favoured document is first
                if (position > 0) {
                    JSONArray newArray = new JSONArray();

                    try {
                        newArray.put(0, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Copy contents to new sorted array without favoured element
                    for (int i = 0; i < documents.length(); i++) {
                        if (i != position) {
                            try {
                                newArray.put(documents.get(i));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Equal main documents array with new sorted array and reset adapter
                    documents = newArray;
                    adapter = new DocumentAdapter(context, documents);
                    listView.setAdapter(adapter);

                    // Smooth scroll to top
                    listView.post(new Runnable() {
                        public void run() {
                            listView.smoothScrollToPosition(0);
                        }
                    });
                }

                // If favoured document was first -> just update object in documents array and notify adapter
                else {
                    try {
                        documents.put(position, newFavourite);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    adapter.notifyDataSetChanged();
                }
            }

            // If document not favourite -> set favoured to false and notify adapter
            else {
                try {
                    newFavourite.put(DOCUMENT_FAVOURED, false);
                    documents.put(position, newFavourite);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                adapter.notifyDataSetChanged();
            }
        }

        // Save documents to local file
        saveData(localPath, documents);
    }

    /**
     * Callback method when EditDocumentFragment finished adding new document or editing existing document
     * @param requestCode requestCode for intent sent, in our case either NEW_DOCUMENT_REQUEST or position
     * @param resultCode resultCode from activity, either RESULT_OK or RESULT_CANCELED
     * @param resultData Data bundle passed back from EditDocumentFragment
     */
    @Override
    public void onReceiveResult(int requestCode, int resultCode, Bundle resultData) {
        if (resultCode == Activity.RESULT_OK) {
            // If search was active -> call 'searchEnded' method
            if (searchActive && searchMenu != null)
                searchMenu.collapseActionView();
        }
    }

    @Override
    public void onDestroy() {
        mReceiver.clearReceiver();
        super.onDestroy();
    }

    /**
     * Represents an asynchronous link cloud task used to request/send data
     */
    public class LinkCloudTask extends AsyncTask<Void, Void, Boolean> {

        private final String URL_DOCUMENTS = LinkCloud.DOC_INFO + MeetingInfo.meetingID;

        private int requestCode;

        private Boolean mLinkSuccess;
        private String mLinkData;

        LinkCloudTask(int request) {
            requestCode = request;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            if (requestCode == GET_DOCUMENTS) {
                try {
                    JSONObject object = LinkCloud.request(URL_DOCUMENTS);
                    Map<String, String> links = LinkCloud.getLink(object);

                    for (String key : links.keySet())
                        Log.i("[DF]", key + " " + links.get(key));

                    // TODO i don't know how to get
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            linkTask = null;

            if(success) {

            }
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
        }
    }
}
