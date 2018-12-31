package com.example.kandels.myapplication;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class PresentationActivity extends AppCompatActivity {

    private static final int SCAN_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);


    }

    public void StartManual(View view) {
        Intent manualActivity = new Intent(PresentationActivity.this, MainActivity.class);
        manualActivity.putExtra(ManualFragment.MANUAL, true);
        startActivity(manualActivity);
    }

    public void StartAutomatic(View view){
        Intent automaticActivity = new Intent(PresentationActivity.this, MainActivity.class);
        automaticActivity.putExtra(ManualFragment.MANUAL, false);
        startActivity(automaticActivity);
    }

    public void StartConnection(View view) {
        Intent scanActivity = new Intent(PresentationActivity.this, DeviceScanActivity.class);
        startActivityForResult(scanActivity, SCAN_CODE);
    }

    public void ShowTutorial() {
        Intent tutorialActivity = new Intent(PresentationActivity.this, TutorialActivity.class);
        startActivity(tutorialActivity);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==SCAN_CODE && resultCode==RESULT_OK){

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_manual, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_tutorial:
                ShowTutorial();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
