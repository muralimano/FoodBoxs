package com.FoodBoxs.android.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.FoodBoxs.android.R;
import com.FoodBoxs.android.getSet.cityGetSet;
import com.FoodBoxs.android.observableLayer.AppDatabase;
import com.FoodBoxs.android.observableLayer.CartListViewModel;
import com.FoodBoxs.android.observableLayer.Order;
import com.FoodBoxs.android.utils.GPSTracker;
import com.FoodBoxs.android.utils.InternetDialog;
import com.FoodBoxs.android.utils.WorkaroundMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class PlaceOrderInfo extends FragmentActivity implements OnMapReadyCallback {

    private List<cityGetSet> cityList;
    private static GoogleMap mMap;
    private final String[] permission_location = {android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private static double latitudeCurrent;
    private static double longitudeCurrent;
    private final int PERMISSION_REQUEST_CODE = 1001;
    private static String address = "";
    private Boolean isCameraMoved;
    private CartListViewModel cartListViewModel;
    private String totalPrice;
    private static ProgressDialog pd;
    private EditText et_fullName, et_address, et_email, et_phoneNumber, et_notes;
    private RadioGroup rg_paymentType;
    private AppCompatSpinner spinner_select_city;
    private Activity activity;
    private String cartOrderDetail = "";
    private RadioButton rb_cash;
    private RadioButton rb_card;
    private ScrollView sv_inputs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order_info);

        //assign view model of database for the activity
        cartListViewModel = ViewModelProviders.of(this).get(CartListViewModel.class);

        getIntents();

        //initializing Views
        initViews();

    }

    private void getIntents() {
        cartOrderDetail = getIntent().getStringExtra("CartDetail");
        totalPrice = getIntent().getStringExtra("CartTotalPrice");
    }

    private void initViews() {
        activity = PlaceOrderInfo.this;
        ImageView iv_background = findViewById(R.id.iv_background);
        Glide.with(this).load(R.drawable.ezgif).into(iv_background);

        TextView tv_itemName = findViewById(R.id.tv_itemName);
        tv_itemName.setTypeface(Home.tf_main_bold);
        sv_inputs = findViewById(R.id.sv_inputs);


        rb_cash = findViewById(R.id.rb_cash);
        rb_card = findViewById(R.id.rb_card);
        et_fullName = findViewById(R.id.et_fullName);
        et_address = findViewById(R.id.et_address);
        et_email = findViewById(R.id.et_email);
        et_phoneNumber = findViewById(R.id.et_phoneNumber);
        et_phoneNumber.setText(getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE).getString("registeredNumber", ""));
        et_notes = findViewById(R.id.et_notes);

        et_address.setTypeface(Home.tf_main_bold);
        et_email.setTypeface(Home.tf_main_bold);
        et_fullName.setTypeface(Home.tf_main_bold);
        et_phoneNumber.setTypeface(Home.tf_main_bold);
        et_notes.setTypeface(Home.tf_main_bold);
        rb_cash.setTypeface(Home.tf_main_bold);
        rb_card.setTypeface(Home.tf_main_bold);
        ((TextView) findViewById(R.id.tv_paymentType)).setTypeface(Home.tf_main_bold);


        rg_paymentType = findViewById(R.id.rg_paymentType);
        spinner_select_city = findViewById(R.id.spinner_select_city);


        if (Home.isNetworkConnected(this))
            getCities();
        else {
            InternetDialog dialog = new InternetDialog(PlaceOrderInfo.this, new InternetDialog.onRetry() {
                @Override
                public void onReload() {
                    getCities();
                }
            });
            dialog.show();
        }


        //initialize google map
        if (checkPermission()) {
            gettingLocation();
            MapsInitializer.initialize(getApplicationContext());
            WorkaroundMapFragment mapFragment = (WorkaroundMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
            mapFragment.setListener(new WorkaroundMapFragment.OnTouchListener() {
                @Override
                public void onTouch() {
                    sv_inputs.requestDisallowInterceptTouchEvent(true);
                }
            });
        } else requestPermission();


        findViewById(R.id.rl_CompleteOrder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityAfterValidation();
            }
        });

    }

    private void getCities() {
        String url = getString(R.string.link) + "api/get_city.php";
        Log.d(getLocalClassName(), url);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Log.d(getLocalClassName(), response);

                    handleResponse(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(6000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        stringRequest.setShouldCache(false);
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);

    }

    private void handleResponse(String response) throws JSONException {
        JSONObject jsonObject = new JSONObject(response);

        JSONObject jsonData = jsonObject.optJSONObject("data");
        if (jsonData.optString("success").equals("1")) {

            JSONArray jo_array = jsonData.getJSONArray("city");
            if (jo_array.length() > 0) {
                cityList = new ArrayList<>();
                for (int i = 0; i < jo_array.length(); i++) {
                    cityGetSet tem = new cityGetSet();
                    JSONObject jo = jo_array.optJSONObject(i);
                    tem.setCityName(jo.optString("city_name"));
                    tem.setCityId(jo.optString("id"));
                    cityList.add(tem);
                }

            }
            if (cityList.size() > 0) {
                cityGetSet temp = new cityGetSet();
                temp.setCityName("Select city");
                cityList.add(temp);

                String[] cities = new String[cityList.size()];
                for (int i = 0; i < cityList.size(); i++) {
                    cities[i] = cityList.get(i).getCityName();
                }
                CustomSpinnerAdapter customSpinnerAdapter = new CustomSpinnerAdapter(PlaceOrderInfo.this, R.layout.custom_spinner_delivery, cities);
                spinner_select_city.setAdapter(customSpinnerAdapter);
                spinner_select_city.setSelection(spinner_select_city.getCount());

            }
        }

    }

    private void startActivityAfterValidation() {
        //validation fields
        if (spinner_select_city.getSelectedItemPosition() != spinner_select_city.getCount()) {
            if (isValid(et_fullName)) {
                if (isValid(et_address)) {
                    if (emailValidator(et_email)) {
                        if (isValid(et_phoneNumber)) {
                            if (isValid(et_notes)) {
                                if (rg_paymentType.getCheckedRadioButtonId() != -1) {
                                    String paymentType = "";
                                    if (rg_paymentType.getCheckedRadioButtonId() == rb_cash.getId())
                                        paymentType = rb_cash.getText().toString();
                                    else if (rg_paymentType.getCheckedRadioButtonId() == rb_card.getId())
                                        paymentType = rb_card.getText().toString();
                                    if (Home.isNetworkConnected(PlaceOrderInfo.this))
                                        postData(getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE).getInt("userId", -1), (String) spinner_select_city.getSelectedItem(), et_fullName.getText().toString(), et_address.getText().toString(), et_email.getText().toString(), et_phoneNumber.getText().toString(), et_notes.getText().toString(), paymentType, cartOrderDetail);
                                    else {
                                        final String finalPaymentType = paymentType;
                                        InternetDialog dialog = new InternetDialog(PlaceOrderInfo.this, new InternetDialog.onRetry() {
                                            @Override
                                            public void onReload() {
                                                postData(getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE).getInt("userId", -1), (String) spinner_select_city.getSelectedItem(), et_fullName.getText().toString(), et_address.getText().toString(), et_email.getText().toString(), et_phoneNumber.getText().toString(), et_notes.getText().toString(), finalPaymentType, cartOrderDetail);
                                            }
                                        });
                                        dialog.show();

                                    }
                                } else {
                                    Toast.makeText(PlaceOrderInfo.this, R.string.error_payment, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                et_notes.setError(getString(R.string.error_notes));
                                et_notes.requestFocus();
                            }
                        } else {
                            et_phoneNumber.setError(getString(R.string.error_number));
                            et_phoneNumber.requestFocus();
                        }
                    } else {
                        et_email.setError(getString(R.string.error_email));
                        et_email.requestFocus();
                    }
                } else {
                    et_address.setError(getString(R.string.error_address));
                    et_address.requestFocus();

                }
            } else {
                et_fullName.requestFocus();
                et_fullName.setError(getString(R.string.error_name));
            }
        } else {
            Toast.makeText(PlaceOrderInfo.this, R.string.error_city, Toast.LENGTH_SHORT).show();
        }
    }

    private void postData(final int userId, final String selectedCity, final String selectedName, final String selectedAddress, final String selectedEmail, String selectedNumber, final String selectedNotes, final String selectedPaymentType, final String selectedOrder) {

        Log.w("userId", userId + "");
        Log.w("selectedCity", selectedCity);
        Log.w("selectedName", selectedName);
        Log.w("selectedAddress", selectedAddress);
        Log.w("selectedEmail", selectedEmail);
        Log.w("selectedNumber", selectedNumber);
        Log.w("selectedPaymentType", selectedPaymentType);
        Log.w("selectedOrder", selectedOrder);
        Log.w("totalPrice", totalPrice);
        final String token_id = getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE).getString("token", "");
        Log.w("token_id", token_id);
        Log.w("device_type", "android");

        String hp = getString(R.string.link) + "api/food_order.php";
        StringRequest postRequest = new StringRequest(Request.Method.POST, hp, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // response
                Log.d("Response", response);
                try {

                    Intent i = new Intent(PlaceOrderInfo.this, OrderDetail.class);
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject.getString("success").equals("Order Book Successfully")) {

                        cartListViewModel.emptyCart();
                        cartListViewModel.emptyCartTopping();
                        Toast.makeText(PlaceOrderInfo.this, jsonObject.getString("order_details"), Toast.LENGTH_SHORT).show();
                        int orderId = jsonObject.getInt("order_id");
                        i.putExtra("orderId", orderId);
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(i);
                        //save successful order in database
                        //create order object
                        Order order = new Order();
                        order.setTimestamp(getTimeStamp());
                        order.setOrderId(orderId);
                        order.setOrderPrice(Double.parseDouble(totalPrice));
                        order.setAddress(selectedAddress);

                        AppDatabase appDatabase = AppDatabase.getAppDatabase(PlaceOrderInfo.this);
                        new addOrderDetailToDatabase(appDatabase).execute(order);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error.toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                params.put("name", selectedName);
                params.put("email", selectedEmail);
                params.put("address", selectedAddress);
                params.put("latlong", String.valueOf(latitudeCurrent) + "," + String.valueOf(longitudeCurrent));
                params.put("payment_type", selectedPaymentType);
                params.put("city", selectedCity);
                params.put("notes", selectedNotes);
                params.put("food_desc", selectedOrder);
                params.put("total_price", totalPrice);
                params.put("token_id", token_id);
                params.put("device_type", "android");
                return params;
            }

        };
        postRequest.setShouldCache(false);
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(postRequest);

    }

    private String getTimeStamp() {
        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss", Locale.ENGLISH);
        return df.format(c);
    }

    private void gettingLocation() {
        GPSTracker gps = new GPSTracker();
        gps.init(PlaceOrderInfo.this);
        // check if GPS enabled
        if (gps.canGetLocation()) {
            try {
                latitudeCurrent = gps.getLatitude();
                longitudeCurrent = gps.getLongitude();
            } catch (NumberFormatException e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        } else {
            gps.showSettingsAlert();
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, permission_location, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.no_map), Toast.LENGTH_SHORT).show();
                return;
            }
            mMap = googleMap;

            mMap.getUiSettings().setCompassEnabled(false);
            mMap.getUiSettings().setRotateGesturesEnabled(false);
            mMap.getUiSettings().setTiltGesturesEnabled(false);
            mMap.getUiSettings().setZoomControlsEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.setTrafficEnabled(false);
            mMap.setIndoorEnabled(false);
            mMap.setBuildingsEnabled(false);
            if (checkPermission())
                mMap.setMyLocationEnabled(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {

            isCameraMoved = false;

            LatLng position = new LatLng(latitudeCurrent, longitudeCurrent);
            latitudeCurrent = position.latitude;
            longitudeCurrent = position.longitude;
            if (Home.isNetworkConnected(PlaceOrderInfo.this))
                new GetDataAsyncTask(activity).execute();
            else new InternetDialog(PlaceOrderInfo.this, new InternetDialog.onRetry() {
                @Override
                public void onReload() {
                    new GetDataAsyncTask(activity).execute();
                }
            }).show();


            mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                    //get latlng at the center by calling
                    LatLng midLatLng = mMap.getCameraPosition().target;
                    latitudeCurrent = midLatLng.latitude;
                    longitudeCurrent = midLatLng.longitude;
                    if (isCameraMoved) {
                        if (Home.isNetworkConnected(PlaceOrderInfo.this)) {
                            new GetDataAsyncTask(activity).execute();
                            isCameraMoved = !isCameraMoved;
                        } else
                            new InternetDialog(PlaceOrderInfo.this, new InternetDialog.onRetry() {
                                @Override
                                public void onReload() {
                                    new GetDataAsyncTask(activity).execute();
                                    isCameraMoved = !isCameraMoved;
                                }
                            }).show();

                    }
                }

            });
            mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                @Override
                public void onCameraMove() {
                    isCameraMoved = true;
                }
            });

        } catch (NullPointerException | NumberFormatException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    gettingLocation();

                    MapsInitializer.initialize(getApplicationContext());
                    WorkaroundMapFragment mapFragment = (WorkaroundMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(this);
                    mapFragment.setListener(new WorkaroundMapFragment.OnTouchListener() {
                        @Override
                        public void onTouch() {
                            sv_inputs.requestDisallowInterceptTouchEvent(true);
                        }
                    });

                } else requestPermission();
            }
        }
    }

    class CustomSpinnerAdapter extends ArrayAdapter<String> {
        final String[] data;

        CustomSpinnerAdapter(@NonNull Context context, int resource, @NonNull String[] objects) {
            super(context, resource, objects);
            data = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView text = view.findViewById(R.id.txt_spnr);
            text.setTypeface(Home.tf_main_bold);
            return view;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return super.getDropDownView(position, convertView, parent);
        }

        public int getCount() {
            int count = super.getCount();
            return count > 0 ? count - 1 : count;

        }
    }


    private static class GetDataAsyncTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<Activity> weakActivity;

        GetDataAsyncTask(Activity activity) {
            this.weakActivity = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = ProgressDialog.show(weakActivity.get(), "", weakActivity.get().getString(R.string.txt_load));
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Activity activity = weakActivity.get();
                if (getAddress(weakActivity.get()) != null)
                    address = getAddress(activity).get(0).getAddressLine(0);
            } catch (NullPointerException e) {
                e.printStackTrace();
                throw e;
            }catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                throw e;
            }catch (IndexOutOfBoundsException e){
                e.printStackTrace();
                throw e;
            }
            finally {
                if (address.isEmpty())
                    address="";
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Activity activity = weakActivity.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                // activity is no longer valid, don't do anything!
                return;
            }

            if (pd.isShowing()) {
                pd.dismiss();
            }
            LatLng position = new LatLng(latitudeCurrent, longitudeCurrent);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17.0f));
            ((EditText) (activity.findViewById(R.id.et_address))).setText(address);
        }

    }

    private static List<Address> getAddress(Activity activity) {
        if (latitudeCurrent != 0 && longitudeCurrent != 0) {
            try {
                Geocoder geocoder = new Geocoder(activity);
                List<Address> addresses = geocoder.getFromLocation(latitudeCurrent, longitudeCurrent, 1);
                String address = addresses.get(0).getAddressLine(0);
                String city = addresses.get(0).getAddressLine(1);
                String country = addresses.get(0).getAddressLine(2);
                Log.d("TAG", "address = " + address + ", city = " + city + ", country = " + country);
                return addresses;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<Address> temp = new ArrayList<>();
        return temp;
    }

    private boolean isValid(EditText editText) {
        return editText.length() != 0;
    }

    private boolean emailValidator(EditText editText) {
        String email = editText.getText().toString();
        Pattern pattern;
        Matcher matcher;
        final String EMAIL_PATTERN = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        pattern = Pattern.compile(EMAIL_PATTERN);
        matcher = pattern.matcher(email);
        return matcher.matches();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pd != null)
            pd.dismiss();
    }

    private static class addOrderDetailToDatabase extends AsyncTask<Order, Void, Void> {

        final AppDatabase appDatabase;

        addOrderDetailToDatabase(AppDatabase appDatabase1) {
            this.appDatabase = appDatabase1;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Order... order) {
            appDatabase.cartDao().insertOrder(order[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }


    }
}
