package projects.jatin.chatexpress;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.quickblox.auth.QBAuth;
import com.quickblox.auth.session.QBSession;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import es.dmoral.toasty.Toasty;

public class SignUpActivity extends AppCompatActivity {

    Button btnSignUp,btnCancel;
    EditText edtFullName,edtUser,edtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        registerSession();

        btnSignUp= (Button) findViewById(R.id.signup_btnSignup);
        btnCancel= (Button) findViewById(R.id.signup_btnCancel);
        edtFullName= (EditText) findViewById(R.id.signup_editFullName);
        edtUser= (EditText) findViewById(R.id.signup_editLogin);
        edtPassword= (EditText) findViewById(R.id.signup_editPassword);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user=edtUser.getText().toString();
                String password=edtPassword.getText().toString();

                QBUser qbUser=new QBUser(user,password);

                qbUser.setFullName(edtFullName.getText().toString());


                QBUsers.signUp(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        Toasty.success(SignUpActivity.this, "Sign up successful.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(QBResponseException e) {

                        Toasty.error(SignUpActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });
    }

    private void registerSession() {

        QBAuth.createSession().performAsync(new QBEntityCallback<QBSession>() {
            @Override
            public void onSuccess(QBSession qbSession, Bundle bundle) {

            }

            @Override
            public void onError(QBResponseException e) {

                Log.e("ERROR",e.getMessage());
            }
        });

    }
}
