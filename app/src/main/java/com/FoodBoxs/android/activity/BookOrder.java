package com.FoodBoxs.android.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.FoodBoxs.android.R;
import com.FoodBoxs.android.adapter.BookOrderAdapter;
import com.FoodBoxs.android.observableLayer.AppDatabase;
import com.FoodBoxs.android.observableLayer.Order;

import java.util.List;

public class BookOrder extends AppCompatActivity {

    private List<Order> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_order);
        //initializing Views
        initViews();
    }

    private void initViews() {
        //setting Background
        ImageView iv_background = findViewById(R.id.iv_background);
        Glide.with(this).load(R.drawable.ezgif).into(iv_background);

        ListView rv_listBookOrder = findViewById(R.id.rv_listBookOrder);
        TextView tv_itemName = findViewById(R.id.tv_itemName);
        TextView tv_noData = findViewById(R.id.tv_noData);
        tv_itemName.setTypeface(Home.tf_main_bold);


        data = AppDatabase.getAppDatabase(this).cartDao().getAllOrders();

        if (data != null && data.size() != 0) {
            BookOrderAdapter bookOrderAdapter = new BookOrderAdapter(data);
            rv_listBookOrder.setAdapter(bookOrderAdapter);
        }
        else tv_noData.setVisibility(View.VISIBLE);


        rv_listBookOrder.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startActivity(new Intent(BookOrder.this, OrderDetail.class).putExtra("orderId", data.get(position).getOrderId()));
            }
        });

    }
}
