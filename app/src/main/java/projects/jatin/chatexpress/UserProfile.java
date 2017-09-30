package projects.jatin.chatexpress;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.quickblox.chat.QBChatService;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import es.dmoral.toasty.Toasty;
import projects.jatin.chatexpress.Common.Common;
import projects.jatin.chatexpress.Holder.QBUsersHolder;
import projects.jatin.chatexpress.Utils.SharedPref;

public class UserProfile extends AppCompatActivity {

    EditText edtPassword,edtOldPassword,edtFullName,edtEmail,edtPhone;
    Button btnUpdate,btnCancel;
    ProgressDialog mDialog;

    SharedPref sharedPref;

    ImageView user_avatar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        sharedPref=new SharedPref(UserProfile.this);

        //Add Toolbar
        Toolbar toolbar= (Toolbar) findViewById(R.id.user_update_toolbar);
        toolbar.setTitle("Chat Express");
        setSupportActionBar(toolbar);


        initViews();

        loadUserProfile();

        user_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),Common.SELECT_PICTURE);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                if(isValid())
//                {

                    String password=edtPassword.getText().toString();
                    String oldPassword=edtOldPassword.getText().toString();
                    String email=edtEmail.getText().toString();
                    String phone=edtPhone.getText().toString();
                    String fullName=edtFullName.getText().toString();

                    QBUser user=new QBUser();
                    user.setId(QBChatService.getInstance().getUser().getId());
                    if (!Common.isNullOrEmptyString(edtOldPassword.getText().toString()))
                    user.setOldPassword(oldPassword);
                    if(!Common.isNullOrEmptyString(edtPassword.getText().toString()))
                        user.setPassword(password);
                    if (!Common.isNullOrEmptyString(edtFullName.getText().toString()))
                        user.setFullName(fullName);
                    if (!Common.isNullOrEmptyString(edtEmail.getText().toString()))
                        user.setEmail(email);
                    if (!Common.isNullOrEmptyString(edtPhone.getText().toString()))
                        user.setPhone(phone);

                mDialog=ProgressDialog.show(UserProfile.this,null,"Please wait...",false,false);

                    QBUsers.updateUser(user).performAsync(new QBEntityCallback<QBUser>() {
                        @Override
                        public void onSuccess(QBUser user, Bundle bundle) {
                            Toasty.success(UserProfile.this, "User: "+user.getLogin()+" updated.", Toast.LENGTH_SHORT).show();
                            mDialog.dismiss();
                        }

                        @Override
                        public void onError(QBResponseException e) {

                            Toasty.error(UserProfile.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
//                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode==Common.SELECT_PICTURE)
        {
            Uri selectedImageUri=data.getData();

            final ProgressDialog mDialog=new ProgressDialog(UserProfile.this);
            mDialog.setMessage("Please wait");
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.show();

            //Update User Avatar
            InputStream in= null;
            try {
                in = getContentResolver().openInputStream(selectedImageUri);
                final Bitmap bitmap= BitmapFactory.decodeStream(in);
                ByteArrayOutputStream bos=new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
                File file=new File(Environment.getExternalStorageDirectory()+"/myimage.png");
                FileOutputStream fos= new FileOutputStream(file);
                fos.write(bos.toByteArray());
                fos.flush();
                fos.close();

                //Get file size
                final int imageSizekb= (int) (file.length()/1024);
                if(imageSizekb>=1024*100)
                {
                    Toast.makeText(this, "File size too large.", Toast.LENGTH_SHORT).show();
                }

                //upload file to server
                QBContent.uploadFileTask(file,true,null)
                        .performAsync(new QBEntityCallback<QBFile>() {
                            @Override
                            public void onSuccess(QBFile qbFile, Bundle bundle) {
                                //Set avatar for user
                                QBUser user= new QBUser();
                                user.setId(QBChatService.getInstance().getUser().getId());
                                user.setFileId(Integer.parseInt(qbFile.getId().toString()));

                                //Update user
                                QBUsers.updateUser(user)
                                        .performAsync(new QBEntityCallback<QBUser>() {
                                            @Override
                                            public void onSuccess(QBUser qbUser, Bundle bundle) {
                                                mDialog.dismiss();
                                                user_avatar.setImageBitmap(bitmap);
                                            }

                                            @Override
                                            public void onError(QBResponseException e) {

                                                Toasty.error(UserProfile.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }

                            @Override
                            public void onError(QBResponseException e) {

                                Toasty.error(UserProfile.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void loadUserProfile() {

        //lOAD AVATAR
        QBUsers.getUser(QBChatService.getInstance().getUser().getId())
                .performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {

                        //save to cache
                        QBUsersHolder.getInstance().putUser(qbUser);
                        if(qbUser.getFileId()!=null)
                        {
                            int profilePictureId=qbUser.getFileId();

                            QBContent.getFile(profilePictureId)
                                    .performAsync(new QBEntityCallback<QBFile>() {
                                        @Override
                                        public void onSuccess(QBFile qbFile, Bundle bundle) {

                                            String fileUrl=qbFile.getPublicUrl();
                                            Picasso.with(getBaseContext())
                                                    .load(fileUrl)
                                                    .into(user_avatar);
                                        }

                                        @Override
                                        public void onError(QBResponseException e) {

                                            Toasty.error(UserProfile.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onError(QBResponseException e) {

                        Toasty.error(UserProfile.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        QBUser currentUser=QBChatService.getInstance().getUser();
        String fullName=currentUser.getFullName();
        String email=currentUser.getEmail();
        String phone=currentUser.getPhone();

        edtEmail.setText(email);
        edtFullName.setText(fullName);
        edtPhone.setText(phone);
    }

    private boolean isValid() {
    if(!Common.isNullOrEmptyString(edtPassword.getText().toString()))
        return false;
    else if (!Common.isNullOrEmptyString(edtOldPassword.getText().toString()))
        return false;
    else if (!Common.isNullOrEmptyString(edtEmail.getText().toString()))
        return false;
    else if (!Common.isNullOrEmptyString(edtPhone.getText().toString()))
        return false;
    else if (!Common.isNullOrEmptyString(edtFullName.getText().toString()))
        return false;
        else
            return true;
    }

    private void initViews() {

        btnCancel= (Button) findViewById(R.id.update_user_btn_cancel);
        btnUpdate= (Button) findViewById(R.id.update_user_btn_update);

        edtEmail= (EditText) findViewById(R.id.update_edt_email);
        edtPhone= (EditText) findViewById(R.id.update_edt_phone);
        edtFullName= (EditText) findViewById(R.id.update_edt_full_name);
        edtPassword= (EditText) findViewById(R.id.update_edt_new_password);
        edtOldPassword= (EditText) findViewById(R.id.update_edt_old_password);

        user_avatar= (ImageView) findViewById(R.id.user_avatar);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_update_menu,menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.user_update_log_out:
                logOut();
                break;
            default:
                break;
        }
        return true;
    }

    private void logOut() {

        QBUsers.signOut().performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                QBChatService.getInstance().logout(new QBEntityCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid, Bundle bundle) {
                        Toasty.success(UserProfile.this, "Logout Successful", Toast.LENGTH_SHORT).show();
                        Intent intent= new Intent(UserProfile.this,MainActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //remove all previous activities
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        sharedPref.clear();
                        startActivity(intent);
                        finish();

                    }

                    @Override
                    public void onError(QBResponseException e) {

                        Toasty.error(UserProfile.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }
}
