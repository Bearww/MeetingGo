package com.nuk.meetinggo;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AddDocument extends Activity {

    private long rowID; // ID of document being edited, if any

    // EditTexts for document information
    private EditText nameEditText;
    private EditText linkEditText;

    // Called when the Activity is first started
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_document); // Inflate the UI

        nameEditText = (EditText) findViewById(R.id.nameEditText);
        linkEditText = (EditText) findViewById(R.id.linkEditText);

        Bundle extras = getIntent().getExtras(); // Get Bundle of extras

        // If there are extras, use them to populate the EditTexts
        if(extras != null) {
            rowID = extras.getLong(Constants.ROW_ID);
            nameEditText.setText(extras.getString(Constants.DOC_NAME));
            linkEditText.setText(extras.getString(Constants.DOC_LINK));
        }

        // Set event listener for the Save Document Button
        Button saveDocumentBtn = (Button) findViewById(R.id.saveDocumentButton);
        saveDocumentBtn.setOnClickListener(saveDocument);
    }

    // Responds to event generated when user clicks the Done Button
    View.OnClickListener saveDocument = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(nameEditText.getText().length() != 0) {
                AsyncTask<Object, Object, Object> saveDocTask = new AsyncTask<Object, Object, Object>() {
                    @Override
                    protected Object doInBackground(Object... params) {
                        saveDocumentInfo(); // Save document to the database
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        finish();
                    }
                };

                // Save the document to the database using a separate thread
                saveDocTask.execute((Object[]) null);
            }
            else {
                // Create a new AlertDialog Builder
                AlertDialog.Builder builder = new AlertDialog.Builder(AddDocument.this);

                // Set dialog title & message, and provide Button to dismiss
                builder.setTitle(R.string.errorTitle);
                builder.setMessage(R.string.errorMessage);
                builder.setPositiveButton(R.string.errorButton, null);
                builder.show();
            }
        }
    };

    // Saves contact information to the database
    private void saveDocumentInfo() {
        // Get DatabaseConnector to interact with the SQLite database
        DatabaseConnector dbConnector = new DatabaseConnector(this);

        if(getIntent().getExtras() == null) {
            // Inser the document information into the database
            dbConnector.insertDocument(
                    nameEditText.getText().toString(),
                    linkEditText.getText().toString()
            );
        }
        else {
            dbConnector.updateDocument(
                    rowID,
                    nameEditText.getText().toString(),
                    linkEditText.getText().toString()
            );
        }
    }
}
