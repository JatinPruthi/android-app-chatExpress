package projects.jatin.chatexpress;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import projects.jatin.chatexpress.Utils.SharedPref;

public class Splash extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 2500;
    SharedPref sharedPref;
    public Context mContext;
    String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                name = sharedPref.getUser();


                if(name==null ||name.equalsIgnoreCase("")){

                    Intent i = new Intent(Splash.this,MainActivity.class);
                    startActivity(i);

                }
                else {
                    Intent i = new Intent(Splash.this, ChatDialogsActivity.class);
                    startActivity(i);
                }

                // close this activity
                finish();
            }
        },SPLASH_TIME_OUT);
    }
}
