package com.straphq.strap;

import com.getpebble.android.kit.util.PebbleDictionary;
import org.json.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;




public class StrapMetrics
{
    // -----------------------
    // #defines from strap.c
    // -----------------------
    private static final int KEY_OFFSET = 48000;
    private static final int T_TIME_BASE = 1000;
    private static final int T_TS = 1;
    private static final int T_X = 2;
    private static final int T_Y = 3;
    private static final int T_Z = 4;
    private static final int T_DID_VIBRATE = 5;
    private static final int T_ACTIVITY = 2000;
    private static final int T_LOG = 3000;
    // -----------------------
    private static final int strap_api_num_samples = 10;
    // -----------------------
    
  private String strapURL = "https://api.straphq.com/create/visit/with/";
//    private String strapURL = "http://192.168.2.8:8000/create/visit/with/";
    private String appID;
    
    Calendar mCalendar = new GregorianCalendar();  
    TimeZone mTimeZone = mCalendar.getTimeZone();  
    int mGMTOffset = mTimeZone.getRawOffset();  
    long tz_offset = TimeUnit.HOURS.convert(mGMTOffset, TimeUnit.MILLISECONDS);

    
    private JSONArray tmpstore = new JSONArray();
    
    public StrapMetrics(String url, String appid) {
        appID = appid;
        strapURL = url;
    }
    
    public StrapMetrics(String appid) {
        appID = appid;
    }
    
    private void concatJSONArrays(JSONArray result, JSONArray tmp) throws JSONException {
        // append the values in tmp to the result JSONArray
        for(int i = 0; i < tmp.length(); i++) {
            result.put(tmp.getJSONObject(i));
        }
    }
    
    public Boolean canHandleMsg(PebbleDictionary data) {
     
        if( data.contains(KEY_OFFSET + T_ACTIVITY)) {
            return true;
        }
        if( data.contains(KEY_OFFSET + T_LOG)) {
            return true;
        }
        
        return false;    	
    }
    
    public void processReceiveData(PebbleDictionary data, int min_readings, Properties lp) throws JSONException, IOException {
        String query;
        
        //TODO: Use Pebble serial instead of Android
        String serial = null; 

        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.serialno");
        } catch (Exception ignored) {
        }
        
       
        int key = KEY_OFFSET + T_LOG;
        if(!data.contains(key)) {
            JSONArray convData = StrapMetrics.convAcclData(data);
            
            concatJSONArrays(tmpstore, convData);

            if(tmpstore.length() > min_readings) {

                
                query = "app_id=" + appID
                    + "&resolution=" + ((lp.getProperty("resolution").length() > 0) ?  lp.getProperty("resolution") : "")
                    + "&useragent=" + ((lp.getProperty("useragent").length() > 0) ?  lp.getProperty("useragent") : "")
                    + "&action_url=" + "STRAP_API_ACCL"
                    + "&visitor_id=" + serial
                    + "&visitor_timeoffset=" + tz_offset
                    + "&accl=" + URLEncoder.encode(tmpstore.toString(),"UTF-8")
//                   + "&act=" + ((tmpstore.length() > 0)?tmpstore[0].act:"UNKNOWN");
                    + "&act=UNKNOWN";

                //console.log('query: ' + query);

                

            	try {
            		Runnable r = new PostLog(strapURL,query);
                    new Thread(r).start();
                    
				} catch (Exception e) {
					e.printStackTrace();
				}
            	tmpstore = new JSONArray();
            }
            // removed else block referencing localstorage
        }
        else {
            
            query = "app_id=" +appID
            		 + "&resolution=" + ((lp.getProperty("resolution").length() > 0) ?  lp.getProperty("resolution") : "")
                     + "&useragent=" + ((lp.getProperty("useragent").length() > 0) ?  lp.getProperty("useragent") : "")
                     + "&visitor_id=" + serial
                     + "&visitor_timeoffset=" + tz_offset
                     + "&action_url=" + data.getString(KEY_OFFSET + T_LOG);

        	try {
        		Runnable r = new PostLog(strapURL,query);
                new Thread(r).start();
                
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
    }

    public static JSONArray convAcclData(PebbleDictionary data) throws JSONException {
        JSONArray convData = new JSONArray();
        
        int key = KEY_OFFSET + T_TIME_BASE;
        long time_base = Long.parseLong(data.getString(key));
        data.remove(key);
        
        for(int i = 0; i < strap_api_num_samples; i++) {
            int point = KEY_OFFSET + (10 * i);
        
            JSONObject ad = new JSONObject();
            // ts key
            key = point + T_TS;
            ad.put("ts", (data.getInteger(key) + time_base));
            data.remove(key);
            
            // x key
            key = point + T_X;
            ad.put("x", data.getInteger(key));
            data.remove(key);
            
            // y key
            key = point + T_Y;
            ad.put("y", data.getInteger(key));
            data.remove(key);
            
            // z key
            key = point + T_Z;
            ad.put("z", data.getInteger(key));
            data.remove(key);
            
            // did_vibrate key
            key = point + T_DID_VIBRATE;
            ad.put("vib", (data.getString(key) == "1")?true:false);
            data.remove(key);
            
            ad.put("act", data.getString(KEY_OFFSET + T_ACTIVITY));
            data.remove(key);
            
            convData.put(ad);
        }

        return convData;
    }
    

//    public void postLog() throws IOException {
//        
//    }
//    
}