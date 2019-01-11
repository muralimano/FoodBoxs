package com.FoodBoxs.android.activity;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.FoodBoxs.android.R;
import com.FoodBoxs.android.adapter.CustomAdapter;
import com.FoodBoxs.android.observableLayer.CartFav;
import com.FoodBoxs.android.observableLayer.CartListViewModel;

import java.util.ArrayList;
import java.util.List;

public class Favourite extends AppCompatActivity {
    private RecyclerView rv_listFavourite;
    private List<CartFav> data;
    private CartListViewModel cartListViewModel;
    private TextView tv_noData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourite);

        //assign view model of database for the activity
        cartListViewModel = ViewModelProviders.of(this).get(CartListViewModel.class);
        //initializing Views
        initViews();
    }

    private void initViews() {
        //setting Background
        ImageView iv_background = findViewById(R.id.iv_background);
        tv_noData = findViewById(R.id.tv_noData);
        Glide.with(this).load(R.drawable.ezgif).into(iv_background);

        rv_listFavourite = findViewById(R.id.rv_listFavourite);
        TextView tv_itemName = findViewById(R.id.tv_itemName);
        tv_itemName.setTypeface(Home.tf_main_bold);


        cartListViewModel.getFavCartList().observe(this, new Observer<List<CartFav>>() {
            @Override
            public void onChanged(@Nullable List<CartFav> carts) {
                data = carts;
                updateUI(data);
            }
        });
    }

    private void updateUI(final List<CartFav> carts) {

        CustomAdapter customAdapter = new CustomAdapter(carts, new CustomAdapter.onClickListen() {
            @Override
            public void onItemClick(View v, int position) {
                Intent i = new Intent(Favourite.this, DetailPage.class);
                ArrayList<String> itemData = new ArrayList<>();
                itemData.add(String.valueOf(carts.get(position).getItemCategoryId()));
                itemData.add(String.valueOf(carts.get(position).getItemId()));
                itemData.add(carts.get(position).getItemName());
                itemData.add(carts.get(position).getItemDescription());
                itemData.add(String.valueOf(carts.get(position).getItemPrice()));
                itemData.add(carts.get(position).getItemImageId());
                i.putStringArrayListExtra("itemDetail", itemData);
                startActivity(i);
            }
        });
        rv_listFavourite.setAdapter(customAdapter);

        if (data.size() > 0) {
            tv_noData.setVisibility(View.GONE);
        } else {
            tv_noData.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

