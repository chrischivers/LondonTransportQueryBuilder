package com.customlondontransport;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class AddNewRoute extends Activity {

    // get UserRouteValues position if any (applies if UserRouteItem is being edited)
    private boolean inEditMode = false;
    private int positionToRestore = -1; //-1 as default if not in edit mode;

    private Menu menu;

    private Spinner transportModeSpinner;
    private Spinner routeLineSpinner;
    private Spinner directionSpinner;
    private Spinner startingStopSpinner;
    private Spinner maxNumberSpinner;

    private ArrayAdapter<RouteLine> routeLineAdapter;
    private ArrayAdapter<StationStop> startingStopAdapter;
    private ArrayAdapter<Direction> directionAdapter;

    private TextView transportModeLabel;
    private TextView routeLineLabel;
    private TextView directionLabel;
    private TextView startingStopLabel;
    private TextView maxNumberLabel;
    private TextView conditionsLabel;

    private Switch conditionsSwitch;
    private TextView conditionsPreviewText;

    private LinearLayout linearLayoutLeft;
    private LinearLayout linearLayoutRight;

    private Button addToOrUpdateUserListButton;

    private MyDatabase db;

    private DayTimeConditions dtc;

    private boolean isTransportModeSet = false;
    private boolean isRouteLineSet = false;
    private boolean isDirectionSet = false;
    private boolean isStartingStopSet = false;

    private Location currentLocation;
    private SharedPreferences prefs;

    private boolean localModeOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_route);

        getGPSLocation();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Set LOCAL MODE from prefs
        localModeOn = (prefs.getBoolean("Local_Mode", false));

        if (getIntent().hasExtra("Position")) {
            Bundle b = getIntent().getExtras();
            positionToRestore = b.getInt("Position");
            inEditMode = true;
        }

        // pull in the database
        new Thread(new LoadDatabase()).run();

        addToOrUpdateUserListButton = (Button) findViewById(R.id.routeAddToOrUpdateUserListButton);
        if (!inEditMode) {
            addToOrUpdateUserListButton.setText("Add");
        } else {
            addToOrUpdateUserListButton.setText("Update");
        }


        transportModeSpinner = (Spinner) findViewById(R.id.routeTransportModeSpinner);
        routeLineSpinner = (Spinner) findViewById(R.id.routeRouteLineSpinner);
        directionSpinner = (Spinner) findViewById(R.id.routeDirectionSpinner);
        startingStopSpinner = (Spinner) findViewById(R.id.routeStartingStopSpinner);
        maxNumberSpinner = (Spinner) findViewById(R.id.routeMaxNumberSpinner);

        transportModeLabel = (TextView) findViewById(R.id.routeTransportModeLabel);
        routeLineLabel = (TextView) findViewById(R.id.routeRouteLineLabel);
        directionLabel = (TextView) findViewById(R.id.routeDirectionLabel);
        startingStopLabel = (TextView) findViewById(R.id.routeStartingStopLabel);
        maxNumberLabel = (TextView) findViewById(R.id.routeMaxNumberLabel);
        conditionsLabel = (TextView) findViewById(R.id.routeConditionsLabel);

        conditionsSwitch = (Switch) findViewById(R.id.routeConditionsSwitch);
        conditionsPreviewText = (TextView) findViewById(R.id.routeConditionsPreviewText);

        linearLayoutLeft = (LinearLayout) findViewById(R.id.routeLinearLayoutLeft1);
        linearLayoutRight = (LinearLayout) findViewById(R.id.routeLinearLayoutRight1);

        // Populate transport mode adapter and Max Number Adapter as default first step
        ArrayAdapter<CharSequence> transportModeAdapter = ArrayAdapter.createFromResource(this, R.array.transport_mode_array, android.R.layout.simple_spinner_item);
        ArrayAdapter<CharSequence> maxNumberAdapter = ArrayAdapter.createFromResource(this, R.array.max_number_array, android.R.layout.simple_spinner_item);

        linearLayoutLeft.removeAllViewsInLayout();
        linearLayoutRight.removeAllViewsInLayout();
        linearLayoutLeft.addView(transportModeLabel);
        linearLayoutRight.addView(transportModeSpinner);

        setLayout();

        transportModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transportModeSpinner.setAdapter(transportModeAdapter);

        maxNumberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        maxNumberSpinner.setAdapter(maxNumberAdapter);

        // Adjust transport spinner if in EDIT MODE
        if (inEditMode) {
            String transportMode = UserListView.userValues.get(positionToRestore).getTransportForm();
            int adapterPosition = transportModeAdapter.getPosition(transportMode);
            transportModeSpinner.setSelection(adapterPosition);
            onTransportModeSpinnerChange();
        }
        // Set MaxNumber if in EDIT MODE
        if (inEditMode) {
           maxNumberSpinner.setSelection(UserListView.userValues.get(positionToRestore).getMaxNumberToShow()); //add 1 to translate into spinner values
        }

        // Set conditions if in EDIT MODE
        if (inEditMode) {
            dtc = UserListView.userValues.get(positionToRestore).getDayTimeConditions();
            if (dtc == null) {
                conditionsSwitch.setChecked(false);
            } else {
                conditionsSwitch.setChecked(true);
            }
        }


        transportModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                onTransportModeSpinnerChange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                isTransportModeSet = false;
                setLayout();
            }
        });

        routeLineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                onRouteLineSpinnerChange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                isRouteLineSet = false;
                setLayout();
            }
        });

        startingStopSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                onStartingStopSpinnerChange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                isStartingStopSet = false;
                setLayout();
            }
        });

        directionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                onDirectionSpinnerChange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                isDirectionSet = false;
                setLayout();
            }
        });

        conditionsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    loadSetDayTimeConditions();
                } else {
                    dtc = null;
                    conditionsPreviewText.setText("");
                }
            }
        });

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_new_route_menu, menu);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_new_route_menu, menu);
        // Set initial local mode icon
        if (localModeOn) {
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_action_location_found));
        } else {
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_action_location_off));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.reset_route) {
            isTransportModeSet = false;
            setLayout();
            conditionsSwitch.setChecked(false);
            conditionsPreviewText.setText("");
            transportModeSpinner.setSelection(0);
            return true;
        } else if (id == R.id.local_mode_toggle) {
            if (localModeOn) {
                localModeOn = false;
                menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_action_location_off));

                onTransportModeSpinnerChange();
            } else {
                localModeOn = true;
                menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.ic_action_location_found));

                onTransportModeSpinnerChange();
            }
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    public void loadSetDayTimeConditions() {
        Intent intent = new Intent(this, SetDayTimeConditions.class);
        startActivityForResult(intent, 1);
    }

    public void onTransportModeSpinnerChange() {
        isRouteLineSet = false;
        isStartingStopSet = false;
        isDirectionSet = false;

        if (transportModeSpinner.getSelectedItem().equals("Tube")) {
            routeLineAdapter = new ArrayAdapter<RouteLine>(getBaseContext(), android.R.layout.simple_spinner_item, fetchTubeLines());
            routeLineAdapter.insert(new RouteLine(),0); //insert empty to front
            routeLineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            routeLineSpinner.setAdapter(routeLineAdapter);
            isTransportModeSet = true;

        } else if (transportModeSpinner.getSelectedItem().equals("Bus")) {
            routeLineAdapter = new ArrayAdapter<RouteLine>(getBaseContext(), android.R.layout.simple_spinner_item, fetchBusRoutes());
            routeLineAdapter.insert(new RouteLine(),0); //insert empty to front
            routeLineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            routeLineSpinner.setAdapter(routeLineAdapter);
            isTransportModeSet = true;

        } else {
            isTransportModeSet = false;
        }

        // IF IN EDIT MODE
        if (inEditMode) {
            String routeLineID = ((UserRouteItem) UserListView.userValues.get(positionToRestore)).getRouteLine().getID();
            for (int i = 0; i < routeLineAdapter.getCount(); i++) {
                if (routeLineAdapter.getItem(i).getID().equals(routeLineID)) {
                    routeLineSpinner.setSelection(i, true);
                    break;
                }
            }
            onRouteLineSpinnerChange();
        }
        setLayout();
    }

    public void onRouteLineSpinnerChange() {
        isStartingStopSet = false;
        isDirectionSet = false;

        if (transportModeSpinner.getSelectedItem().equals("Tube") && !((RouteLine) routeLineSpinner.getSelectedItem()).getID().equals("")) {
            startingStopAdapter = new ArrayAdapter<StationStop>(getBaseContext(), android.R.layout.simple_spinner_item, fetchTubeStations(((RouteLine) routeLineSpinner.getSelectedItem()).getID()));
            startingStopAdapter.insert(new StationStop(),0); //insert empty to front
            startingStopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            startingStopSpinner.setAdapter(startingStopAdapter);
            isRouteLineSet = true;

            // IF IN EDIT MODE
            if (inEditMode) {
                String startingStopID = UserListView.userValues.get(positionToRestore).getStartingStop().getID();
                for(int i=0 ; i<startingStopAdapter.getCount() ; i++){
                    if (startingStopAdapter.getItem(i).getID().equals(startingStopID)) {
                        startingStopSpinner.setSelection(i, true);
                        break;
                    }
                }
                onStartingStopSpinnerChange();
            }

        } else if (transportModeSpinner.getSelectedItem().equals("Bus") && !((RouteLine) routeLineSpinner.getSelectedItem()).getID().equals("")) {
            directionAdapter = new ArrayAdapter<Direction>(getBaseContext(), android.R.layout.simple_spinner_item, fetchBusDirections(((RouteLine) routeLineSpinner.getSelectedItem()).getID()));
            directionAdapter.insert(new Direction(),0); //insert empty to front
            directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            directionSpinner.setAdapter(directionAdapter);
            isRouteLineSet = true;

            // IF IN EDIT MODE
            if (inEditMode) {
                int directionID = ((UserRouteItem) UserListView.userValues.get(positionToRestore)).getDirection().getID();
                for(int i=0 ; i<directionAdapter.getCount() ; i++){
                    if (directionAdapter.getItem(i).getID() == directionID) {
                        directionSpinner.setSelection(i, true);
                        break;
                    }
                }
                onDirectionSpinnerChange();
            }
        } else {
            isRouteLineSet = false;
        }
        setLayout();
    }

    public void onStartingStopSpinnerChange() {
        if (transportModeSpinner.getSelectedItem().equals("Tube") && !((StationStop) startingStopSpinner.getSelectedItem()).getID().equals("")) {
            directionAdapter = new ArrayAdapter<Direction>(getBaseContext(), android.R.layout.simple_spinner_item, fetchTubeDirectionsAndPlatforms(((RouteLine) routeLineSpinner.getSelectedItem()).getID(), ((StationStop) startingStopSpinner.getSelectedItem()).getID()));
            directionAdapter.insert(new Direction(),0); //insert empty to front
            directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            directionSpinner.setAdapter(directionAdapter);
            isStartingStopSet = true;

            // IF IN EDIT MODE
            if (inEditMode) {
                int directionID = ((UserRouteItem) UserListView.userValues.get(positionToRestore)).getDirection().getID();
                for(int i=0 ; i<directionAdapter.getCount() ; i++){
                    if (directionAdapter.getItem(i).getID() == directionID) {
                        directionSpinner.setSelection(i, true);
                        break;
                    }
                }
                onDirectionSpinnerChange();
            }

        } else
            isStartingStopSet = transportModeSpinner.getSelectedItem().equals("Bus") && !((StationStop) startingStopSpinner.getSelectedItem()).getID().equals("");
        setLayout();
    }

    public void onDirectionSpinnerChange() {
        if (transportModeSpinner.getSelectedItem().equals("Bus") && ((Direction) directionSpinner.getSelectedItem()).getID() != 0) {
            startingStopAdapter = new ArrayAdapter<StationStop>(getBaseContext(), android.R.layout.simple_spinner_item, fetchBusStops(routeLineSpinner.getSelectedItem().toString(), ((Direction) directionSpinner.getSelectedItem()).getID()));
            startingStopAdapter.insert(new StationStop(),0); //insert empty to front
            startingStopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            startingStopSpinner.setAdapter(startingStopAdapter);
            isDirectionSet = true;

            // IF IN EDIT MODE
            if (inEditMode) {
                String startingStopID = UserListView.userValues.get(positionToRestore).getStartingStop().getID();
                for(int i=0 ; i<startingStopAdapter.getCount() ; i++){
                    if (startingStopAdapter.getItem(i).getID().equals(startingStopID)) {
                        startingStopSpinner.setSelection(i, true);
                        break;
                    }
                }
                onStartingStopSpinnerChange();
            }
        } else
            isDirectionSet = transportModeSpinner.getSelectedItem().equals("Tube") && directionSpinner.getSelectedItemPosition() != 0;
        setLayout();
    }

    private void setLayout() {

        if (!isTransportModeSet) {
            linearLayoutLeft.removeAllViewsInLayout();
            linearLayoutRight.removeAllViewsInLayout();
            linearLayoutLeft.addView(transportModeLabel);
            linearLayoutRight.addView(transportModeSpinner);
            addToOrUpdateUserListButton.setVisibility(View.INVISIBLE);
        } else if (!isRouteLineSet) {
            linearLayoutLeft.removeAllViewsInLayout();
            linearLayoutRight.removeAllViewsInLayout();
            linearLayoutLeft.addView(transportModeLabel);
            linearLayoutRight.addView(transportModeSpinner);
            linearLayoutLeft.addView(routeLineLabel);
            linearLayoutRight.addView(routeLineSpinner);
            addToOrUpdateUserListButton.setVisibility(View.INVISIBLE);

        } else if (transportModeSpinner.getSelectedItem().equals("Bus")) {
            if (!isDirectionSet) {
                linearLayoutLeft.removeAllViewsInLayout();
                linearLayoutRight.removeAllViewsInLayout();
                linearLayoutLeft.addView(transportModeLabel);
                linearLayoutRight.addView(transportModeSpinner);
                linearLayoutLeft.addView(routeLineLabel);
                linearLayoutRight.addView(routeLineSpinner);
                linearLayoutLeft.addView(directionLabel);
                linearLayoutRight.addView(directionSpinner);
                addToOrUpdateUserListButton.setVisibility(View.INVISIBLE);
            } else if (!isStartingStopSet) {
                linearLayoutLeft.removeAllViewsInLayout();
                linearLayoutRight.removeAllViewsInLayout();
                linearLayoutLeft.addView(transportModeLabel);
                linearLayoutRight.addView(transportModeSpinner);
                linearLayoutLeft.addView(routeLineLabel);
                linearLayoutRight.addView(routeLineSpinner);
                linearLayoutLeft.addView(directionLabel);
                linearLayoutRight.addView(directionSpinner);
                linearLayoutLeft.addView(startingStopLabel);
                linearLayoutRight.addView(startingStopSpinner);
                addToOrUpdateUserListButton.setVisibility(View.INVISIBLE);
            } else {
                linearLayoutLeft.removeAllViewsInLayout();
                linearLayoutRight.removeAllViewsInLayout();
                linearLayoutLeft.addView(transportModeLabel);
                linearLayoutRight.addView(transportModeSpinner);
                linearLayoutLeft.addView(routeLineLabel);
                linearLayoutRight.addView(routeLineSpinner);
                linearLayoutLeft.addView(directionLabel);
                linearLayoutRight.addView(directionSpinner);
                linearLayoutLeft.addView(startingStopLabel);
                linearLayoutRight.addView(startingStopSpinner);
                linearLayoutLeft.addView(conditionsLabel);
                linearLayoutRight.addView(conditionsSwitch);
                linearLayoutLeft.addView(maxNumberLabel);
                linearLayoutRight.addView(maxNumberSpinner);
                addToOrUpdateUserListButton.setVisibility(View.VISIBLE);
            }
        } else if (transportModeSpinner.getSelectedItem().equals("Tube")) {
            if (!isStartingStopSet) {
                linearLayoutLeft.removeAllViewsInLayout();
                linearLayoutRight.removeAllViewsInLayout();
                linearLayoutLeft.addView(transportModeLabel);
                linearLayoutRight.addView(transportModeSpinner);
                linearLayoutLeft.addView(routeLineLabel);
                linearLayoutRight.addView(routeLineSpinner);
                linearLayoutLeft.addView(startingStopLabel);
                linearLayoutRight.addView(startingStopSpinner);
                addToOrUpdateUserListButton.setVisibility(View.INVISIBLE);
            } else if (!isDirectionSet) {
                linearLayoutLeft.removeAllViewsInLayout();
                linearLayoutRight.removeAllViewsInLayout();
                linearLayoutLeft.addView(transportModeLabel);
                linearLayoutRight.addView(transportModeSpinner);
                linearLayoutLeft.addView(routeLineLabel);
                linearLayoutRight.addView(routeLineSpinner);
                linearLayoutLeft.addView(startingStopLabel);
                linearLayoutRight.addView(startingStopSpinner);
                linearLayoutLeft.addView(directionLabel);
                linearLayoutRight.addView(directionSpinner);
                addToOrUpdateUserListButton.setVisibility(View.INVISIBLE);
            } else {
                linearLayoutLeft.removeAllViewsInLayout();
                linearLayoutRight.removeAllViewsInLayout();
                linearLayoutLeft.addView(transportModeLabel);
                linearLayoutRight.addView(transportModeSpinner);
                linearLayoutLeft.addView(routeLineLabel);
                linearLayoutRight.addView(routeLineSpinner);
                linearLayoutLeft.addView(startingStopLabel);
                linearLayoutRight.addView(startingStopSpinner);
                linearLayoutLeft.addView(directionLabel);
                linearLayoutRight.addView(directionSpinner);
                linearLayoutLeft.addView(conditionsLabel);
                linearLayoutRight.addView(conditionsSwitch);
                linearLayoutLeft.addView(maxNumberLabel);
                linearLayoutRight.addView(maxNumberSpinner);
                addToOrUpdateUserListButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private List<RouteLine> fetchBusRoutes() {
       if (localModeOn) {
           return db.getNearestBusRoutes(currentLocation);
       } else {
           return db.getBusRoutesAlphabetical();
       }
    }

    private List<Direction> fetchBusDirections(String busRoute) {
        return db.getBusDirections(busRoute);
    }


    private List<StationStop> fetchBusStops(String busRoute, int busDirection) {
        if (localModeOn) {
            return sortStationsByNearest(db.getBusStopsForRouteAlphabetical(busRoute, busDirection));
        } else {
            return db.getBusStopsForRouteAlphabetical(busRoute, busDirection);
        }
    }

    private List<StationStop> fetchTubeStations(String tubeLineID) {
        if (localModeOn) {
            return sortStationsByNearest(db.getTubeStationsAlphabetical(tubeLineID));
        } else {
            return db.getTubeStationsAlphabetical(tubeLineID);
        }
    }

    private List<RouteLine> fetchTubeLines() {
        return db.getTubeLinesAlphabetical();
    }

    private synchronized List<Direction> fetchTubeDirectionsAndPlatforms(String tubeLineID, String tubeStationID) {
        APIFetcher apifetcher = new APIFetcher();
        apifetcher.execute(tubeLineID, tubeStationID);
        return apifetcher.getTubeDirectionsAndPlatformList();

    }

    public List<StationStop> sortStationsByNearest(List<StationStop> list) {
        Collections.sort(list, new Comparator<StationStop>(){

            @Override
            public int compare(StationStop lhs, StationStop rhs) {
                if (lhs.getLocation().distanceTo(currentLocation) > rhs.getLocation().distanceTo(currentLocation)) {
                    return 1;
                } else if (lhs.getLocation().distanceTo(currentLocation) < rhs.getLocation().distanceTo(currentLocation)) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        return list;
    }


    //button
    public void addToOrUpdateAndReturnToUserListView(View view) {
        int maxNumberToFetch = maxNumberSpinner.getSelectedItemPosition(); //0 = all
        UserRouteItem userRouteItem = new UserRouteItem(transportModeSpinner.getSelectedItem().toString(), ((RouteLine) routeLineSpinner.getSelectedItem()), ((Direction) directionSpinner.getSelectedItem()), ((StationStop) startingStopSpinner.getSelectedItem()), dtc, maxNumberToFetch);
        saveCustomSettingsToPrefs(); //Save custom prefs
        if (!inEditMode) {
            // If not in EDIT MODE then add to List
            UserListView.userValues.add(userRouteItem);
        } else {
            // Replace at position
            UserListView.userValues.set(positionToRestore, userRouteItem);
        }

        setResult(RESULT_OK, null);
        finish();
    }


    public class LoadDatabase implements Runnable {
        @Override
        public void run() {
            db = new MyDatabase(getApplicationContext());
            System.out.println("Database Loaded");
        }
    }

    //Return from conditions method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            dtc = (DayTimeConditions) data.getSerializableExtra("DayTimeConditions");
            Toast.makeText(this, "Conditions set", Toast.LENGTH_SHORT).show();
            conditionsPreviewText.setText(dtc.toString());
        } else {
            Toast.makeText(this, "No conditions set", Toast.LENGTH_SHORT).show();
            conditionsSwitch.setChecked(false);
            conditionsPreviewText.setText("");

        }
    }

    public void saveCustomSettingsToPrefs() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("Local_Mode", localModeOn);
        editor.apply();
    }

    public void getGPSLocation() {
        GPSTracker gps = new GPSTracker(AddNewRoute.this);

        // check if GPS enabled
        if(gps.canGetLocation()){
            currentLocation = new Location("");
            currentLocation.setLatitude(gps.getLatitude());
            currentLocation.setLongitude(gps.getLongitude());

            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + currentLocation.getLatitude() + "\nLong: " + currentLocation.getLongitude(), Toast.LENGTH_LONG).show();
        } else {
            currentLocation = null;
            gps.showSettingsAlert();
        }

        //TODO
        //For emulator testing GPS being set manually
        currentLocation.setLatitude(51.465053721837);
        currentLocation.setLongitude(-0.29280117154);
    }

    class APIFetcher extends AsyncTask<String, Void, Void> {

        List<Direction> tubeDirectionsAndPlatformList;

        @Override
        protected synchronized Void doInBackground(String... strings) {
            tubeDirectionsAndPlatformList = null;
            tubeDirectionsAndPlatformList = (new APIInterface().fetchTubeDirectionsAndPlatforms(strings[0], strings[1]));
            notifyAll();
            return null;
        }

        public synchronized List<Direction> getTubeDirectionsAndPlatformList() {
            while (tubeDirectionsAndPlatformList == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return tubeDirectionsAndPlatformList;
        }

    }

}
