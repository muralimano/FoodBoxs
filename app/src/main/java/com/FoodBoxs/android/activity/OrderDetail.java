package com.FoodBoxs.android.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.FoodBoxs.android.R;
import com.FoodBoxs.android.adapter.CustomOrderDetailAdapter;
import com.FoodBoxs.android.getSet.OrderDetailGetSet;
import com.FoodBoxs.android.observableLayer.Cart;
import com.FoodBoxs.android.observableLayer.CartItemTopping;
import com.FoodBoxs.android.observableLayer.CombinedCart;
import com.FoodBoxs.android.timeline.OrderStatus;
import com.FoodBoxs.android.timeline.TimeLineAdapter;
import com.FoodBoxs.android.timeline.TimeLineModel;
import com.FoodBoxs.android.utils.InternetDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetail extends AppCompatActivity {
    private RecyclerView rv_timeline;
    private RecyclerView rv_orderedItems;
    private int orderId;
    private ProgressBar progressBar;
    RelativeLayout rl_CancelOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        gettingIntents();

        //initializing Views
        initViews();

        //getting data
        if (Home.isNetworkConnected(OrderDetail.this))
            getDataFromServer(orderId);
        else new InternetDialog(OrderDetail.this, new InternetDialog.onRetry() {
            @Override
            public void onReload() {
                getDataFromServer(orderId);
            }
        }).show();
    }


    private void getDataFromServer(final int orderId) {
        progressBar.setVisibility(View.VISIBLE);
        String hp = String.format(Locale.ENGLISH, "%sapi/order_details.php?order_id=%d", getString(R.string.link), orderId);
        Log.e(getLocalClassName(), hp);
        StringRequest postRequest = new StringRequest(Request.Method.GET, hp, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // response
                Log.d("Response", response);
                progressBar.setVisibility(View.GONE);
                try {
                    handleResponse(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
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
                params.put("order_id", String.valueOf(orderId));
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(postRequest);
    }


    private void handleResponse(String response) throws JSONException {
        JSONObject jsonObject = new JSONObject(response);
        String text_success = jsonObject.getString("success");
        if (text_success.equals("1")) {
            JSONArray ja_orderDetail = jsonObject.getJSONArray("order_details");
            JSONObject jo_orderDetail = ja_orderDetail.getJSONObject(0);
            int numberOfItems = jo_orderDetail.optInt("item_order");
            String orderAddress = jo_orderDetail.optString("address");
            String orderContactNumber = jo_orderDetail.optString("contact");
            Double orderTotalPrice = jo_orderDetail.optDouble("total_price");
            String orderDate = jo_orderDetail.optString("order_placed_date");
            JSONArray ja_subList = jo_orderDetail.getJSONArray("menu");

            //////////////////create main data/////////////////////////////

            OrderDetailGetSet orderDetailGetSet = new OrderDetailGetSet();
            orderDetailGetSet.setNumberOfItems(String.valueOf(numberOfItems));
            orderDetailGetSet.setOrderDate(orderDate);
            orderDetailGetSet.setOrderDeliveryAddress(orderAddress);
            orderDetailGetSet.setOrderDeliveryPhone(orderContactNumber);
            orderDetailGetSet.setOrderTotalAmount(String.valueOf(orderTotalPrice));

            ////////////////////create sublist data///////////////////////

            ArrayList<CombinedCart> combinedCart = new ArrayList<>();
            ArrayList<CartItemTopping> toppings = null;
            for (int i = 0; i < ja_subList.length(); i++) {
                CombinedCart tempCart = new CombinedCart();
                JSONObject dat = ja_subList.getJSONObject(i);
                Cart temp = new Cart();
                temp.setItemId(dat.optInt("ItemId"));
                temp.setItemPrice(dat.optDouble("ItemAmt"));
                temp.setItemQuantity(dat.optInt("ItemQty"));
                temp.setItemName(dat.optString("ItemName"));
                JSONArray ja_Ingredients = dat.optJSONArray("Ingredients");
                if (ja_Ingredients.length() > 0) {
                    toppings = new ArrayList<>();
                    for (int j = 0; j < ja_Ingredients.length(); j++) {
                        JSONObject sub_dat = ja_Ingredients.getJSONObject(j);
                        CartItemTopping topping = new CartItemTopping();
                        topping.setToppingId(sub_dat.optInt("id"));
                        topping.setToppingName(sub_dat.optString("item_name"));
                        topping.setToppingPrice(sub_dat.optDouble("price"));
                        topping.setItemId(sub_dat.optInt("menu_id"));
                        toppings.add(topping);
                    }
                }
                tempCart.setCart(temp);
                tempCart.setItemToppings(toppings);
                combinedCart.add(tempCart);
            }
            CustomOrderDetailAdapter customAdapter = new CustomOrderDetailAdapter(combinedCart, orderDetailGetSet, OrderDetail.this);
            rv_orderedItems.setAdapter(customAdapter);

            //////////////////////////updating TimeLine///////////////////////
            List<TimeLineModel> timeLineModels = new ArrayList<>();

            TimeLineModel timeLineModel;
            //////stage1

            String place_status = jo_orderDetail.optString("place_status");
            if (place_status.equals("Activate")) {
                timeLineModel = new TimeLineModel(jo_orderDetail.optString("order_placed_date"), OrderStatus.ACTIVE);
            } else {
                timeLineModel = new TimeLineModel("", OrderStatus.INACTIVE);
            }
            timeLineModels.add(timeLineModel);

            //////stage2////
            /* Check whether the order is cancelled by the user, if not the continue in else block  */

            String order_status = jo_orderDetail.optString("order_status");

            if (order_status.equals("Cancelled")) {
                rl_CancelOrder.setVisibility(View.GONE);
                timeLineModel = new TimeLineModel("", OrderStatus.INACTIVE);
                timeLineModels.add(timeLineModel);

                timeLineModel = new TimeLineModel("", OrderStatus.INACTIVE);
                timeLineModels.add(timeLineModel);

                timeLineModel = new TimeLineModel(jo_orderDetail.optString("cancel_date_time"), OrderStatus.CANCELLED);

                timeLineModels.add(timeLineModel);

            } else {
                String preparing_status = jo_orderDetail.optString("preparing_status");
                if (preparing_status.equals("Activate")) {
                    timeLineModel = new TimeLineModel(jo_orderDetail.optString("preparing_date_time"), OrderStatus.ACTIVE);
                    rl_CancelOrder.setVisibility(View.GONE);
                } else {
                    timeLineModel = new TimeLineModel("", OrderStatus.INACTIVE);
                }
                timeLineModels.add(timeLineModel);

                /////stage3
                String dispatched_status = jo_orderDetail.optString("dispatched_status");
                if (dispatched_status.equals("Activate")) {
                    rl_CancelOrder.setVisibility(View.GONE);
                    timeLineModel = new TimeLineModel(jo_orderDetail.optString("dispatched_date_time"), OrderStatus.ACTIVE);
                } else {
                    timeLineModel = new TimeLineModel("", OrderStatus.INACTIVE);
                }
                timeLineModels.add(timeLineModel);


                /////stage4
                String delivered_status = jo_orderDetail.optString("delivered_status");
                if (delivered_status.equals("Activate")) {
                    rl_CancelOrder.setVisibility(View.GONE);
                    timeLineModel = new TimeLineModel(jo_orderDetail.optString("delivered_date_time"), OrderStatus.ACTIVE);
                } else {
                    timeLineModel = new TimeLineModel("", OrderStatus.INACTIVE);
                }
                timeLineModels.add(timeLineModel);
            }
            TimeLineAdapter timeLineAdapter = new TimeLineAdapter(timeLineModels);
            rv_timeline.setAdapter(timeLineAdapter);
        }
    }


    private void gettingIntents() {
        orderId = getIntent().getIntExtra("orderId", -1);
    }


    private void initViews() {
        rv_timeline = findViewById(R.id.rv_timeline);
        rl_CancelOrder = findViewById(R.id.rl_CancelOrder);
        rv_orderedItems = findViewById(R.id.rv_orderedItems);
        TextView tv_itemName = findViewById(R.id.tv_itemName);
        progressBar = findViewById(R.id.progressBar);
        tv_itemName.setText(String.format(Locale.ENGLISH, "Order No %d", orderId));

        ImageView iv_background = findViewById(R.id.iv_background);
        Glide.with(this).load(R.drawable.ezgif).into(iv_background);

        tv_itemName.setTypeface(Home.tf_main_bold);

        rl_CancelOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Home.isNetworkConnected(OrderDetail.this))
                    cancelOrder(orderId);
                else new InternetDialog(OrderDetail.this, new InternetDialog.onRetry() {
                    @Override
                    public void onReload() {
                        cancelOrder(orderId);
                    }
                }).show();
            }
        });
    }


    private void cancelOrder(int orderId) {
        progressBar.setVisibility(View.VISIBLE);
        String url = String.format(Locale.ENGLISH, "%sapi/order_cancel.php?order_id=%d", getString(R.string.link), orderId);
        Log.e(getLocalClassName(), url);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                progressBar.setVisibility(View.GONE);
                Log.e(getLocalClassName(), response);
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String isSuccess = jsonObject.optString("success");
                    if (isSuccess.equals("1")) {
                        Toast.makeText(OrderDetail.this, jsonObject.optString("order"), Toast.LENGTH_SHORT).show();
                        rl_CancelOrder.setVisibility(View.GONE);
                        onBackPressed();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(OrderDetail.this, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
            }
        });

        stringRequest.setShouldCache(false);

        RequestQueue requestQueue = Volley.newRequestQueue(this);

        requestQueue.add(stringRequest);
    }
}
