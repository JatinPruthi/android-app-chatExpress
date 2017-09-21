package projects.jatin.chatexpress;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bhargavms.dotloader.DotLoader;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.listeners.QBChatDialogParticipantListener;
import com.quickblox.chat.listeners.QBChatDialogTypingListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.model.QBPresence;
import com.quickblox.chat.request.QBDialogRequestBuilder;
import com.quickblox.chat.request.QBMessageGetBuilder;
import com.quickblox.chat.request.QBMessageUpdateBuilder;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestUpdateBuilder;
import com.squareup.picasso.Picasso;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import es.dmoral.toasty.Toasty;
import projects.jatin.chatexpress.Adapter.ChatMessageAdapter;
import projects.jatin.chatexpress.Common.Common;
import projects.jatin.chatexpress.Holder.QBChatMessagesHolder;

public class ChatMessageActivity extends AppCompatActivity implements QBChatDialogMessageListener{

    QBChatDialog qbChatDialog;
    ListView lstChatMessages;
    ImageButton submitButton;
    EditText edtContent;
    Toolbar toolbar;

    ChatMessageAdapter adapter;

    //Variables for Edit/Delete Message
    int contextMenuIndexClicked=-1;
    boolean isEditMode= false;
    QBChatMessage editMessage;

    //Typing status
    DotLoader dotLoader;

