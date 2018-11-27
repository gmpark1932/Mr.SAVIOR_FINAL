package com.example.pgm.myapplication;

/**
 * Created by DowonYoon on 2017-06-21.
 */

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;



public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final static String naverSearchURL = "https://openapi.naver.com/v1/search/local.json";
    final static String naverClientID = "DRUQe9yF8wCGgKqgHlfY";
    final static String naverClientSecret = "OQam5I5tiW";


    private static final String TAG = "Main";
    Button btn_Insert;
    Button btn_send;
    Button btn_gps;
    EditText edit_Name;
    EditText edit_Phone;
    TextView text_Name;
    TextView text_Phone;
    TextView current_GPS;
    TextView search_result;

    TextView tv;
    ToggleButton tb;

    Button btn_next_page;

    long nowIndex;
    String name;
    long phone;
    String sort = "phone";

    ArrayAdapter<String> arrayAdapter;

    static ArrayList<String> arrayIndex =  new ArrayList<String>();
    static ArrayList<String> arrayData = new ArrayList<String>();
    private DbOpenHelper mDbOpenHelper;

    final Geocoder geocoder = new Geocoder(this);

    List<Address> current_address;

    public List<Address> getAddress(){
        return current_address;
    }

    public static Context mContext;

    public class RequestHttpURLConnection {

        public String request(String _url, ContentValues _params){

            // HttpURLConnection 참조 변수.
            HttpURLConnection urlConn = null;
            // URL 뒤에 붙여서 보낼 파라미터.
            StringBuffer sbParams = new StringBuffer();

            /**
             * 1. StringBuffer에 파라미터 연결
             * */
            // 보낼 데이터가 없으면 파라미터를 비운다.
            if (_params == null)
                sbParams.append("");
                // 보낼 데이터가 있으면 파라미터를 채운다.
            else {
                // 파라미터가 2개 이상이면 파라미터 연결에 &가 필요하므로 스위칭할 변수 생성.
                boolean isAnd = false;
                // 파라미터 키와 값.
                String key;
                String value;

                for(Map.Entry<String, Object> parameter : _params.valueSet()){
                    key = parameter.getKey();
                    value = parameter.getValue().toString();

                    // 파라미터가 두개 이상일때, 파라미터 사이에 &를 붙인다.
                    if (isAnd)
                        sbParams.append("&");

                    sbParams.append(key).append("=").append(value);

                    // 파라미터가 2개 이상이면 isAnd를 true로 바꾸고 다음 루프부터 &를 붙인다.
                    if (!isAnd)
                        if (_params.size() >= 2)
                            isAnd = true;
                }
            }

            /**
             * 2. HttpURLConnection을 통해 web의 데이터를 가져온다.
             * */
            try{
                URL url = new URL(_url);
                urlConn = (HttpURLConnection) url.openConnection();

                // [2-1]. urlConn 설정.
                urlConn.setRequestMethod("POST"); // URL 요청에 대한 메소드 설정 : POST.
                urlConn.setRequestProperty("Accept-Charset", "UTF-8"); // Accept-Charset 설정.
                urlConn.setRequestProperty("Context_Type", "application/x-www-form-urlencoded;cahrset=UTF-8");

                // [2-2]. parameter 전달 및 데이터 읽어오기.
                String strParams = sbParams.toString(); //sbParams에 정리한 파라미터들을 스트링으로 저장. 예)id=id1&pw=123;
                OutputStream os = urlConn.getOutputStream();
                os.write(strParams.getBytes("UTF-8")); // 출력 스트림에 출력.
                os.flush(); // 출력 스트림을 플러시(비운다)하고 버퍼링 된 모든 출력 바이트를 강제 실행.
                os.close(); // 출력 스트림을 닫고 모든 시스템 자원을 해제.

                // [2-3]. 연결 요청 확인.
                // 실패 시 null을 리턴하고 메서드를 종료.
                if (urlConn.getResponseCode() != HttpURLConnection.HTTP_OK)
                    return null;

                // [2-4]. 읽어온 결과물 리턴.
                // 요청한 URL의 출력물을 BufferedReader로 받는다.
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));

                // 출력물의 라인과 그 합에 대한 변수.
                String line;
                String page = "";

                // 라인을 받아와 합친다.
                while ((line = reader.readLine()) != null){
                    page += line;
                }

                return page;

            } catch (MalformedURLException e) { // for URL.
                e.printStackTrace();
            } catch (IOException e) { // for openConnection().
                e.printStackTrace();
            } finally {
                if (urlConn != null)
                    urlConn.disconnect();
            }

            return null;

        }

    }

    public class NetworkTask extends AsyncTask<Void, Void, String> {

        private String url;
        private ContentValues values;

        public NetworkTask(String url, ContentValues values) {

            this.url = url;
            this.values = values;
        }

        @Override
        protected String doInBackground(Void... params) {

            String result; // 요청 결과를 저장할 변수.
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();

            result = getHospitalList(0);

            //result = requestHttpURLConnection.request(url, values); // 해당 URL로 부터 결과물을 얻어온다.

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            //doInBackground()로 부터 리턴된 값이 onPostExecute()의 매개변수로 넘어오므로 s를 출력한다.
            search_result.setText(s);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        //GPS 권한 요청
        checkVerify();

        btn_Insert = (Button) findViewById(R.id.insert_button);
        btn_Insert.setOnClickListener(this);
        edit_Name = (EditText) findViewById(R.id.edit_name);
        edit_Phone = (EditText) findViewById(R.id.edit_phone);
        btn_send = (Button) findViewById(R.id.send_button);
        btn_send.setOnClickListener(this);
        current_GPS = (TextView)findViewById(R.id.current_gps);
        btn_gps = (Button) findViewById(R.id.gps_button);
        btn_gps.setOnClickListener(this);

        search_result = (TextView)findViewById(R.id.search_result);

        btn_next_page = (Button)findViewById(R.id.next_page_button);
        btn_next_page.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,HospitalActivity.class);
                startActivity(intent);
            }
        });



        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        ListView listView = (ListView) findViewById(R.id.db_list);
        listView.setAdapter(arrayAdapter);

        mDbOpenHelper = new DbOpenHelper(this);
        mDbOpenHelper.open();
        mDbOpenHelper.create();

        showDatabase(sort);

        //GPS
        tv = (TextView) findViewById(R.id.current_gps);
        tv.setText("위치정보 미수신중");

        tb = (ToggleButton)findViewById(R.id.gps_button);

        // LocationManager 객체를 얻어온다
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        tb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    if(tb.isChecked()){
                        tv.setText("수신중..");
                        // GPS 제공자의 정보가 바뀌면 콜백하도록 리스너 등록하기~!!!
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, // 등록할 위치제공자
                                100, // 통지사이의 최소 시간간격 (miliSecond)
                                1, // 통지사이의 최소 변경거리 (m)
                                mLocationListener);
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, // 등록할 위치제공자
                                100, // 통지사이의 최소 시간간격 (miliSecond)
                                1, // 통지사이의 최소 변경거리 (m)
                                mLocationListener);
                    }else{
                        tv.setText("위치정보 미수신중");
                        lm.removeUpdates(mLocationListener);  //  미수신할때는 반드시 자원해체를 해주어야 한다.
                    }
                }catch(SecurityException ex){
                }
            }
        });

        //GPS END

        btn_Insert.setEnabled(true);
        btn_send.setEnabled(true);
        btn_gps.setEnabled(true);
    }
    private final LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            //여기서 위치값이 갱신되면 이벤트가 발생한다.
            //값은 Location 형태로 리턴되며 좌표 출력 방법은 다음과 같다.

            Log.d("test", "onLocationChanged, location:" + location);
            double longitude = location.getLongitude(); //경도
            double latitude = location.getLatitude();   //위도
            double altitude = location.getAltitude();   //고도
            float accuracy = location.getAccuracy();    //정확도
            String provider = location.getProvider();   //위치제공자
            List<Address> list = null;
            String address = null;

            try {
                list = geocoder.getFromLocation(
                        latitude, // 위도
                        longitude, // 경도
                        10); // 얻어올 값의 개수
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("test", "입출력 오류 - 서버에서 주소변환시 에러발생");
            }
            if (list != null) {
                if (list.size()==0) {
                    address = "그런 주소는 없어요~";
                } else {
                    address = list.get(0).toString();
                }
            }

            current_address = list;

            tv.setText("위치정보 : " + provider + "\n위도 : " + longitude + "\n경도 : " + latitude
                    + "\n고도 : " + altitude + "\n정확도 : " + accuracy
            +"\n주소 : " + list.get(0).getAddressLine(0));
            ContentValues cv = new ContentValues();
            cv.put("X-Naver-Client-Id",naverClientID);
            cv.put("X-Naver-Client-Secret", naverClientSecret);

            NetworkTask networkTask = new NetworkTask(naverSearchURL, cv);
            networkTask.execute();

            //getHospitalList(0);
        }

        public void onProviderDisabled(String provider) {
            // Disabled시
            Log.d("test", "onProviderDisabled, provider:" + provider);
        }

        public void onProviderEnabled(String provider) {
            // Enabled시
            Log.d("test", "onProviderEnabled, provider:" + provider);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // 변경시
            Log.d("test", "onStatusChanged, provider:" + provider + ", status:" + status + " ,Bundle:" + extras);
        }
    };

    public void checkVerify()
    {
        if (
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
                )
        {
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION))
            {
                // ...
            }
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.INTERNET},
                    1);
        }
        else
        {
            //startApp();
        }
    }

    public void setInsertMode(){
        edit_Name.setText("");
        edit_Phone.setText("");
        btn_Insert.setEnabled(true);
        btn_send.setEnabled(true);
        btn_gps.setEnabled(true);
    }

    public void showDatabase(String sort){
        Cursor iCursor = mDbOpenHelper.sortColumn(sort);
        Log.d("showDatabase", "DB Size: " + iCursor.getCount());
        arrayData.clear();
        arrayIndex.clear();
        while(iCursor.moveToNext()){
            String tempIndex = iCursor.getString(iCursor.getColumnIndex("_id"));
            String tempName = iCursor.getString(iCursor.getColumnIndex("name"));
            tempName = setTextLength(tempName,10);
            String tempPhone = iCursor.getString(iCursor.getColumnIndex("phone"));
            tempPhone = setTextLength(tempPhone,10);

            String Result = tempName + tempPhone;
            arrayData.add(Result);
            arrayIndex.add(tempIndex);
        }
        arrayAdapter.clear();
        arrayAdapter.addAll(arrayData);
        arrayAdapter.notifyDataSetChanged();
    }

    public String setTextLength(String text, int length){
        if(text.length()<length){
            int gap = length - text.length();
            for (int i=0; i<gap; i++){
                text = text + " ";
            }
        }
        return text;
    }

    private void sendSMS(String phoneNumber, String message)
    {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.insert_button:
                name = edit_Name.getText().toString();
                phone = Long.parseLong(edit_Phone.getText().toString());
                mDbOpenHelper.open();
                mDbOpenHelper.insertColumn(name, phone);
                showDatabase(sort);
                setInsertMode();
                break;
            case R.id.send_button:
                Cursor iCursor = mDbOpenHelper.sortColumn(sort);
                Log.d("showDatabase", "DB Size: " + iCursor.getCount());
                arrayData.clear();
                arrayIndex.clear();
                while(iCursor.moveToNext()){
                    String tempIndex = iCursor.getString(iCursor.getColumnIndex("_id"));
                    String tempName = iCursor.getString(iCursor.getColumnIndex("name"));
                    tempName = setTextLength(tempName,10);
                    String tempPhone = iCursor.getString(iCursor.getColumnIndex("phone"));
                    tempPhone = setTextLength(tempPhone,10);

                    //문자 보내기 필요하다.
                    String emergency_message = "위급상황입니다.";
                    sendSMS("01050411932",emergency_message);
                }
                arrayAdapter.clear();
                arrayAdapter.addAll(arrayData);
                arrayAdapter.notifyDataSetChanged();
                break;
        }
    }


    public String getHospitalList(int count){
        List<Address> address_list = ((MainActivity)MainActivity.mContext).getAddress();
        String address = address_list.get(0).getThoroughfare();
        String result = new String();
        try {
            String text = URLEncoder.encode(address + " 응급실", "UTF-8");
            String apiURL = "https://openapi.naver.com/v1/search/local.xml?query="+ text; // json 결과
            //String apiURL = "https://openapi.naver.com/v1/search/blog.xml?query="+ text; // xml 결과
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-Naver-Client-Id", naverClientID);
            con.setRequestProperty("X-Naver-Client-Secret", naverClientSecret);
            int responseCode = con.getResponseCode();
            BufferedReader br;
            if(responseCode==200) { // 정상 호출
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {  // 에러 발생
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            System.out.println(response.toString());
            result = response.toString();
            //search_result.setText(response.toString());
        } catch (Exception e) {
            System.out.println(e);
        }

        return result;
    }
}
