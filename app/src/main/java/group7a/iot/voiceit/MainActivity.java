package group7a.iot.voiceit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class MainActivity extends AppCompatActivity {
    private ImageButton mainButton = null;
    private volatile String innerTemp = "0";
    private volatile String outerTemp = "";
    private TextToSpeech textToSpeech;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private SpeakerRecognition mSpeakerRecognition = new SpeakerRecognition();
    private static final int SPEECH_REQUEST_CODE = 0;

    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private static final int PERMISSION_RECORD_AUDIO = 0;
    private MainActivity.RecordWaveTask recordTask = null;
    private File fileToSend;
    private static final String HOSTNAME = "10.200.11.154";
    private static final String USERNAME = "pi";
    private static final String PASSWORD = "voiceit";

    //Hashmap with all numbers in text-form
    private Map<String, Integer> textNumbers = new HashMap<>();
    private List<String> mapNumbers = Arrays.asList("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"
            , "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty", "twentyone"
            , "twentytwo", "twentythree", "twentyfour", "twentyfive", "twentysix", "twentyseven", "twentyeight", "twentynine", "thirty"
            , "thirtyone", "thirtytwo", "thirtythree", "thirtyfour", "thirtyfive", "thirtysix", "thirtyseven", "thirtyeight", "thirtynine"
            , "fourty", "fourtyone", "fourtytwo", "fourtythree", "fourtyfour", "fourtyfive", "fourtysix", "fourtyseven", "sourtyeight", "fourtynine"
            , "fifty", "fiftyone", "fiftytwo", "fiftythree", "fiftyfour", "fiftyfive", "fiftysix", "fiftyseven", "fiftyeight", "fiftynine");

    private void activateVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH );
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en_US");
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, true);
        //Start activity to activate voice recognition.
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    //The result from Speaker Recognizer is attained in this call back method.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            try {
                handleVoiceCommand(spokenText);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String handleInput(String spokenText) {
        String[] inputTextTemp = spokenText.split(" ");
        ArrayList<String> inputText = new ArrayList<>(Arrays.asList(inputTextTemp));
        StringBuilder command = new StringBuilder();
        for(int i = 0; i < inputText.size()-2; i++) {
            command.append(inputText.get(i));
            if(i < inputText.size()-2) {
                command.append(" ");
            }
        }
        return command + inputText.get(inputText.size()-1);
    }
    //Handle time for the scheduling function
    private int handleTimerTime(String spokenText) {
        String[] temporary = spokenText.split(" ");
        String k = temporary[temporary.length-2];
        int digit = 0;

        try {
            digit = Integer.parseInt(k);
            speak("The lamp will be turned " + temporary[1] + " in "
                    + digit + temporary[temporary.length-1]);
            if (temporary[temporary.length-1].equalsIgnoreCase("hours") ||
                    temporary[temporary.length-1].equalsIgnoreCase("hour")) {
                digit = digit * 3600000;

            } else if (temporary[temporary.length-1].equalsIgnoreCase("minutes") ||
                    temporary[temporary.length-1].equalsIgnoreCase("minute")) {
                digit = digit * 60000;
            } else if (temporary[temporary.length-1].equalsIgnoreCase("seconds") ||
                    temporary[temporary.length-1].equalsIgnoreCase("second")) {
                digit = digit * 1000;
            }
            return digit;
        } catch (NumberFormatException e) {
            if(textNumbers.containsKey(k)) {
                digit = textNumbers.get(k);
                Log.i("My_Tag", "value: " + digit);
                switch (temporary[temporary.length - 1]) {
                    case "hours":
                    case "hour":
                        digit = digit * 3600000;
                        break;
                    case "minutes":
                    case "minute":
                        digit = digit * 60000;
                        break;
                    case "seconds":
                    case "second":
                        digit = digit * 1000;
                        break;
                }
                return digit;
            } else {
                return digit;
            }
        }
    }

    private void handleVoiceCommand(String spokenText) throws InterruptedException {
        final Handler handler = new Handler();
        String command;
        String tempCommand;
        int time = 0;
        if(spokenText.contains(" ") && (spokenText.substring(0, spokenText.indexOf(' ')).equals("turn") && spokenText.contains("timer"))) {
            time = handleTimerTime(spokenText);
            tempCommand = handleInput(spokenText);
            command = tempCommand.substring(0, tempCommand.lastIndexOf(" "));
        } else {
            command = spokenText;
        }

        switch (command) {
            case "hello":
                speak("Hello");
                break;
            case "turn off lamps":
                startAsyncTask("tdtool --off 1");
                startAsyncTask("tdtool --off 2");
                break;
            case "turn on lamp one":
                startAsyncTask("tdtool --on 1");
                break;
            case "turn on lamp two":
                //Start second speak-method which initiates speaker recognition
                speak("Please repeat your PASSWORD phrase.", 1);
                break;
            case "turn off lamp one":
                startAsyncTask("tdtool --off 1");
                break;
            case "turn off lamp two":
                startAsyncTask("tdtool --off 2");
                break;
            case "what's the temp":
                startAsyncTask("tdtool --list-sensors");
                break;
            case "what's the inside temp":
            case "what is the inside temp":
                startAsyncTask("tdtool --list-sensors", "in");
                break;
            case "what's the outside temp":
            case "what is the outside temp":
                startAsyncTask("tdtool --list-sensors", "out");
                break;
            case "turn on lamp one timer":
            case "turn on lamp1 timer":
            case "turn on lamp 1 timer":
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startAsyncTask("tdtool --on 1");
                    }
                }, time);
                break;
            case "turn on lamp two timer":
            case "turn on lamp2 timer":
            case "turn on lamp 2 timer":
            case "turn on lamp to timer":
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startAsyncTask("tdtool --on 2");
                    }
                }, time);
                break;
            case "turn off lamp one timer":
            case "turn off lamp1 timer":
            case "turn off lamp 1 timer":
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startAsyncTask("tdtool --off 1");
                    }
                }, time);
                break;
            case "turn off lamp two timer":
            case "turn off lamp2 timer":
            case "turn off lamp 2 timer":
            case "turn off lamp to timer":
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startAsyncTask("tdtool --off 2");
                    }
                }, time);
                break;
            default:
                speak("Sorry, I don't get it!");
        }
    }

    public void run(String command) {
        try {
            Connection conn = new Connection(HOSTNAME);
            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPassword(USERNAME, PASSWORD);
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
                    String tmp = strB.toString();
                    String[] lines = tmp.split("\\n");
                    lines = lines[0].split("\\t");
                    lines = lines[4].split("=");

                    String[] lines2 = tmp.split("\\n");
                    lines2 = lines2[1].split("\\t");
                    lines2 = lines2[4].split("=");

                    innerTemp = lines[1];
                    outerTemp = lines2[1];

                    break;
            }
            System.out.println("ExitCode: " + sess.getExitStatus());
            sess.close();
            conn.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

    public void startAsyncTask(final String command) {
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                run(command);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                switch (command) {
                    case "tdtool --on 1":
                        speak("Lamp 1 has been turned on");
                        break;
                    case "tdtool --on 2":
                        speak("Lamp 2 has been turned on");
                        break;
                    case "tdtool --off 1":
                        speak("Lamp 1 has been turned off");
                        break;
                    case "tdtool --off 2":
                        speak("Lamp 2 has been turned off");
                        break;
                    case "tdtool --list-sensors":
                        speak("The inside temperature is " + innerTemp + " and the outside temperature is " + outerTemp);
                }
            }
        }.execute(1);
    }

    public void startAsyncTask(final String command, final String setting) {
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                run(command);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                switch (command) {
                    case "tdtool --list-sensors":
                        if (setting.equals("in")) {
                            speak("The temperature is" + innerTemp);
                        } else if (setting.equals("out")) {
                            speak("The temperature is" + outerTemp);
                        } else {
                            speak("The inside temperature is " + innerTemp + " and the outside temperature is " + outerTemp);
                        }
                }
                            }
        }.execute(1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //populate hashmap with digits
        int counter = 1;
        for(String e : mapNumbers) {
            textNumbers.put(e, counter);
            counter++;
        }

        mainButton = findViewById(R.id.micButton);

        mainButton.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                activateVoiceRecognition();
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
        // Restore the previous task or create a new one if necessary
        recordTask = (MainActivity.RecordWaveTask) getLastCustomNonConfigurationInstance();
        if (recordTask == null) {
            recordTask = new MainActivity.RecordWaveTask(this);
        } else {
            recordTask.setContext(this);
        }
    }

    private void speak(String whatToSpeak) {
        int speechStatus = textToSpeech.speak(whatToSpeak,
                TextToSpeech.QUEUE_FLUSH, null);
        if (speechStatus == TextToSpeech.ERROR) {
            Log.e("TTS", "Error in converting Text to Speech!");
        }
    }

    //Second speak-method for speaker recognition commands
    private void speak(String whatToSpeak, int i) throws InterruptedException {
        int speechStatus = textToSpeech.speak(whatToSpeak, TextToSpeech.QUEUE_FLUSH, null);
        Thread.sleep(2000);
        activateSoundRecording();

        //Wait for PASSWORD-phrase
        Thread.sleep(4000);

        //Stop recording
        if (!recordTask.isCancelled() && recordTask.getStatus() == AsyncTask.Status.RUNNING) {
            recordTask.cancel(false);
        } else {
            Toast.makeText(MainActivity.this, "Task not running.", Toast.LENGTH_SHORT).show();
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    activateSoundRecording();
                } else {
                    // Permission denied
                    Toast.makeText(this, "\uD83D\uDE41", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void activateSoundRecording() {
        switch (recordTask.getStatus()) {
            case RUNNING:
                Toast.makeText(this, "Task already running...", Toast.LENGTH_SHORT).show();
                return;
            case FINISHED:
                recordTask = new MainActivity.RecordWaveTask(this);
                break;
            case PENDING:
                if (recordTask.isCancelled()) {
                    recordTask = new MainActivity.RecordWaveTask(this);
                }
        }
        File wavFile = new File(getFilesDir(), "recording_" + System.currentTimeMillis() / 1000 + ".wav");
        //Toast.makeText(this, wavFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        recordTask.execute(wavFile);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        recordTask.setContext(null);
        return recordTask;
    }
    //The following class is written by Kevin Mark 2016, with only minor changes by us.
    private class RecordWaveTask extends AsyncTask<File, Void, Object[]> {

        // Sound file settings.
        private final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
        private final int SAMPLE_RATE = 16000; // Hz
        private final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        private final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;

        private final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);
        private Context ctx;

        private RecordWaveTask(Context ctx) {
            setContext(ctx);
        }

        private void setContext(Context ctx) {
            this.ctx = ctx;
        }

        /**
         * Opens up the given file, writes the header, and keeps filling it with raw PCM bytes from
         * AudioRecord until it reaches 4GB or is stopped by the user. It then goes back and updates
         * the WAV header to include the proper final chunk sizes.
         *
         * @param files Index 0 should be the file to write to
         * @return Either an Exception (error) or two longs, the filesize, elapsed time in ms (success)
         */
        @Override
        protected Object[] doInBackground(File... files) {
            AudioRecord audioRecord = null;
            long startTime = 0;
            long endTime = 0;

            audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);
            try (FileOutputStream wavOut = new FileOutputStream(files[0])) {
                // Open our two resources

                // Write out the wav file header
                writeWavHeader(wavOut, CHANNEL_MASK, SAMPLE_RATE, ENCODING);

                // Avoiding loop allocations
                byte[] buffer = new byte[BUFFER_SIZE];
                boolean run = true;
                int read;
                long total = 0;

                // Let's go
                startTime = SystemClock.elapsedRealtime();
                audioRecord.startRecording();
                while (run && !isCancelled()) {
                    read = audioRecord.read(buffer, 0, buffer.length);

                    // WAVs cannot be > 4 GB due to the use of 32 bit unsigned integers.
                    if (total + read > 4294967295L) {
                        // Write as many bytes as we can before hitting the max size
                        for (int i = 0; i < read && total <= 4294967295L; i++, total++) {
                            wavOut.write(buffer[i]);
                        }
                        run = false;
                    } else {
                        // Write out the entire read buffer
                        wavOut.write(buffer, 0, read);
                        total += read;
                    }
                }
            } catch (IOException ex) {
                return new Object[]{ex};
            } finally {
                if (audioRecord != null) {
                    try {
                        if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                            audioRecord.stop();
                            endTime = SystemClock.elapsedRealtime();
                        }
                    } catch (IllegalStateException ignored) {
                    }
                    if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                        audioRecord.release();
                    }
                }
            }

            try {
                // This is not put in the try/catch/finally above since it needs to run
                // after we close the FileOutputStream
                updateWavHeader(files[0]);
            } catch (IOException ex) {
                return new Object[]{ex};
            }

            return new Object[]{files[0].length(), endTime - startTime};
        }

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out         The stream to write the header to
         * @param channelMask An AudioFormat.CHANNEL_* mask
         * @param sampleRate  The sample rate in hertz
         * @param encoding    An AudioFormat.ENCODING_PCM_* value
         * @throws IOException
         */
        private void writeWavHeader(OutputStream out, int channelMask, int sampleRate, int encoding) throws IOException {
            short channels;
            switch (channelMask) {
                case AudioFormat.CHANNEL_IN_MONO:
                    channels = 1;
                    break;
                case AudioFormat.CHANNEL_IN_STEREO:
                    channels = 2;
                    break;
                default:
                    throw new IllegalArgumentException("Unacceptable channel mask");
            }

            short bitDepth;
            switch (encoding) {
                case AudioFormat.ENCODING_PCM_8BIT:
                    bitDepth = 8;
                    break;
                case AudioFormat.ENCODING_PCM_16BIT:
                    bitDepth = 16;
                    break;
                case AudioFormat.ENCODING_PCM_FLOAT:
                    bitDepth = 32;
                    break;
                default:
                    throw new IllegalArgumentException("Unacceptable encoding");
            }

            writeWavHeader(out, channels, sampleRate, bitDepth);
        }

        /**
         * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
         * Two size fields are left empty/null since we do not yet know the final stream size
         *
         * @param out        The stream to write the header to
         * @param channels   The number of channels
         * @param sampleRate The sample rate in hertz
         * @param bitDepth   The bit depth
         * @throws IOException
         */
        private void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
            // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
            byte[] littleBytes = ByteBuffer
                    .allocate(14)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putShort(channels)
                    .putInt(sampleRate)
                    .putInt(sampleRate * channels * (bitDepth / 8))
                    .putShort((short) (channels * (bitDepth / 8)))
                    .putShort(bitDepth)
                    .array();

            // Not necessarily the best, but it's very easy to visualize this way
            out.write(new byte[]{
                    // RIFF header
                    'R', 'I', 'F', 'F', // ChunkID
                    0, 0, 0, 0, // ChunkSize (must be updated later)
                    'W', 'A', 'V', 'E', // Format
                    // fmt subchunk
                    'f', 'm', 't', ' ', // Subchunk1ID
                    16, 0, 0, 0, // Subchunk1Size
                    1, 0, // AudioFormat
                    littleBytes[0], littleBytes[1], // NumChannels
                    littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                    littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                    littleBytes[10], littleBytes[11], // BlockAlign
                    littleBytes[12], littleBytes[13], // BitsPerSample
                    // data subchunk
                    'd', 'a', 't', 'a', // Subchunk2ID
                    0, 0, 0, 0, // Subchunk2Size (must be updated later)
            });
        }

        /**
         * Updates the given wav file's header to include the final chunk sizes
         *
         * @param wav The wav file to update
         * @throws IOException
         */
        private void updateWavHeader(File wav) throws IOException {
            byte[] sizes = ByteBuffer
                    .allocate(8)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    // There are probably a bunch of different/better ways to calculate
                    // these two given your circumstances. Cast should be safe since if the WAV is
                    // > 4 GB we've already made a terrible mistake.
                    .putInt((int) (wav.length() - 8)) // ChunkSize
                    .putInt((int) (wav.length() - 44)) // Subchunk2Size
                    .array();

            //noinspection CaughtExceptionImmediatelyRethrown
            try (RandomAccessFile accessWave = new RandomAccessFile(wav, "rw")) {
                // ChunkSize
                accessWave.seek(4);
                accessWave.write(sizes, 0, 4);

                // Subchunk2Size
                accessWave.seek(40);
                accessWave.write(sizes, 4, 4);
                fileToSend = wav;
                //Uncomment the following if you want feedback of your recording.
//                Uri uri = Uri.parse(fileToSend.getAbsolutePath());
//                MediaPlayer mediaPlayer = new MediaPlayer();
//                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//                mediaPlayer.setDataSource(getApplicationContext(), uri);
//                mediaPlayer.prepare();
//                mediaPlayer.start();
                System.out.println("Running speaker recognition module!");

            } catch (IOException ex) {
                // Rethrow but we still close accessWave in our finally
                throw ex;
            }
        }

        @Override
        protected void onCancelled(Object[] results) {
            // Handling cancellations and successful runs in the same way
            onPostExecute(results);
        }

        @Override
        protected void onPostExecute(Object[] results) {
            Throwable throwable = null;
            //Send recorded file to speaker recognition module. The returned boolean indicates
            //wheter the voice phrase was accepted or not.
            //mSpeakerRecognition.createEnrollment(fileToSend);
            boolean returnValue = mSpeakerRecognition.verifySpeaker(fileToSend);
            if (returnValue) {
                speak("Access granted");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startAsyncTask("tdtool --on 2");
            } else {
                speak("Access denied");
            }
            if (results[0] instanceof Throwable) {
                // Error
                throwable = (Throwable) results[0];
                Log.e(MainActivity.RecordWaveTask.class.getSimpleName(), throwable.getMessage(), throwable);
            }

            // If we're attached to an activity
            if (ctx != null) {
                if (throwable == null) {
                    // Display final recording stats
                    double size = (long) results[0] / 1000000.00;
                    long time = (long) results[1] / 1000;
                    Toast.makeText(ctx, String.format(Locale.getDefault(), "%.2f MB / %d seconds",
                            size, time), Toast.LENGTH_LONG).show();
                } else {
                    // Error
                    Toast.makeText(ctx, throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