    // Update Online users
    ImageView img_online_count, dialog_avatar;
    TextView txt_online_count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_message);

        initViews();

        initChatDialogs();

        retrieveMessage();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!edtContent.getText().toString().isEmpty()) {

                    if (!isEditMode) {


                        QBChatMessage chatMessage = new QBChatMessage();
                        chatMessage.setBody(edtContent.getText().toString());
                        chatMessage.setSenderId(QBChatService.getInstance().getUser().getId());
                        chatMessage.setSaveToHistory(true);

                        try {
                            qbChatDialog.sendMessage(chatMessage);
                        } catch (SmackException.NotConnectedException e) {
                            Toasty.error(ChatMessageActivity.this, "Connect to the internet.", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }

                        //Fix Private Chat
                        if (qbChatDialog.getType() == QBDialogType.PRIVATE) {
                            //Cache Message
                            QBChatMessagesHolder.getInstance().putMessage(qbChatDialog.getDialogId(), chatMessage);
                            ArrayList<QBChatMessage> messages = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(chatMessage.getDialogId());
                            adapter = new ChatMessageAdapter(getBaseContext(), messages);
                            lstChatMessages.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        }

                        //Remove text from EditText
                        edtContent.setText("");
                        edtContent.setFocusable(true);
                    } else {

                        final ProgressDialog progressDialog = new ProgressDialog(ChatMessageActivity.this);
                        progressDialog.setMessage("Please wait");
                        progressDialog.show();

                        QBMessageUpdateBuilder messageUpdateBuilder = new QBMessageUpdateBuilder();
                        messageUpdateBuilder.updateText(edtContent.getText().toString()).markDelivered().markRead();
                        QBRestChatService.updateMessage(editMessage.getId(), qbChatDialog.getDialogId(), messageUpdateBuilder)
                                .performAsync(new QBEntityCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid, Bundle bundle) {
                                        //refresh data
                                        progressDialog.dismiss();
                                        retrieveMessage();
                                        isEditMode = false; //reset variable

                                        edtContent.setText("");
                                        edtContent.setFocusable(true);
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {

                                        progressDialog.dismiss();
                                        Toasty.error(ChatMessageActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if(qbChatDialog.getType()==QBDialogType.GROUP||qbChatDialog.getType()==QBDialogType.PUBLIC_GROUP)
            getMenuInflater().inflate(R.menu.chat_message_group_menu,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.chat_group_edit_name:
                editNameGroup();
                break;

            case R.id.chat_group_add_user:
                addUser();
                break;

            case R.id.chat_group_remove_user:
                removeUser();
                break;

        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode==RESULT_OK)
        {
            if(requestCode==Common.SELECT_PICTURE)
            {
                Uri selectedImageUri=data.getData();
                final ProgressDialog mDialog= new ProgressDialog(ChatMessageActivity.this);
                mDialog.setMessage("Please wait...");
                mDialog.setCancelable(false);
                mDialog.show();

                try {

                    //Convert uri to file
                    InputStream in=getContentResolver().openInputStream(selectedImageUri);
                    final Bitmap bitmap= BitmapFactory.decodeStream(in);
                    ByteArrayOutputStream  bos= new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
                    File file=new File(Environment.getExternalStorageDirectory()+"/image.png");
                    FileOutputStream fos=new FileOutputStream(file);
                    fos.write(bos.toByteArray());
                    fos.flush();
                    fos.close();

                    int imageSizeKb= (int) (file.length()/1024);
                    if(imageSizeKb>=(1024*100))
                    {
                        Toasty.error(this, "Image size too large!\nPlease choose a smaller sized image.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //Upload file
                    QBContent.uploadFileTask(file,true,null)
                            .performAsync(new QBEntityCallback<QBFile>() {
                                @Override
                                public void onSuccess(QBFile qbFile, Bundle bundle) {
                                    qbChatDialog.setPhoto(qbFile.getId().toString());

                                    //Update Chat Dialog
                                    QBRequestUpdateBuilder requestBuilder= new QBDialogRequestBuilder();
                                    QBRestChatService.updateGroupChatDialog(qbChatDialog,requestBuilder)
                                            .performAsync(new QBEntityCallback<QBChatDialog>() {
                                                @Override
                                                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                                mDialog.dismiss();
                                                    dialog_avatar.setImageBitmap(bitmap);
                                                }

                                                @Override
                                                public void onError(QBResponseException e) {

                                                    Toasty.error(ChatMessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }

                                @Override
                                public void onError(QBResponseException e) {

                                    Toasty.error(ChatMessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                }
                            });
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void removeUser() {

        Intent intent=new Intent(this,ListUsersActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA,qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE,Common.UPDATE_REMOVE_MODE);
        startActivity(intent);
    }

    private void addUser() {

        Intent intent=new Intent(this,ListUsersActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA,qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE,Common.UPDATE_ADD_MODE);
        startActivity(intent);

    }

    private void editNameGroup() {

        LayoutInflater inflater=LayoutInflater.from(this);
        View view=inflater.inflate(R.layout.dialog_edit_group_layout,null);

        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setView(view);
        final EditText newName= (EditText) view.findViewById(R.id.edt_new_group_name);

        builder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        qbChatDialog.setName(newName.getText().toString());

                        QBDialogRequestBuilder requestBuilder=new QBDialogRequestBuilder();
                        QBRestChatService.updateGroupChatDialog(qbChatDialog,requestBuilder).performAsync(new QBEntityCallback<QBChatDialog>() {
                            @Override
                            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                Toasty.info(ChatMessageActivity.this, "Group name changed to "+newName.getText().toString(), Toast.LENGTH_SHORT).show();
                                toolbar.setTitle(qbChatDialog.getName());
                            }

                            @Override
                            public void onError(QBResponseException e) {

                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        //Get index context menu click
        AdapterView.AdapterContextMenuInfo info=(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        contextMenuIndexClicked=info.position;

        switch(item.getItemId())
        {
            case R.id.chat_message_update_message:
                updateMessage();
                break;
            case R.id.chat_message_delete_message:
                deleteMessage();
                break;
        }
        return true;
    }

    private void deleteMessage() {

        final ProgressDialog progressDialog= new ProgressDialog(ChatMessageActivity.this);
        progressDialog.setMessage("Please wait");
        progressDialog.show();

        editMessage=QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatDialog.getDialogId()).get(contextMenuIndexClicked);

        QBRestChatService.deleteMessage(editMessage.getId(),false).performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {

                //refresh data
                retrieveMessage();
                progressDialog.dismiss();
            }

            @Override
            public void onError(QBResponseException e) {

                progressDialog.dismiss();
                Toasty.error(ChatMessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }



    private void updateMessage() {



        //Set message for edit text
        editMessage=QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatDialog.getDialogId()).get(contextMenuIndexClicked);
        edtContent.setText(editMessage.getBody());
        isEditMode=true; //Set Edit Mode to true
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.chat_message_context_menu,menu);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        qbChatDialog.removeMessageListrener(this);
    }

    private void retrieveMessage() {

        QBMessageGetBuilder messageGetBuilder=new QBMessageGetBuilder();
        messageGetBuilder.setLimit(500);  //500 messages limit in chat

        if(qbChatDialog!=null)
        {
            QBRestChatService.getDialogMessages(qbChatDialog,messageGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatMessage>>() {
                @Override
                public void onSuccess(ArrayList<QBChatMessage> qbChatMessages, Bundle bundle) {

                    QBChatMessagesHolder.getInstance().putMessages(qbChatDialog.getDialogId(),qbChatMessages);

                    adapter=new ChatMessageAdapter(getBaseContext(),qbChatMessages);
                    lstChatMessages.setAdapter(adapter);
                    adapter.notifyDataSetChanged();

                }

                @Override
                public void onError(QBResponseException e) {

                    Toasty.error(ChatMessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    private void initChatDialogs() {

        qbChatDialog= (QBChatDialog) getIntent().getSerializableExtra(Common.DIALOG_EXTRA);

        if(qbChatDialog.getPhoto()==null)
        {
            QBContent.getFile(Integer.parseInt(qbChatDialog.getPhoto()))
                    .performAsync(new QBEntityCallback<QBFile>() {
                        @Override
                        public void onSuccess(QBFile qbFile, Bundle bundle) {

                            String fileURL= qbFile.getPublicUrl();
                            Picasso.with(getBaseContext())
                                    .load(fileURL)
                                    .resize(50,50)
                                    .centerCrop()
                                    .into(dialog_avatar);
                        }

                        @Override
                        public void onError(QBResponseException e) {

                            Toasty.error(ChatMessageActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        qbChatDialog.initForChat(QBChatService.getInstance());

        //Register Listener incoming message
        QBIncomingMessagesManager incomingMessage=QBChatService.getInstance().getIncomingMessagesManager();
        incomingMessage.addDialogMessageListener(new QBChatDialogMessageListener() {
            @Override
            public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {

            }

            @Override
            public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

                Toasty.error(ChatMessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        //Add typing listener
        registerTypingForChatDialog(qbChatDialog);


        //Add Join group to group chat
        if(qbChatDialog.getType()== QBDialogType.PUBLIC_GROUP||qbChatDialog.getType()==QBDialogType.GROUP)
        {
            DiscussionHistory discussionHistory=new DiscussionHistory();
            discussionHistory.setMaxStanzas(0);

            qbChatDialog.join(discussionHistory, new QBEntityCallback() {
                @Override
                public void onSuccess(Object o, Bundle bundle) {

                }

                @Override
                public void onError(QBResponseException e) {
                Log.e("ERROR",""+e.getMessage());
                    Toasty.error(ChatMessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                }
            });

        }

        final QBChatDialogParticipantListener participantListener= new QBChatDialogParticipantListener() {
            @Override
            public void processPresence(String dialogId, QBPresence qbPresence) {

                if(dialogId==qbChatDialog.getDialogId())
                {
                    QBRestChatService.getChatDialogById(dialogId)
                            .performAsync(new QBEntityCallback<QBChatDialog>() {
                                @Override
                                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                    //Get Online user
                                    try {
                                        Collection<Integer> onlineList=qbChatDialog.getOnlineUsers();
                                        TextDrawable.IBuilder builder= TextDrawable.builder()
                                                .beginConfig()
                                                .withBorder(4)
                                                .endConfig()
                                                .round();

                                        TextDrawable online=builder.build("", Color.RED);
                                        img_online_count.setImageDrawable(online);
                                        txt_online_count.setText(String.format("%d/%d Online",onlineList.size(),qbChatDialog.getOccupants().size()));
//                                        Toast.makeText(ChatMessageActivity.this, "list "+onlineList.size(), Toast.LENGTH_SHORT).show();
//                                        Toast.makeText(ChatMessageActivity.this, "occ "+qbChatDialog.getOccupants().size(), Toast.LENGTH_SHORT).show();
//                                        txt_online_count.setText("Itne Online");
//                                        txt_online_count.setText(onlineList.size()+" "+qbChatDialog.getOccupants().size()+" Online");
                                    } catch (XMPPException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onError(QBResponseException e) {

                                    Toasty.error(ChatMessageActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                }
                            });

                }
            }
        };


        qbChatDialog.addParticipantListener(participantListener);

        qbChatDialog.addMessageListener(this);


        //Set title for toolbar
        toolbar.setTitle(qbChatDialog.getName());
        setSupportActionBar(toolbar);



    }

    private void registerTypingForChatDialog(QBChatDialog qbChatDialog) {

        QBChatDialogTypingListener typingListener= new QBChatDialogTypingListener() {
            @Override
            public void processUserIsTyping(String s, Integer integer) {

                if(dotLoader.getVisibility()!=View.VISIBLE)
                    dotLoader.setVisibility(View.VISIBLE);
            }

            @Override
            public void processUserStopTyping(String s, Integer integer) {

                if(dotLoader.getVisibility()!=View.INVISIBLE)
                    dotLoader.setVisibility(View.INVISIBLE);
            }
        };

        qbChatDialog.addIsTypingListener(typingListener);
    }

    private void initViews() {

        lstChatMessages= (ListView) findViewById(R.id.list_of_message);
        submitButton= (ImageButton) findViewById(R.id.send_button);
        edtContent= (EditText) findViewById(R.id.edt_content);
        edtContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {


                try {
                    qbChatDialog.sendIsTypingNotification();
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                try {
                    qbChatDialog.sendStopTypingNotification();
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }
        });


        img_online_count= (ImageView) findViewById(R.id.img_online_count);
        txt_online_count= (TextView) findViewById(R.id.txt_online_count);

        dotLoader= (DotLoader) findViewById(R.id.dot_loader);

        //Dialog avatar

        dialog_avatar= (ImageView) findViewById(R.id.dialog_avatar);
        dialog_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectImage=new Intent();
                selectImage.setType("image/*");
                selectImage.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(selectImage,"Select Picture"),Common.SELECT_PICTURE);
            }
        });

        //Add context menu
        registerForContextMenu(lstChatMessages);


        //Add Toolbar
        toolbar= (Toolbar) findViewById(R.id.chat_message_toolbar);

    }

    @Override
    public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
        //Cache Message
        QBChatMessagesHolder.getInstance().putMessage(qbChatMessage.getDialogId(),qbChatMessage);
        ArrayList<QBChatMessage> messages=QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatMessage.getDialogId());
        adapter=new ChatMessageAdapter(getBaseContext(),messages);
        lstChatMessages.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

        Log.e("ERROR",""+e.getMessage());
        Toasty.error(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
