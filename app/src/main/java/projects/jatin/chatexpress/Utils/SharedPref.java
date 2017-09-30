package projects.jatin.chatexpress.Utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Jateen on 30-09-2017.
 */

public class SharedPref {

    Context context;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    String user,password;

    public SharedPref(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(context.getPackageName(),Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public String getUser() {
        return preferences.getString("user",null);
    }

    public void setUser(String user) {
        editor.putString("user",user);
        editor.apply();
    }

    public String getPassword() {
        return preferences.getString("password",null);
    }

    public void setPassword(String password) {
        editor.putString("password",password);
        editor.apply();
    }

    public void clear(){

        editor.clear();
        editor.apply();

    }
}
