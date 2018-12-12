package group7a.iot.voiceit;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.Timer;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class MainActivity extends AppCompatActivity {
    TextView txv_temp_indoor = null;
    TextView txv_temp_outdoor = null;
    TextView txv_lighting_status = null;
    TextView txv_heating_status = null;
    Switch btnToggle = null;
    Switch btnToggle2 = null;
    Timer timer = new Timer(true);
    boolean newData = false;
    private String[] lines = new String[1000];
    private volatile String innerTemp = "0";
    private volatile String outerTemp = "";
    private TextToSpeech textToSpeech;

    private static final int SPEECH_REQUEST_CODE = 0;


    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en_US");
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, true);
// Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
// This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            handleVoiceCommand(spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleVoiceCommand(String spokenText) {
        switch (spokenText) {
            case "hello":
                speak("Hello");
                break;
            case "turn on lamp one":
                startAsyncTask("tdtool --on 1");
                break;
            case "turn on lamp two":
                startAsyncTask("tdtool --on 2");
                break;
            case "turn off lamp one":
                startAsyncTask("tdtool --off 1");
                break;
            case "turn off lamp two":
                startAsyncTask("tdtool --off 2");
                break;
            case "what's the temp":
            case "what is the temp":
                startAsyncTask("tdtool --list-sensors");
                break;
      //      case "what's the outside temp":
      //      case "what is the outside temp":
      //
      //          break;
            default:
                speak("Sorry, I don't get it!");
        }
    }

    public String run(String command) {
        //ÄNDRA IP EFTER VARJE UPPKOPPLING
        String hostname = "192.168.0.29";
        String username = "pi";
        String password = "voiceit";
        String returnString = "";
        try {
            Connection conn = new Connection(hostname);//init connection
            conn.connect();//start connection to the hostname
            boolean isAuthenticated = conn.authenticateWithPassword(username, password);
            if (!isAuthenticated)
                throw new IOException("Authentication failed.");
            Session sess = conn.openSession();
            sess.execCommand(command);
            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

            switch (command) {
                case "tdtool --list-sensors":
                    StringBuilder strB = new StringBuilder();
                    while (true) {
                        String line = br.readLine();
                        if (line == null) {
                            break;
                        }
                        strB.append(line);
                        strB.append("\n");
                        System.out.println(line);
                    }
                    String hej = strB.toString();
                    lines = hej.split("\\n");
                    lines = lines[0].split("\\t");
                    lines = lines[4].split("=");
                    innerTemp = lines[1]; //bör hanteras med returvärde istället...Hela metoden behöver ses över.
                    outerTemp = lines[2];
                break;
                /*
                case "tdtool --on 1":
                    System.out.println(br.readLine());
                    returnString = br.readLine();
                break;
                case "tdtool --off 1":
                    System.out.println(br.readLine());
                    returnString = br.readLine();
                break;
                */
            }
            /* Show exit status, if available (otherwise "null") */
            System.out.println("ExitCode: " + sess.getExitStatus());
            sess.close(); // Close this session
            conn.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
        return returnString;
    }
    public void startAsyncTask(final String command){
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                run(command);
                //your code to fetch results via SSH
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                switch (command){
                    case "tdtool --on 1":
                        speak("Lamp has been turned on");
                        break;
                    case "tdtool --off 1":
                        speak("Lamp has been turned off");
                        break;
                    case "tdtool --list-sensors":
                        speak("The temperature is" + innerTemp);
                        speak("The temperature is" + outerTemp);
                }
                txv_temp_indoor.setText(innerTemp);
                txv_temp_outdoor.setText(outerTemp);
            }
        }.execute(1);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        txv_temp_indoor = (TextView) findViewById(R.id.indoorTempShow);
        txv_temp_indoor.setText("24.5");
        txv_temp_outdoor = (TextView) findViewById(R.id.outdoorTempShow);
        txv_temp_outdoor.setText("14.5");
        txv_lighting_status = (TextView) findViewById(R.id.outdoorLightShow);
        txv_heating_status = (TextView) findViewById(R.id.outdoorLightShow2);
        btnToggle = (Switch) findViewById(R.id.btnToggle);
        btnToggle2 = (Switch) findViewById(R.id.btnToggle2);
        btnToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // below you write code to change switch status and action to take
                if (isChecked) { //do something if checked
                    txv_lighting_status.setText("On");
                    startAsyncTask("tdtool --on 1");
                } else {
                    txv_lighting_status.setText("Off");
                    startAsyncTask("tdtool --on 1");
                }
            }
        });
        btnToggle2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    txv_heating_status.setText("On");
                    displaySpeechRecognizer();

                } else {
                    txv_heating_status.setText("Off");
                    displaySpeechRecognizer();
                }
            }
        });
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int ttsLang = textToSpeech.setLanguage(Locale.US);

                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                            || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The Language is not supported!");
                    } else {
                        Log.i("TTS", "Language Supported.");
                    }
                    Log.i("TTS", "Initialization success.");
                } else {
                    Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void speak(String whatToSpeak) {
        int speechStatus = textToSpeech.speak(whatToSpeak, TextToSpeech.QUEUE_FLUSH, null);
        if (speechStatus == TextToSpeech.ERROR) {
            Log.e("TTS", "Error in converting Text to Speech!");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}