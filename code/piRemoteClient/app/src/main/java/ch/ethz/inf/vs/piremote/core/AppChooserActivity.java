package ch.ethz.inf.vs.piremote.core;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import SharedConstants.ApplicationCsts;
import SharedConstants.CoreCsts.ServerState;
import ch.ethz.inf.vs.piremote.R;

/**
 * The Application Chooser is also represented by an application.
 */
public class AppChooserActivity extends AbstractClientApplication {

    private final ServerState[] serverStates = ServerState.values();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_chooser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        application.setActivity(this); // TODO: NULL

        // Get an array of all the available applications (given by the ServerState enumeration) and store their names.
        final String[] applicationNames = new String[serverStates.length];
        for (int i = 0; i < serverStates.length; i++) {
            applicationNames[i] = serverStates[i].name();
        }

        // Display the available applications in a ListView.
        ListView mApplicationList = (ListView) findViewById(R.id.list_application);
        mApplicationList.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, applicationNames));

        mApplicationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                chooseApplication(position);
            }
        });
    }

    @Override
    public void onApplicationStart(ApplicationCsts.ApplicationState startState) {

        // Create Intent to adapt UI for the new application.
/*
        Intent startApplication = new Intent(activity.getBaseContext(), AppChooserActivity.class);
        activity.startActivity(startApplication);
*/
    }

    @Override
    public void onApplicationStop() {

    }

    @Override
    public void onApplicationStateChange(ApplicationCsts.ApplicationState newState) {

    }

    @Override
    public void onReceiveInt(int i) {

    }

    @Override
    public void onReceiveDouble(double d) {

    }

    @Override
    public void onReceiveString(String str) {

    }

    private void chooseApplication(int position) {
        switch (serverStates[position]) {
            case NONE:
                break;
            case SERVER_DOWN:
                // TEST ONLY
                // Switch to back to main
/*
                application.onApplicationStop();
                application = ApplicationFactory.makeApplication(ServerState.SERVER_DOWN);
                application.onApplicationStart(null);
*/
                // TEST ONLY
                break;
            default:
                // TEST ONLY
                // Switch to back to main
/*
                application.onApplicationStop();
                application = ApplicationFactory.makeApplication(serverStates[position]);
                application.onApplicationStart(null);
*/
                // TEST ONLY
                // application.clientCore.changeServerState(serverStates[position]);
                break;
        }
    }
}
