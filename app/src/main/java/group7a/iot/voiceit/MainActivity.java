package group7a.iot.voiceit;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private volatile String innerTemp = "";

    public void run(String command) {
        String hostname = "192.168.0.101";
        String username = "pi";
        String password = "voiceit";
        try {
            Connection conn = new Connection(hostname);//init connection
            conn.connect();//start connection to the hostname
            boolean isAuthenticated = conn.authenticateWithPassword(username, password);
            if (isAuthenticated == false)
                throw new IOException("Authentication failed.");
            Session sess = conn.openSession();
            sess.execCommand(command);
            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            switch (command) {
                case "tdtool --list-sensors":
                    //int i = 0;
                    StringBuilder strB = new StringBuilder();
                    while (true) {
                        String line = br.readLine();
                        // read line
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
                    innerTemp = lines[1];

                    break;
                case "tdtool --on 1":
                    System.out.println(br.readLine());
                    break;
                case "tdtool --off 1":
                    System.out.println(br.readLine());
                    break;
            }
//        	for (String s : lines){
//            	System.out.println(s);
//        	}
//        	for(String l : lines){
//            	if(l.matches(".*\\btemperature=[0-9]*.[0-9]+\\b.*"))
//                	System.out.println("found");
//
//        	}
            /* Show exit status, if available (otherwise "null") */
            System.out.println("ExitCode: " + sess.getExitStatus());
            sess.close(); // Close this session
            conn.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
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
                    new AsyncTask<Integer, Void, Void>() {
                        @Override
                        protected Void doInBackground(Integer... params) {
                            run("tdtool --on 1");
                            //your code to fetch results via SSH
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            txv_temp_indoor.setText(innerTemp);
                        }
                    }.execute(1);
                    //action
                } else {
                    txv_lighting_status.setText("Off");
                    new AsyncTask<Integer, Void, Void>() {
                        @Override
                        protected Void doInBackground(Integer... params) {
                            run("tdtool --off 1");
                            //your code to fetch results via SSH
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            txv_temp_indoor.setText(innerTemp);
                        }
                    }.execute(1);
                    //action
                }
            }
        });
        btnToggle2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // below you write code to change switch status and action to take
                if (isChecked) { //do something if checked
                    txv_heating_status.setText("On");
                    //action
                } else {
                    txv_heating_status.setText("Off");
                    //action
                }
            }
        });
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                run("tdtool --list-sensors");
                //your code to fetch results via SSH
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                txv_temp_indoor.setText(innerTemp);
                Double outdoorDouble = Double.parseDouble(innerTemp) - 10.2;
                //txv_temp_outdoor.setText(("" + outdoorDouble).substring(0, 4));
            }
        }.execute(1);

    }
}