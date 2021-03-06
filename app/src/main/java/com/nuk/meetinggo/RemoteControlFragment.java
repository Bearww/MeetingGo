package com.nuk.meetinggo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import static com.nuk.meetinggo.RemoteActivity.PICK_IMAGE_REQUEST;

public class RemoteControlFragment extends Fragment implements View.OnTouchListener, View.OnKeyListener,
        Toolbar.OnMenuItemClickListener {

    Context context;
    View view;
    Toolbar toolbar;
    MenuItem linkMenu;
    RelativeLayout buttonLayout;
    RelativeLayout selectLayout;
    Button leftButton;
    Button rightButton;
    //Button connectButton;
    Button keyboardButton;
    Button selectButton;
    ImageView mousePad;
    View progressView;
    TextView noConnectionText;
    MenuItem shareMenu;

    private static int currentMode;
    public final static int NONE = -1;
    public final static int MODE_CONTROL = 1;
    public final static int MODE_SHARE = 2;
    public final static int MODE_IMAGE = 3;
    
    private Handler messageHandler;
    private ConnectServerTask connectServerTask;

    private static boolean isConnected = false;
    private Thread listener;
    private Thread provider;
    private boolean displayKeyboard = false;
    private Socket socket;
    private static PrintWriter out;
    private static InputStream in;

    private int mouseSensitivity = 3;
    private float screenRatio = 1.0f;

    private float initX = 0;
    private float initY = 0;

    private static float buttonLayoutBaseYCoordinate; // Base Y coordinate of button layout
    private static float selectLayoutBaseYCoordinate; // Base Y coordinate of select layout

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Save the context to show Toast messages
        context = getContext();
        
        currentMode = NONE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_remote, container, false);

        // Get reference of button layout
        buttonLayout = (RelativeLayout) view.findViewById(R.id.buttonLayout);
        selectLayout = (RelativeLayout) view.findViewById(R.id.selectLayout);

        buttonLayoutBaseYCoordinate = buttonLayout.getY();
        buttonLayoutVisibility(false);

        selectLayoutBaseYCoordinate = selectLayout.getY();
        selectLayoutVisibility(false);

        // Get references of all buttons
        leftButton = (Button) view.findViewById(R.id.leftButton);
        rightButton = (Button) view.findViewById(R.id.rightButton);
        //connectButton = (Button) view.findViewById(R.id.connectButton);
        keyboardButton = (Button) view.findViewById(R.id.keyboardButton);
        selectButton = (Button) view.findViewById(R.id.selectButton);

        // Get reference of progress bar
        progressView = view.findViewById(R.id.connectProgress);

        // Get reference of textview
        noConnectionText = (TextView) view.findViewById(R.id.noConnection);
        noConnectionText.setVisibility(View.VISIBLE);

        // Init layout components
        toolbar = (Toolbar) view.findViewById(R.id.toolbarMain);
        if (toolbar != null)
            initToolbar();

        // This activity extends View.OnTouchListener, set this as onTouchListener for all buttons
        leftButton.setOnTouchListener(this);
        rightButton.setOnTouchListener(this);
        keyboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyClickHandler(v);
            }
        });
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < 19) {
                    Intent intent = new Intent();
                    // Show only images, no videos or anything else
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    // Always show the chooser (if there are multiple options available)
                    getActivity().startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
                }
                else {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    getActivity().startActivityForResult(intent, PICK_IMAGE_REQUEST);
                }
            }
        });

        // Set the width of the buttons to half the screen size
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        //leftButton.setWidth(width / 2);
        //rightButton.setWidth(width / 2);

        ImageListener.DeviceWidth = width;
        ImageListener.DeviceHeight = size.y - leftButton.getHeight();

        // Get reference to the EditText acting as editText
        EditText editText = (EditText) view.findViewById(R.id.editText);
        editText.setOnKeyListener(this);
        editText.addTextChangedListener(new TextWatcher() {
            public void  afterTextChanged (Editable s) {
                sendMessage(Constants.KEYBOARD + s.toString());
                s.clear();
            }

            public void  beforeTextChanged  (CharSequence s, int start, int count, int after) {
            }

            public void  onTextChanged  (CharSequence s, int start, int before, int count) {
            }
        });

        // Get reference to the ImageView acting as mousepad
        mousePad = (ImageView) view.findViewById(R.id.mousePad);

        // Capture finger taps and movement on the view
        mousePad.setOnTouchListener(this);

        messageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch(msg.what) {
                    case Constants.DO_UI_TEXT:
                        if (currentMode == msg.getData().getInt("Mode"))
                            testMsg("" + msg.getData().getInt("Text"));
                        break;
                    case Constants.DO_UI_IMAGE:
                        if (currentMode == msg.getData().getInt("Mode"))
                            setImage(msg.getData().getByteArray("Image"));
                        break;
                }
            }
        };

        return view;
    }

    /**
     * Method to show and hide the button layout
     * @param isVisible true to show tab, false to hide
     */
    public void buttonLayoutVisibility(boolean isVisible) {
        if (isVisible) {
            buttonLayout.animate().cancel();
            buttonLayout.animate().translationY(buttonLayoutBaseYCoordinate);
        } else {
            buttonLayout.animate().cancel();
            buttonLayout.animate().translationY(buttonLayoutBaseYCoordinate + 500);
        }
    }

    /**
     * Method to show and hide the button layout
     * @param isVisible true to show tab, false to hide
     */
    public void selectLayoutVisibility(boolean isVisible) {
        if (isVisible) {
            selectLayout.animate().cancel();
            selectLayout.animate().translationY(selectLayoutBaseYCoordinate);
        } else {
            selectLayout.animate().cancel();
            selectLayout.animate().translationY(selectLayoutBaseYCoordinate + 500);
        }
    }

    private void setImageRequestSizes() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getMetrics(metrics);
        int width, height;
        width = metrics.widthPixels;
        height = metrics.heightPixels;

        ImageListener.DeviceWidth = (int)(screenRatio * width);
        ImageListener.DeviceHeight = (int)(screenRatio * height);
        Log.e("REQUESTINGSIZE", screenRatio + " " + ImageListener.DeviceWidth + " " + ImageListener.DeviceHeight);
    }

    public static void sendMessage(String message) {
        if (isConnected && out != null) {
            // Send message to server
            //Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            out.println(message);
        }
    }

    private void sendMessage(char c) {
        sendMessage("" + c);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        setImageRequestSizes();
        super.onConfigurationChanged(newConfig);
    }

    // TODO setting tool bar, reference MainFragment initToolbar
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getActivity().getMenuInflater().inflate(R.menu.menu_remote, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == leftButton) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:   sendMessage(Constants.LEFTMOUSEDOWN);    break;
                case MotionEvent.ACTION_UP:       sendMessage(Constants.LEFTMOUSEUP);      break;
            }
        } else if(v == rightButton) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:   sendMessage(Constants.RIGHTMOUSEDOWN);    break;
                case MotionEvent.ACTION_UP:       sendMessage(Constants.RIGHTMOUSEUP);      break;
            }
        }
        else
            mousePadHandler(event);

        return true;
    }

    // Send a mouse message
    private void mousePadHandler(MotionEvent event) {
        int action = event.getAction();
        int touchCount = event.getPointerCount();

        // If a single touch
        if(touchCount == 1) {
            switch(action) {
                case 0:	// Touch down
                    initX = event.getX();
                    initY = event.getY();
                    break;

                case 1:	// Touch up
                    long deltaTime = event.getEventTime() - event.getDownTime();
                    if(deltaTime < 250)
                        sendMessage(Constants.LEFTCLICK);
                    break;

                case 2: // Moved
                    float deltaX = (initX - event.getX()) * -1;
                    float deltaY = (initY - event.getY()) * -1;

                    sendMessage(Constants.createMoveMouseMessage(deltaX * mouseSensitivity
                            , deltaY * mouseSensitivity));

                    initX = event.getX();
                    initY = event.getY();
                    break;

                default: break;
            }
        }

        // If two touches send scroll message
        // based off MAC osx multi touch scrolls up and down
        else if(touchCount == 2) {
            if(action == 2) {
                float deltaY = event.getY() - initY;
                float tolerance = 10;

                if (deltaY > tolerance) {
                    sendMessage(Constants.SCROLLUP);
                    initY = event.getY();
                }
                else if(deltaY < -1 * tolerance) {
                    sendMessage(Constants.SCROLLDOWN);
                    initY = event.getY();
                }
            }
            else
                initY = event.getY();
        }
    }

    // Detect keyboard event, and send message
    @Override
    public boolean onKey(View v, int c, KeyEvent event) {
        // c is the event keycode
        if(event.getAction() == 1) {
            sendMessage("" + Constants.KEYCODE + c);
        }
        // This will prevent the focus from moving off the text field
        return c == KeyEvent.KEYCODE_DPAD_UP ||
                c == KeyEvent.KEYCODE_DPAD_DOWN ||
                c == KeyEvent.KEYCODE_DPAD_LEFT ||
                c == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    // Show and hide Keyboard by setting the
    // focus on a hidden text field
    public void keyClickHandler(View v) {
        EditText editText = (EditText) view.findViewById(R.id.editText);
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if(displayKeyboard) {
            mgr.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            displayKeyboard = false;
        }
        else {
            mgr.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            displayKeyboard = true;
        }
    }

    public void testMsg(final String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    public void setImage(final byte[] image) {
        if (currentMode == MODE_CONTROL || currentMode == MODE_SHARE) {
            Bitmap map = BitmapFactory.decodeByteArray(image, 0, image.length);

            mousePad.setImageBitmap(map);
            mousePad.postInvalidate();
        }
    }

    public void setImage(final Bitmap image) {
        Log.i("[RCF]", "Set image");
        currentMode = MODE_IMAGE;
        mousePad.setImageBitmap(image);
        mousePad.postInvalidate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(isConnected && out != null) {
            try {
                out.println("exit"); // Tell server to exit
                if (socket != null) socket.close(); // Close socket
            } catch (IOException e) {
                Log.e("[RCF]", "Error in closing socket", e);
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        if (currentMode == NONE) {
            if (!isConnected && connectServerTask == null) {
                // Show a progress spinner, and try to connect to server in another thread.
                showProgress(true);
                connectServerTask = new ConnectServerTask();
                connectServerTask.execute(LinkCloud.SERVER_IP);
            }
            else return false;
        }

        if (id == R.id.action_receiver) {
            // If listener is null -> start image listener, or reconnect image listener
            if(listener == null || !ImageListener.isConnected) {
                Log.i("[RCF]", "Start receiver");
                ImageProvider.isConnected = false;
                provider = null;

                currentMode = MODE_CONTROL;
                listener = new Thread(new ImageListener(in, Constants.FRAMES_PER_SECOND, messageHandler));
                listener.start();

                buttonLayoutVisibility(true);
                selectLayoutVisibility(false);

                return true;
            }
        }
        if (id == R.id.action_transmitter) {
            if (provider == null || !ImageProvider.isConnected) {
                Log.i("[RCF]", "Start provider");
                ImageListener.isConnected = false;
                listener = null;
                
                currentMode = MODE_SHARE;
                provider = new Thread(new ImageProvider(socket, getActivity(), messageHandler));
                provider.start();

                buttonLayoutVisibility(false);
                selectLayoutVisibility(true);

                return true;
            }
        }

        return false;
    }

    /**
     * Initialize toolbar with required components such as
     * - title, menu/OnMenuItemClickListener and searchView -
     */
    protected void initToolbar() {
        // TODO change to current note title
        toolbar.setTitle(R.string.app_name);

        // Inflate menu_main to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.menu_remote);

        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(this);

        Menu menu = toolbar.getMenu();

        if (menu != null) {
            // Get 'Share' menu item
            shareMenu = menu.findItem(R.id.action_transmitter);
/*
            if (MeetingInfo.getControllable(MemberInfo.memberName))
                shareMenu.setVisible(true);
            else
                shareMenu.setVisible(false);
*/        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
/*
            connectButton.setVisibility(show ? View.GONE : View.VISIBLE);
            connectButton.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    connectButton.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });
*/
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            //connectButton.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public class ConnectServerTask extends AsyncTask<String,Void,Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = true;
            try {
                InetAddress serverAddr = InetAddress.getByName(params[0]);
                Log.d("[RCF]", serverAddr.toString());
                socket = new Socket(serverAddr, Constants.SERVER_PORT); // Open socket on server IP and port
            } catch (IOException e) {
                Log.e("[RCF]", "Error while connecting", e);
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result)
        {
            connectServerTask = null;
            isConnected = result;
            showProgress(false);
            Toast.makeText(context, isConnected ? "Connected to server!" : "Error while connecting", Toast.LENGTH_LONG).show();
            try {
                if(isConnected) {
                    noConnectionText.setVisibility(View.INVISIBLE);
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket
                            .getOutputStream())), true); //create output stream to send data to server
                    in = socket.getInputStream();
                }
                else
                    noConnectionText.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                Log.e("[RCF]", "Error while creating OutWriter", e);
                Toast.makeText(context, "Error while connecting", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
            connectServerTask = null;
        }
    }
}
