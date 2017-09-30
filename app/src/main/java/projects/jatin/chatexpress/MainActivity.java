package projects.jatin.chatexpress;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.quickblox.auth.session.QBSettings;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import es.dmoral.toasty.Toasty;
import projects.jatin.chatexpress.Utils.SharedPref;

public class MainActivity extends AppCompatActivity {

    static final String APP_ID="60859";
    static final String AUTH_KEY="CW5gLWZuMbKjK3q";
    static final String AUTH_SECRET="xD6PJ7ghvyeQJX5";
    static final String ACCOUNT_KEY="BGk1piDLVzYzfBhJMGEF";

    static final int REQUEST_CODE=1000;

    SharedPref sharedPref;

    Button btnLogin,btnSignup;
    EditText edtUser,edtPassword;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref=new SharedPref(MainActivity.this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestRuntimePermissions();
        }


        initializeFramework();

        btnLogin= (Button) findViewById(R.id.main_btnLogin);
        btnSignup= (Button) findViewById(R.id.main_btnSignup);

        edtUser= (EditText) findViewById(R.id.main_editLogin);
        edtPassword= (EditText) findViewById(R.id.main_editPassword);


        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,SignUpActivity.class));
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                final ProgressDialog progressDialog= ProgressDialog.show(MainActivity.this,"Authenticating...","Please wait.",false,false);

                final String user=edtUser.getText().toString();
                final String password=edtPassword.getText().toString();

                QBUser qbUser=new QBUser(user,password);

                QBUsers.signIn(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        progressDialog.dismiss();
                        Toasty.success(MainActivity.this, "Login Successful.", Toast.LENGTH_SHORT).show();

                        Intent intent= new Intent(MainActivity.this,ChatDialogsActivity.class);
                        intent.putExtra("user",user);
                        intent.putExtra("password",password);
                        startActivity(intent);
                        finish();


                    }

                    @Override
                    public void onError(QBResponseException e) {

                        progressDialog.dismiss();
                        Toasty.error(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestRuntimePermissions() {

        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            },REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (REQUEST_CODE)
        {
            case REQUEST_CODE:
            {
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
                    Toasty.success(getBaseContext(), "Permission Granted!", Toast.LENGTH_SHORT).show();
                else
                    Toasty.success(getBaseContext(), "Permission Denied!", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void initializeFramework() {

        QBSettings.getInstance().init(getApplicationContext(),APP_ID,AUTH_KEY,AUTH_SECRET);
        QBSettings.getInstance().setAccountKey(ACCOUNT_KEY);

    }
}
