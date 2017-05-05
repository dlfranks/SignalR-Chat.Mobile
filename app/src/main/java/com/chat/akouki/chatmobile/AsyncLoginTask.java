package com.chat.akouki.chatmobile;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import com.chat.akouki.chatmobile.helpers.PrefsManager;
import com.chat.akouki.chatmobile.helpers.User;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import microsoft.aspnet.signalr.client.http.CookieCredentials;

public class AsyncLoginTask extends AsyncTask<String, Void, CookieCredentials> {

    Activity activity;
    ProgressDialog progressDialog;

    public AsyncLoginTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = ProgressDialog.show(activity, "", "Signing In...", true, false);
    }

    @Override
    protected CookieCredentials doInBackground(String... params) {

        DefaultHttpClient httpclient = new DefaultHttpClient();

        try {

            String url = params[0];
            String username = params[1];
            String password = params[2];

            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpGet);
            HttpEntity resEntity = response.getEntity();
            String responseBody = EntityUtils.toString(resEntity);

            // Get __RequestVerificationToken
            String token = GetToken(responseBody);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setHeader("Set-Cookie", token);

            // Build Request Form
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("__RequestVerificationToken", token));
            nameValuePairs.add(new BasicNameValuePair("Username", username));
            nameValuePairs.add(new BasicNameValuePair("Password", password));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute Post Request
            response = httpclient.execute(httpPost);

        } catch (Exception e) {
        } finally {
            httpclient.getConnectionManager().shutdown();
        }

        final CookieCredentials cc = new CookieCredentials();
        List<Cookie> cookies = httpclient.getCookieStore().getCookies();

        // Check if user is authenticated
        boolean isAuthed = false;
        for (Cookie c : cookies) {
            if (!isAuthed && c.getName().equals(".AspNet.ApplicationCookie"))
                isAuthed = true;

            cc.addCookie(c.getName(), c.getValue());
        }

        return isAuthed ? cc : null;
    }

    @Override
    protected void onPostExecute(CookieCredentials cookieCredentials) {
        progressDialog.cancel();

        if (cookieCredentials != null) {
            User.loginCredentials = cookieCredentials;
            PrefsManager.saveAuthCookie(activity, cookieCredentials);
            activity.startActivity(new Intent(activity, MainActivity.class));
        } else {
            Toast.makeText(activity, "Please enter valid username / password", Toast.LENGTH_SHORT).show();
        }
    }

    private String GetToken(String content) {
        int startIndex = content.indexOf("__RequestVerificationToken");
        int endIndex = content.indexOf("/>", startIndex);

        if (startIndex == -1 || endIndex == -1) {
            return null;
        }

        content = content.substring(startIndex, endIndex);

        // Find Token
        Pattern p = Pattern.compile("value=\"(\\S+)\"");
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
