package com.nuk.meetinggo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
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
import android.webkit.MimeTypeMap;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static com.nuk.meetinggo.DataUtils.APP_FOLDER_NAME;
import static com.nuk.meetinggo.DataUtils.CLOUD_UPDATE_CODE;
import static com.nuk.meetinggo.DataUtils.DOCUMENTS_FILE_NAME;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_DOWNLOADED;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_FAVOURED;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_LINK;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_RECEIVER;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_REQUEST_CODE;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_TITLE;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_TOPIC;
import static com.nuk.meetinggo.DataUtils.DOCUMENT_VIEW;
import static com.nuk.meetinggo.DataUtils.NEW_DOCUMENT_REQUEST;
import static com.nuk.meetinggo.DataUtils.deleteDocuments;
import static com.nuk.meetinggo.DataUtils.retrieveData;
import static com.nuk.meetinggo.DataUtils.saveData;
import static com.nuk.meetinggo.LinkCloud.CLOUD_UPDATE;
import static com.nuk.meetinggo.MeetingInfo.meetingID;

public class DocumentFragment extends Fragment implements AdapterView.OnItemClickListener,
        Toolbar.OnMenuItemClickListener, AbsListView.MultiChoiceModeListener,
        SearchView.OnQueryTextListener, DetachableResultReceiver.Receiver {

    private static File localPath;
    private static String localDirectory;

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
    private static ArrayList<Integer> realIndexesOfFilterResults; // To keep track of real indexes in filtered documents
    private static JSONArray filteredDocuments; // Filtered documents array

    private int lastFirstVisibleItem = -1; // Last first item seen in list view scroll changed
    private float newDocumentButtonBaseYCoordinate; // Base Y coordinate of newDocument button

    // declare the dialog as a member field of your activity
    ProgressDialog progressDialog;
    private AlertDialog addDocumentDialog;

    private DetachableResultReceiver mReceiver;

    private LinkCloudTask linkTask;
    private DownloadFileTask downloadTask;

    private static int GET_DOCUMENTS = 60001;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize local file path and backup file path
        localPath = new File(getContext().getFilesDir() + "/" + meetingID + DOCUMENTS_FILE_NAME);
        localDirectory = Environment.getExternalStorageDirectory() + APP_FOLDER_NAME + MeetingInfo.meetingID;

        // Init documents array
        documents = new JSONArray();

        // Retrieve from local path
        JSONArray tempDocuments = retrieveData(localPath);

        // If not null -> equal main documents to retrieved documents
        if (tempDocuments != null)
            documents = tempDocuments;

        // Filter topic id from documents
        filteredDocuments = filterDocument();

        mReceiver = new DetachableResultReceiver(new Handler());
        mReceiver.setReceiver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_document, container, false);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarMain);
        listView = (ListView) view.findViewById(R.id.beginList);
        newDocument = (ImageButton) view.findViewById(R.id.newDocument);
        noDocuments = (TextView) view.findViewById(R.id.noDocuments);

        if (toolbar != null)
            initToolbar();

        newDocumentButtonBaseYCoordinate = newDocument.getY();

        // Initialize DocumentAdapter with documents array
        adapter = new DocumentAdapter(getContext(), filteredDocuments);
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


        // If newDocument button clicked -> Start ViewDocumentFragment intent with NEW_DOCUMENT_REQUEST as request
        newDocument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addDocumentDialog.show();
            }
        });

        // TODO invisible new document button
        newDocument.setVisibility(View.GONE);

        // If no documents -> show 'Press + to add new document' text, invisible otherwise
        if (filteredDocuments.length() == 0)
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
                                    for (int i = 0; i < filteredDocuments.length(); i++)
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

        // Instantiate progress dialog
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("A message");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);

        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (downloadTask != null)
                    downloadTask.cancel(true);
            }
        });
    }

    /**
     * Implementation of filter document
     */
    protected static JSONArray filterDocument() {
        
        JSONArray documentsFiltered = new JSONArray();
        realIndexesOfFilterResults = new ArrayList<>();
        
        if (MeetingInfo.topicID != 0) {
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
                        if (document.getString(DOCUMENT_TOPIC).equals(String.valueOf(MeetingInfo.topicID))) {

                            documentsFiltered.put(document);
                            realIndexesOfFilterResults.add(i);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else {
            for (int i = 0; i < documents.length(); i++) {
                try {
                    documentsFiltered.put(documents.getJSONObject(i));
                    realIndexesOfFilterResults.add(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return documentsFiltered;
    }

    /**
     * If item clicked in list view -> Start ViewDocumentFragment intent with position as requestCode
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Boolean isViewable = false;
        
        // Create fragment and give it an argument
        ViewDocumentFragment nextFragment = new ViewDocumentFragment();
        Bundle args = new Bundle();
        args.putInt(DOCUMENT_REQUEST_CODE, NEW_DOCUMENT_REQUEST);
        args.putParcelable(DOCUMENT_RECEIVER, mReceiver);
        Log.i("[DF]", "Put receiver " + mReceiver.toString());

        // If search is active -> use position from realIndexesOfSearchResults for ViewDocumentFragment
        if (searchActive) {
            int newPosition = realIndexesOfSearchResults.get(position);

            try {
                if (documents.getJSONObject(position).getString(DOCUMENT_LINK).contains(("back_end"))) {
                    if (documents.getJSONObject(newPosition).getBoolean(DOCUMENT_DOWNLOADED))
                        openFile(localDirectory, documents.getJSONObject(newPosition).getString(DOCUMENT_TITLE));

                    else {
                        if (downloadTask == null) {
                            downloadTask = new DownloadFileTask(documents.getJSONObject(newPosition).getString(DOCUMENT_LINK),
                                    documents.getJSONObject(newPosition).getString(DOCUMENT_LINK));
                            downloadTask.execute();
                        }
                    }
                }
                else if (documents.getJSONObject(position).getString(DOCUMENT_LINK).contains(("drive"))) {
                    // Package selected document content and send to ViewDocumentFragment
                    args.putString(DOCUMENT_TITLE, documents.getJSONObject(newPosition).getString(DOCUMENT_TITLE));
                    args.putString(DOCUMENT_VIEW, documents.getJSONObject(newPosition).getString(DOCUMENT_VIEW));
                    args.putString(DOCUMENT_TOPIC, documents.getJSONObject(newPosition).getString(DOCUMENT_TOPIC));
                    isViewable = true;
                }
                else {
                    Toast.makeText(getContext(), "Document link error", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            args.putInt(DOCUMENT_REQUEST_CODE, newPosition);
        }

        // If search is not active -> use normal position for ViewDocumentFragment
        else {
            try {
                if (documents.getJSONObject(position).getString(DOCUMENT_LINK).contains(("back_end"))) {
                    if (documents.getJSONObject(position).getBoolean(DOCUMENT_DOWNLOADED))
                        openFile(localDirectory, documents.getJSONObject(position).getString(DOCUMENT_TITLE));

                    else {
                        if (downloadTask == null) {
                            downloadTask = new DownloadFileTask(documents.getJSONObject(position).getString(DOCUMENT_TITLE),
                                    documents.getJSONObject(position).getString(DOCUMENT_LINK));
                            downloadTask.execute();
                        }
                    }
                }
                else if (documents.getJSONObject(position).getString(DOCUMENT_LINK).contains(("drive"))) {
                    // Package selected document content and send to ViewDocumentFragment
                    args.putString(DOCUMENT_TITLE, documents.getJSONObject(position).getString(DOCUMENT_TITLE));
                    args.putString(DOCUMENT_VIEW, documents.getJSONObject(position).getString(DOCUMENT_VIEW));
                    args.putString(DOCUMENT_TOPIC, documents.getJSONObject(position).getString(DOCUMENT_TOPIC));
                    isViewable = true;
                }
                else {
                    Toast.makeText(getContext(), "Document link error", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            args.putInt(DOCUMENT_REQUEST_CODE, position);
        }

        if (isViewable) {
            nextFragment.setArguments(args);

            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.layout_container, nextFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
        }
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
                            filteredDocuments = filterDocument();
                            adapter = new DocumentAdapter(getContext(), filteredDocuments);
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
                            if (filteredDocuments.length() == 0)
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
            for (int i = 0; i < filteredDocuments.length(); i++) {
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
                                document.getString(DOCUMENT_TOPIC).toLowerCase().contains(s)) {

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
            for (int i = 0; i < filteredDocuments.length(); i++)
                realIndexesOfSearchResults.add(i);

            adapter = new DocumentAdapter(getContext(), filteredDocuments);
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
        adapter = new DocumentAdapter(getContext(), filteredDocuments);
        listView.setAdapter(adapter);
        listView.setLongClickable(true);
        newDocumentButtonVisibility(true);
    }

    protected void openFile(String fileDirectory, String fileName) {
        File file = new File(fileDirectory, fileName);
        MimeTypeMap map = MimeTypeMap.getSingleton();
        String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
        String type = map.getMimeTypeFromExtension(ext);

        if (type == null)
            type = "*/*";

        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data = Uri.fromFile(file);

        intent.setDataAndType(data, type);

        startActivity(intent);
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
            newFavourite = filteredDocuments.getJSONObject(position);

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
                        if (i != realIndexesOfFilterResults.get(position)) {
                            try {
                                newArray.put(documents.get(i));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Equal main documents array with new sorted array and reset adapter
                    documents = newArray;
                    adapter = new DocumentAdapter(context, filteredDocuments = filterDocument());
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
                        documents.put(realIndexesOfFilterResults.get(position), newFavourite);
                        filteredDocuments.put(position, newFavourite);

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
                    documents.put(realIndexesOfFilterResults.get(position), newFavourite);
                    filteredDocuments.put(position, newFavourite);

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
     * Callback method when ViewDocumentFragment finished adding new document or editing existing document
     * @param requestCode requestCode for intent sent, in our case either NEW_DOCUMENT_REQUEST or position
     * @param resultCode resultCode from activity, either RESULT_OK or RESULT_CANCELED
     * @param resultData Data bundle passed back from ViewDocumentFragment
     */
    @Override
    public void onReceiveResult(int requestCode, int resultCode, Bundle resultData) {
        if (resultCode == Activity.RESULT_OK) {
            // If search was active -> call 'searchEnded' method
            if (searchActive && searchMenu != null)
                searchMenu.collapseActionView();

            if (resultData != null) {
                Log.i("[DF]", "do something");
                linkTask = new LinkCloudTask(requestCode, resultCode, resultData);
                linkTask.execute((Void) null);
            }
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
        private int resultCode;
        private Bundle resultData;

        private Boolean mLinkSuccess;
        private String mLinkData;

        private final String CONTENT_OBJECT = "obj_doc_list";
        private final String CONTENT_TITLE = "remark_name";
        private final String CONTENT_LINK = "download";
        private final String CONTENT_VIEW = "open_doc";

        LinkCloudTask(int request, int result, Bundle data) {
            requestCode = request;
            resultCode = result;
            resultData = data;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Cloud documents
            if (requestCode == CLOUD_UPDATE) {
                JSONObject request = null;
                try {
                    request = new JSONObject(resultData.getString(CLOUD_UPDATE_CODE));

                    JSONObject info = request.getJSONObject("link");
                    Log.i("[DF]", "info:" + info.toString());

                    JSONObject object = null;
                    if (info.has(CONTENT_OBJECT)) {
                        object = info.getJSONObject(CONTENT_OBJECT);

                        JSONArray title = null;
                        JSONArray link = null;
                        JSONArray view = null;
                        if (object.has(CONTENT_TITLE))
                            title = object.getJSONArray(CONTENT_TITLE);
                        else
                            Log.i("[DF]", "Fail to fetch field " + CONTENT_TITLE);

                        if (object.has(CONTENT_LINK))
                            link = object.getJSONArray(CONTENT_LINK);
                        else
                            Log.i("[DF]", "Fail to fetch field " + CONTENT_LINK);

                        if (object.has(CONTENT_VIEW))
                            view = object.getJSONArray(CONTENT_VIEW);
                        else
                            Log.i("[DF]", "Fail to fetch field " + CONTENT_VIEW);

                        if (title != null && link != null && view != null) {
                            if (title.length() == link.length()) {
                                // Update documents, check document id is either existed or not
                                // Yes -> update data, no -> add new document
                                for(int i = 0; i < title.length(); i++) {
                                    int position = -1;
                                    for(int j = 0; j < documents.length(); j++) {
                                        if(documents.getJSONObject(j).has(DOCUMENT_TITLE)
                                                && title.getString(i).equals(documents.getJSONObject(j).getString(DOCUMENT_TITLE))) {
                                            position = j;
                                            break;
                                        }
                                    }

                                    JSONObject document = null;
                                    // Add new document
                                    if (position < 0) {
                                        document = new JSONObject();

                                        // Add new document
                                        document.put(DOCUMENT_TITLE, title.getString(i));
                                        document.put(DOCUMENT_LINK, link.getString(i));
                                        document.put(DOCUMENT_VIEW, view.getString(i));
                                        document.put(DOCUMENT_DOWNLOADED, false);
                                        document.put(DOCUMENT_TOPIC, "0");
                                        document.put(DOCUMENT_FAVOURED, false);

                                        documents.put(document);
                                        filteredDocuments.put(document);
                                    }
                                    // Update existed document
                                    else {
                                        document = documents.getJSONObject(position);

                                        document.put(DOCUMENT_LINK, link.getString(i));

                                        documents.put(position, document);

                                        for (int j = 0; j < filteredDocuments.length(); j++) {
                                            if (position == realIndexesOfFilterResults.get(j)) {
                                                filteredDocuments.put(realIndexesOfFilterResults.get(j), document);
                                                break;
                                            }
                                        }
                                    }
                                }

                                Thread.sleep(2000);
                                return true;
                            }
                            else
                                Log.i("[DF]", "Field length aren't the same in array");
                        }
                        else
                            Log.i("[DF]", "Loading object content error");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            linkTask = null;

            if(success) {
                if (requestCode == CLOUD_UPDATE) {
                    // Update document list view
                    adapter.notifyDataSetChanged();

                    Boolean saveSuccessful = saveData(localPath, documents);

                    if (saveSuccessful) {
                        Toast toast = Toast.makeText(getContext(),
                                getResources().getString(R.string.toast_new_document),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    // If no documents -> show 'Press + to add new document text, invisible otherwise
                    if(filteredDocuments.length() == 0)
                        noDocuments.setVisibility(View.VISIBLE);
                    else
                        noDocuments.setVisibility(View.INVISIBLE);
                }
            }
        }

        @Override
        protected void onCancelled() {
            linkTask = null;
        }
    }

    public class DownloadFileTask extends AsyncTask<Void, Void, Boolean> {
        
        String mFileName = "";
        String mUrl = "";
        String mFileDirectory = "";

        PowerManager.WakeLock mWakeLock;

        DownloadFileTask(String fileName, String url) {
            mFileName = fileName;
            mUrl = LinkCloud.BASIC_WEB_LINK + url;
            mFileDirectory = localDirectory;
        }
        
        @Override
        protected Boolean doInBackground(Void... params) {

            FileOutputStream output = null;
            InputStream input = null;
            HttpURLConnection connection = null;

            try {
                byte[] buff;

                long totalSize = 0;
                File toDirectory = new File(mFileDirectory);

                if (!toDirectory.exists()) {
                    Boolean created = toDirectory.mkdirs();

                    // If file failed to create -> return false
                    if (!created)
                        return false;
                }

                File toFile = new File(toDirectory, mFileName);

                if (!toFile.exists()) {
                    Boolean created = toFile.createNewFile();

                    // If file failed to create -> return false
                    if (!created)
                        return false;
                }
                Log.i("[DF]", "File " + toFile.toString());

                connection = (HttpURLConnection) (new URL(mUrl)).openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.i("[DF]", "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                    return false;
                }

                if (connection.getContentLength() > 0) {
                    input = connection.getInputStream();
                    output = new FileOutputStream(toFile);
                    totalSize = connection.getContentLength();
                    Log.i("[DF]", "Size " + totalSize);

                    long total = 0;
                    buff = new byte[4096];
                    int bufferLength = 0;
                    while ((bufferLength = input.read(buff)) > 0) {
                        // allow canceling with back button
                        if (isCancelled()) {
                            input.close();
                            return false;
                        }
                        // Publishing the progress....
                        total += bufferLength;
                        publishProgress((int) ((total * 100) / totalSize));
                        output.write(buff, 0, bufferLength);
                    }

                    // Modify current download state, if true -> downloaded, false otherwise
                    for (int i = 0; i < documents.length(); i++) {
                        if (documents.getJSONObject(i).getString(DOCUMENT_TITLE).equals(mFileName)) {
                            JSONObject document = documents.getJSONObject(i);

                            document.put(DOCUMENT_DOWNLOADED, true);

                            documents.put(i, document);
                            return true;
                        }
                    }
                } else
                    Log.w(mFileName, "File not find");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("[DF]", e.toString());
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return false;
        }

        private void publishProgress(Integer... progress) {
            // if we get here, length is known, now set indeterminate to false
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            progressDialog.show();
        }

        protected void onPostExecute(Boolean success) {
            downloadTask = null;
            mWakeLock.release();
            progressDialog.dismiss();
            
            if (success) {
                Log.i("[DF]", "Downloaded " + mFileName);
                progressDialog.dismiss();

                adapter.notifyDataSetChanged();

                Boolean saveSuccessful = saveData(localPath, documents);

                if (saveSuccessful) {
                    Toast toast = Toast.makeText(getContext(),
                            getResources().getString(R.string.toast_new_document),
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
                openFile(mFileDirectory, mFileName);
            }
            else
                Log.d("[DF]", "Fail to download " + mFileName);
        }

        @Override
        protected void onCancelled() {
            downloadTask = null;
        }
    }
}
