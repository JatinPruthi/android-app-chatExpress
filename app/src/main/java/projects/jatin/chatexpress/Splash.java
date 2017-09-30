package projects.jatin.chatexpress;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.quickblox.auth.session.QBSettings;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import es.dmoral.toasty.Toasty;
import projects.jatin.chatexpress.Utils.SharedPref;

import static projects.jatin.chatexpress.MainActivity.ACCOUNT_KEY;
import static projects.jatin.chatexpress.MainActivity.APP_ID;
import static projects.jatin.chatexpress.MainActivity.AUTH_KEY;
import static projects.jatin.chatexpress.MainActivity.AUTH_SECRET;

public class Splash extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 2500;
    SharedPref sharedPref;
    public Context mContext;
    String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sharedPref=new SharedPref(Splash.this);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                name = sharedPref.getUser();


                if(name==null ||name.equalsIgnoreCase("")){

                    Intent i = new Intent(Splash.this,MainActivity.class);
                    startActivity(i);

                }
                else {
//                    Toast.makeText(mContext, ""+sharedPref.getUser(), Toast.LENGTH_SHORT).show();
//                    Toast.makeText(mContext, ""+sharedPref.getPassword(), Toast.LENGTH_SHORT).show();
                    initializeFramework();
                    doLogin();

                }

                // close this activity
                finish();
            }
        },SPLASH_TIME_OUT);
    }

    private void doLogin() {

//        final ProgressDialog progressDialog= ProgressDialog.show(Splash.this,"Authenticating...","Please wait.",false,false);

        final String user=sharedPref.getUser();
        final String password=sharedPref.getPassword();

        QBUser qbUser=new QBUser(user,password);

        QBUsers.signIn(qbUser).performAsync(new QBEntityCallback<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {
//                progressDialog.dismiss();
                Toasty.success(Splash.this, "Login Successful.", Toast.LENGTH_SHORT).show();

                Intent intent= new Intent(Splash.this,ChatDialogsActivity.class);
                intent.putExtra("user",user);
                intent.putExtra("password",password);
                startActivity(intent);
                finish();


            }

            @Override
            public void onError(QBResponseException e) {

//                progressDialog.dismiss();
                Toasty.error(Splash.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void initializeFramework() {

        QBSettings.getInstance().init(getApplicationContext(),APP_ID,AUTH_KEY,AUTH_SECRET);
        QBSettings.getInstance().setAccountKey(ACCOUNT_KEY);

    }
}
