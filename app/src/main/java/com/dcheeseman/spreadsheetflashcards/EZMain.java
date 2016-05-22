package com.dcheeseman.spreadsheetflashcards;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.poi.xssf.usermodel.XSSFPictureData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

public class EZMain extends AppCompatActivity {

    private static List questions;
    private static List backupQuestions;
    private Map<String, List<QAInfo>> topics;
    private boolean isLoaded;
    //private static boolean onQuestion;
    private static Random rand;
    private static QAInfo currQuestion;
    private ProgressBar pb_progress;
    private TextView tv_progress;

    private final static String TAG = "CSV Flashcard DEBUG";

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    private static final int PICKFILE_REQUEST_CODE = 0;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    static PlaceholderFragment pfQuestion, pfAnswer;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            /*cardText.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);*/
        }
    };

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private void showFileDialog() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }

    private boolean failedFileCheck() {
        if (topics.keySet().size() == 0) {
            isLoaded = false;
            return true;
        }
        return false;
    }

    public void nextQuestion(){
        if (questions.size() > 0) {
            currQuestion = (QAInfo) questions.get(rand.nextInt(questions.size()));
            questions.remove(currQuestion);
        } else {
            questions.addAll(backupQuestions);
            nextQuestion();
        }

        pb_progress.setProgress(backupQuestions.size() - questions.size());
        tv_progress.setText(String.valueOf(backupQuestions.size()-questions.size())+"/"+String.valueOf(backupQuestions.size()));

        pfAnswer.showAnswer();
        pfQuestion.showQuestion();
    }

    private void scrubTopicLIst() {
        List<String> toRemove = new ArrayList<String>();
        for (String key : topics.keySet()) {
            if (topics.get(key).size() == 0)
                toRemove.add(key);
        }

        for (String key : toRemove)
            topics.remove(key);
    }

    private void loadTopic(String topic) {
        questions = new ArrayList();
        questions.addAll(topics.get(topic));
        backupQuestions = new ArrayList();
        backupQuestions.addAll(questions);
        if (questions.size() == 0) {
            backupQuestions = questions = null;
            Toast.makeText(this, R.string.no_question_error, Toast.LENGTH_LONG).show();
            topics.remove(topic);
            if (failedFileCheck())
                return;
            else
                chooseTopic();
        } else {
            hide();

            pb_progress.setMax(backupQuestions.size());
            pb_progress.setProgress(0);
            tv_progress.setText(String.valueOf(backupQuestions.size()-questions.size())+"/"+String.valueOf(backupQuestions.size()));

            mViewPager.setCurrentItem(0);
            nextQuestion();
        }
    }

    private void menuOption(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_string);
        int menuId;
        if(isLoaded){
            menuId = R.array.menu_options;
        }else{
            menuId = R.array.menu_options_unloaded;
        }
        builder.setItems(menuId, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(isLoaded) {
                    switch (item) {
                        case 0:
                            showFileDialog();
                            break;
                        case 1:
                            chooseTopic();
                            break;
                        case 2:
                            downloadTemplate();
                            break;
                    }
                }else{
                    switch (item) {
                        case 0:
                            showFileDialog();
                            break;
                        case 1:
                            downloadTemplate();
                            break;
                    }
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void chooseTopic() {
        SortedSet<String> sortedKeys = new TreeSet<String>();
        sortedKeys.addAll(topics.keySet());
        final String[] array = sortedKeys.toArray(new String[topics.keySet().size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_topic);
        builder.setItems(array, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                // Do something with the selection
                loadTopic(array[item]);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String fpath = data.getDataString();
            loadFile(fpath);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        pfAnswer.checkOrientation();
        pfQuestion.checkOrientation();
    }

    private boolean loadFile(String fpath) {
        Log.d(TAG, "Attempting to open file: " + fpath);
        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(Uri.parse(fpath));
            fpath = fpath.toLowerCase();
            if (fpath.endsWith(".csv")) {
                topics = SpreadsheetReader.getQAPairs(is, SpreadsheetReader.SpreadSheetFormat.CSV);
            } else if (fpath.endsWith(".xls")) {
                topics = SpreadsheetReader.getQAPairs(is, SpreadsheetReader.SpreadSheetFormat.XLS);
            } else if (fpath.endsWith(".xlsx")) {
                topics = SpreadsheetReader.getQAPairs(is, SpreadsheetReader.SpreadSheetFormat.XLSX);
                //} else if (fpath.endsWith(".ods")) {
                //    topics = SpreadsheetReader.getQAPairsODS(fpath);
            }else{
                Toast.makeText(this, R.string.invalid_file_error, Toast.LENGTH_LONG).show();
                return true;
            }
            is.close();

            scrubTopicLIst();
            if (topics.size() == 0) {
                isLoaded = false;
                Toast.makeText(this,R.string.no_topic_error, Toast.LENGTH_LONG).show();
                return true;
            }
            isLoaded = true;
            chooseTopic();
            return false;
        } catch (FileNotFoundException e) {
            Toast.makeText(EZMain.this, R.string.no_file_error, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            Toast.makeText(EZMain.this, R.string.corrupt_file_error, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        }
    }

    public void downloadTemplate() {
        InputStream in = getResources().openRawResource(R.raw.template);
        FileOutputStream fout = null;
        try {
            String downloadsDirectoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            String filename = "SpreadsheetFlashCardsTemplate.xlsx";
            fout = new FileOutputStream(new File(downloadsDirectoryPath + "/" + filename));
            final byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
            if (in != null) {
                in.close();
            }
            if (fout != null) {
                fout.close();
            }
            Toast.makeText(this, R.string.template_saved,Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            return;
        }
    }

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ezmain);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        tv_progress = (TextView) findViewById(R.id.tv_progress);
        pb_progress = (ProgressBar) findViewById(R.id.pb_progress);
        pb_progress.setMax(1);
        pb_progress.setProgress(1);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewPager.setCurrentItem(0);
                nextQuestion();
            }
        });

        //setContentView(R.layout.activity_main);
        isLoaded = false;
        rand = new Random();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            menuOption();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";


        LinearLayout ll_content_container;
        TextView cardText;
        ImageView iv_picture;
        final Matrix matrix = new Matrix();
        final Matrix savedMatrix = new Matrix();
        View rootView;

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_ezmain, container, false);

            // Remember some things for zooming
            final PointF start = new PointF();
            final PointF mid = new PointF();

            cardText = (TextView) rootView.findViewById(R.id.tv_text);
            iv_picture = (ImageView) rootView.findViewById(R.id.iv_picture);
            iv_picture.setOnTouchListener(new View.OnTouchListener() {
                int mode = NONE;
                float oldDist = 1f;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // TODO Auto-generated method stub

                    ImageView view = (ImageView) v;
                    dumpEvent(event);

                    // Handle touch events here...
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            savedMatrix.set(matrix);
                            start.set(event.getX(), event.getY());
                            Log.d(TAG, "mode=DRAG");
                            mode = DRAG;
                            break;
                        case MotionEvent.ACTION_POINTER_DOWN:
                            oldDist = spacing(event);
                            Log.d(TAG, "oldDist=" + oldDist);
                            if (oldDist > 10f) {
                                savedMatrix.set(matrix);
                                midPoint(mid, event);
                                mode = ZOOM;
                                Log.d(TAG, "mode=ZOOM");
                            }
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_POINTER_UP:
                            mode = NONE;
                            Log.d(TAG, "mode=NONE");
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (mode == DRAG) {
                                // ...
                                matrix.set(savedMatrix);
                                matrix.postTranslate(event.getX() - start.x, event.getY()
                                        - start.y);
                            } else if (mode == ZOOM) {
                                float newDist = spacing(event);
                                Log.d(TAG, "newDist=" + newDist);
                                if (newDist > 10f) {
                                    matrix.set(savedMatrix);
                                    float scale = newDist / oldDist;
                                    matrix.postScale(scale, scale, mid.x, mid.y);
                                }
                            }
                            break;
                    }

                    view.setImageMatrix(matrix);
                    return false;
                }

                private void dumpEvent(MotionEvent event) {
                    String names[] = {"DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
                            "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?"};
                    StringBuilder sb = new StringBuilder();
                    int action = event.getAction();
                    int actionCode = action & MotionEvent.ACTION_MASK;
                    sb.append("event ACTION_").append(names[actionCode]);
                    if (actionCode == MotionEvent.ACTION_POINTER_DOWN
                            || actionCode == MotionEvent.ACTION_POINTER_UP) {
                        sb.append("(pid ").append(
                                action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
                        sb.append(")");
                    }
                    sb.append("[");
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        sb.append("#").append(i);
                        sb.append("(pid ").append(event.getPointerId(i));
                        sb.append(")=").append((int) event.getX(i));
                        sb.append(",").append((int) event.getY(i));
                        if (i + 1 < event.getPointerCount())
                            sb.append(";");
                    }
                    sb.append("]");
                    Log.d(TAG, sb.toString());
                }

                /** Determine the space between the first two fingers */
                private float spacing(MotionEvent event) {
                    float x = event.getX(0) - event.getX(1);
                    float y = event.getY(0) - event.getY(1);
                    return (float) Math.sqrt((double) (x * x + y * y));
                }

                /** Calculate the mid point of the first two fingers */
                private void midPoint(PointF point, MotionEvent event) {
                    float x = event.getX(0) + event.getX(1);
                    float y = event.getY(0) + event.getY(1);
                    point.set(x / 2, y / 2);
                }
            });
            ll_content_container = (LinearLayout) rootView.findViewById(R.id.ll_content_container);

            checkOrientation();

            return rootView;
        }

        private void checkOrientation() {
            int orientation = getResources().getConfiguration().orientation;

            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ((LinearLayout) rootView.findViewById(R.id.ll_content_container)).setOrientation(LinearLayout.HORIZONTAL);
            } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                ((LinearLayout) rootView.findViewById(R.id.ll_content_container)).setOrientation(LinearLayout.VERTICAL);
            }
        }

        private void showQuestion(){
            cardText.setText(currQuestion.getQuestion());
            setImage(currQuestion.getQuestionPicture());
        }

        private void showAnswer(){
            cardText.setText(currQuestion.getAnswer());
            setImage(currQuestion.getAnswerPicture());
        }

        private void scaleImage(){
            if(iv_picture.getDrawable() != null) {
                float iw = iv_picture.getDrawable().getIntrinsicWidth();
                float ih = iv_picture.getDrawable().getIntrinsicHeight();

                Matrix m = iv_picture.getImageMatrix();
                RectF drawableRect = new RectF(0, 0, iw, ih);
                RectF viewRect = new RectF(0, 0, iv_picture.getWidth(), iv_picture.getHeight());
                m.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);

                matrix.set(m);
                savedMatrix.set(m);
                iv_picture.setImageMatrix(m);
                iv_picture.invalidate();
            }
        }

        private int getBitmapColor(Bitmap bitmap) {
            long redBucket = 0;
            long greenBucket = 0;
            long blueBucket = 0;
            long pixelCount = 0;

            for (int y = 0; y < bitmap.getHeight(); y++) {
                for (int x = 0; x < bitmap.getWidth(); x++) {
                    int p = bitmap.getPixel(x, y);

                    int R = (p >> 16) & 0xff;
                    int G = (p >> 8) & 0xff;
                    int B = p & 0xff;

                    pixelCount++;
                    redBucket += R;
                    greenBucket += G;
                    blueBucket += B;
                    // does alpha matter?
                }
            }

            int averageColor = Color.argb(255, (int) (redBucket / pixelCount),
                    (int) (greenBucket / pixelCount),
                    (int) (blueBucket / pixelCount));

            return averageColor;
        }

        private void setImage(final Bitmap b) {
            if (b != null) {
                final int color = getBitmapColor(b);
                iv_picture.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
                iv_picture.invalidate();

                final Handler h = new Handler();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while(iv_picture.getWidth() == 0 || iv_picture.getHeight() == 0) {
                                Thread.sleep(50);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        h.post(new Runnable() {
                            @Override
                            public void run() {
                                iv_picture.setImageDrawable(new BitmapDrawable(getResources(), b));
                                //iv_picture.setBackgroundColor(color);

                                scaleImage();
                            }
                        });
                    }
                }).start();

            } else {
                iv_picture.setImageResource(0);
                iv_picture.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            PlaceholderFragment pf;
            switch(position) {
                case 0:
                    pf = PlaceholderFragment.newInstance(position + 1);
                    pfQuestion = pf;
                    //pf.showQuestion();
                    break;
                default:
                    pf = PlaceholderFragment.newInstance(position + 1);
                    pfAnswer = pf;
                    //pf.showAnswer();
                    break;
            }
            return pf;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Question";
                case 1:
                    return "Answer";
            }
            return null;
        }
    }

    public void onResume() {
        super.onResume();
        hide();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_VIEW.equals(action) && type != null) {
            loadFile(intent.getData().toString());
        } else if (Intent.ACTION_OPEN_DOCUMENT.equals(action) && type != null) {
            loadFile(intent.getData().toString());
        } else {
            // Handle other intents, such as being started from the home screen
        }
    }

    private void hide() {
       // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void onStart(){
        super.onStart();
        if(!isLoaded) {
            tv_progress.setText("Press Phone's Menu Button to Load Flashcards");
            //menuOption();
        }
    }
}
