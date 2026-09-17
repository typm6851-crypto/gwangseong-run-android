package com.gwangseong.run;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public final class ApiClient {
    private static final String BASE="https://gwangseong-run.typm6851.chatgpt.site";
    public interface Callback { void done(boolean ok, String message, JSONObject data); }

    public static void login(String phone,String password,Callback callback){
        JSONObject body=new JSONObject(); try{body.put("phone",phone);body.put("password",password);}catch(Exception ignored){}
        post("/api/app/login",body,callback);
    }
    public static void register(String name,String phone,String password,Callback callback){
        JSONObject body=new JSONObject(); try{body.put("name",name);body.put("phone",phone);body.put("password",password);}catch(Exception ignored){}
        post("/api/app/register",body,callback);
    }
    public static void resetPassword(String name,String phone,String password,Callback callback){
        JSONObject body=new JSONObject(); try{body.put("name",name);body.put("phone",phone);body.put("password",password);}catch(Exception ignored){}
        post("/api/app/reset-password",body,callback);
    }
    public static void submit(String token,int steps,long seconds,double km,Callback callback){
        JSONObject body=new JSONObject(); try{body.put("token",token);body.put("steps",steps);body.put("seconds",seconds);body.put("distanceKm",km);}catch(Exception ignored){}
        post("/api/app/run",body,callback);
    }
    private static void post(String path,JSONObject body,Callback callback){
        new Thread(()->{
            HttpURLConnection connection=null;
            try{
                connection=(HttpURLConnection)new URL(BASE+path).openConnection();
                connection.setRequestMethod("POST");connection.setConnectTimeout(12000);connection.setReadTimeout(12000);connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type","application/json; charset=utf-8");
                byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
                try(OutputStream out=connection.getOutputStream()){out.write(bytes);}
                int status=connection.getResponseCode(); InputStream input=status<400?connection.getInputStream():connection.getErrorStream();
                StringBuilder text=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null)text.append(line);}
                JSONObject result=new JSONObject(text.toString());callback.done(status>=200&&status<300&&result.optBoolean("ok"),result.optString("message","처리했어요."),result);
            }catch(Exception error){callback.done(false,"인터넷 연결을 확인해주세요.",new JSONObject());}
            finally{if(connection!=null)connection.disconnect();}
        }).start();
    }
}
