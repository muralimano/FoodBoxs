package com.FoodBoxs.android.activity;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.FoodBoxs.android.R;
import com.FoodBoxs.android.activity.deliveryBoySection.DeliveryBoyHome;
import com.FoodBoxs.android.activity.deliveryBoySection.LoginAsDelivery;
import com.FoodBoxs.android.adapter.HomeCustomAdapter;
import com.FoodBoxs.android.getSet.MenuGetSet;
import com.FoodBoxs.android.getSet.SubMenuGetSet;
import com.FoodBoxs.android.observableLayer.CartListViewModel;
import com.FoodBoxs.android.observableLayer.HomeViewModel;
import com.FoodBoxs.android.utils.InternetDialog;
import com.FoodBoxs.android.utils.RegisterDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Home extends AppCompatActivity {
    private Spinner spinnerCategory;
    private ListView list_home;
    public static Typeface tf_main_regular, tf_main_bold, tf_main_italic;
    private DrawerLayout mDrawerLayout;
    private boolean mSlideState = false;
    private HomeViewModel homeViewModel;
    private List<MenuGetSet> menuData;
    private List<SubMenuGetSet> subMenuData;
    private CartListViewModel cartListViewModel;
    private TextView tv_noData;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //send user to delivery boy account if its sign in that account
        SharedPreferences prefs = getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE);
        Boolean isDeliveryAccountActive = prefs.getBoolean("isDeliverAccountActive", false);
        if (isDeliveryAccountActive) {
            Intent iv = new Intent(Home.this, DeliveryBoyHome.class);
            startActivity(iv);
            finish();
        }

        //set typeface to be used along the app
        globalTypeface();


        cartListViewModel = ViewModelProviders.of(Home.this).get(CartListViewModel.class);


        //initialize views
        initViews();

        if (!isNetworkConnected(this)) {
            InternetDialog internetDialog = new InternetDialog(Home.this, new InternetDialog.onRetry() {
                @Override
                public void onReload() {
                    //assigning view model class for services for this activity
                    homeViewModel = ViewModelProviders.of(Home.this).get(HomeViewModel.class);

                    //  if (getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE).getBoolean("isFirstTimeRegister", true))
                    //      registerUserWithServer(false);

                    //observing the data from the view model class
                    getDataFromServer();
                }
            });
            internetDialog.show();
        } else {
            //assigning view model class for services for this activity
            homeViewModel = ViewModelProviders.of(Home.this).get(HomeViewModel.class);

            //    if (getSharedPreferences(getString(R.string.shared_pref_name), MODE_PRIVATE).getBoolean("isFirstTimeRegister", true))
            //        registerUserWithServer(false);

            //observing the data from the view model class
            getDataFromServer();

        }

    }

    private void registerUserWithServer(Boolean isCancel) {
        RegisterDialog registerDialog = new RegisterDialog(Home.this, android.R.style.Theme_NoTitleBar_Fullscreen);
        registerDialog.setCancelable(isCancel);
        registerDialog.show();
    }

    private void globalTypeface() {
        tf_main_regular = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Regular.ttf");
        tf_main_bold = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Bold.ttf");
        tf_main_italic = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Italic.ttf");
    }

    private void initViews() {

        spinnerCategory = findViewById(R.id.spnr_cate);
        list_home = findViewById(R.id.list_home);
        ImageView imgGif = findViewById(R.id.imgGif);
        RelativeLayout rl_my_cart = findViewById(R.id.rl_my_cart);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        progressBar = findViewById(R.id.progressBar);
        tv_noData = findViewById(R.id.tv_noData);
        ImageView iv_addNumber = findViewById(R.id.iv_addNumber);

        Glide.with(Home.this).load(R.drawable.ezgif).into(imgGif);

        list_home.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(Home.this, DetailPage.class);
                ArrayList<String> itemData = new ArrayList<>();
                itemData.add(subMenuData.get(position).getCatId());
                itemData.add(subMenuData.get(position).getItemId());
                itemData.add(subMenuData.get(position).getItemName());
                itemData.add(subMenuData.get(position).getItemDescription());
                itemData.add(subMenuData.get(position).getItemPrice());
                itemData.add(subMenuData.get(position).getItemImage());
                i.putStringArrayListExtra("itemDetail", itemData);
                Log.e("TAG", "" + subMenuData.get(position).getItemName() + " Catid " + subMenuData.get(position).getCatId() + " itemID " + subMenuData.get(position).getItemId());
                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
            }
        });
        rl_my_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cartCount = cartListViewModel.getCartCount();

                if (cartCount > 0) {
                    Intent intent = new Intent(Home.this, CompleteOrder.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                } else {
                    Toast.makeText(Home.this, "Cart is empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        iv_addNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUserWithServer(true);
            }
        });


        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                swapProgress(true);
                homeViewModel.LoadSubMenu(menuData.get(position).getMenuId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setUpNavigationBar();
    }

    public void swapProgress(Boolean isVisible) {
        if (isVisible)
            progressBar.setVisibility(View.VISIBLE);
        else progressBar.setVisibility(View.GONE);
    }

    private void setUpNavigationBar() {

        //initialize navigation bar items
        TextView tv_home = findViewById(R.id.tv_home);
        tv_home.setTypeface(tf_main_regular);

        TextView tv_my_cart = findViewById(R.id.tv_my_cart);
        tv_my_cart.setTypeface(tf_main_regular);

        TextView tv_cart = findViewById(R.id.tv_cart);
        tv_cart.setTypeface(tf_main_bold);

        TextView tv_book_order = findViewById(R.id.tv_book_order);
        tv_book_order.setTypeface(tf_main_regular);

        TextView tv_favourite = findViewById(R.id.tv_favourite);
        tv_favourite.setTypeface(tf_main_regular);

        TextView tv_aboutUs = findViewById(R.id.tv_aboutUs);
        tv_aboutUs.setTypeface(tf_main_regular);

        TextView tv_ourPolicy = findViewById(R.id.tv_ourPolicy);
        tv_ourPolicy.setTypeface(tf_main_regular);

        TextView tv_share = findViewById(R.id.tv_share);
        tv_share.setTypeface(Home.tf_main_bold, Typeface.ITALIC);

        TextView tv_shareFB = findViewById(R.id.tv_shareFB);
        tv_shareFB.setTypeface(tf_main_regular);

        TextView tv_shareWA = findViewById(R.id.tv_shareWA);
        tv_shareWA.setTypeface(tf_main_regular);

        TextView tv_shareOther = findViewById(R.id.tv_shareOther);
        tv_home.setTypeface(tf_main_regular);

        Button btn_loginDeliveryBoy = findViewById(R.id.btn_loginDeliveryBoy);
        btn_loginDeliveryBoy.setTypeface(tf_main_regular);


        //common listener for navigation bar
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i;
                switch (v.getId()) {
                    case R.id.tv_home:
                        i = new Intent(Home.this, Home.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(i);
                        break;
                    case R.id.tv_my_cart:
                        int cartCount = cartListViewModel.getCartCount();

                        if (cartCount > 0) {
                            i = new Intent(Home.this, CompleteOrder.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            startActivity(i);
                        } else {
                            Toast.makeText(Home.this, R.string.empty_cart, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.tv_book_order:
                        i = new Intent(Home.this, BookOrder.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        break;
                    case R.id.tv_favourite:
                        i = new Intent(Home.this, Favourite.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        break;
                    case R.id.btn_loginDeliveryBoy:
                        i = new Intent(Home.this, LoginAsDelivery.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(i);
                        break;
                    case R.id.tv_shareFB:
                        shareToFacebook();
                        break;
                    case R.id.tv_shareWA:
                        shareToWhatsApp();
                        break;
                    case R.id.tv_shareOther:
                        shareToOther();
                        break;
                    case R.id.tv_aboutUs:
                        i = new Intent(Home.this, AboutUs.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(i);
                        break;
                    case R.id.tv_ourPolicy:
                        i = new Intent(Home.this, OurPolicy.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(i);
                        break;
                }
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                }
            }
        };

        tv_home.setOnClickListener(listener);
        tv_my_cart.setOnClickListener(listener);
        tv_book_order.setOnClickListener(listener);
        tv_favourite.setOnClickListener(listener);
        btn_loginDeliveryBoy.setOnClickListener(listener);
        tv_shareOther.setOnClickListener(listener);
        tv_shareFB.setOnClickListener(listener);
        tv_shareWA.setOnClickListener(listener);
        tv_aboutUs.setOnClickListener(listener);
        tv_ourPolicy.setOnClickListener(listener);
    }

    private void shareToOther() {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.download_app) + getPackageName());
        emailIntent.setType("text/plain");
        startActivity(Intent.createChooser(emailIntent, "Send to friend"));
    }

    private void shareToWhatsApp() {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        emailIntent.setPackage("com.whatsapp");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.download_app) + getPackageName());
        emailIntent.setType("text/plain");
        startActivity(Intent.createChooser(emailIntent, "Send to friend"));
    }

    private void shareToFacebook() {
        if (ShareDialog.canShow(ShareLinkContent.class)) {

            ShareLinkContent content = new ShareLinkContent.Builder()
                    .setContentUrl(Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()))
                    .setShareHashtag(new ShareHashtag.Builder()
                            .setHashtag("#" + getString(R.string.app_name)).build())
                    .build();
            ShareDialog shareDialog = new ShareDialog(Home.this);
            shareDialog.show(content, ShareDialog.Mode.AUTOMATIC);
        }
    }

    public void clickEventSlide(View view) {
        if (mSlideState) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
        mSlideState = !mSlideState;
    }

    private void getDataFromServer() {
        swapProgress(true);
        //getting and updating menu list
        homeViewModel.getMenuList().observe(this, new Observer<List<MenuGetSet>>() {
            @Override
            public void onChanged(@Nullable List<MenuGetSet> menuGetSets) {
                swapProgress(false);
                menuData = menuGetSets;
                if (menuData != null) {
                    homeViewModel.LoadSubMenu(menuData.get(0).getMenuId());
                }
                spinnerCategory.setAdapter(new MySpinnerAdapter(Home.this, R.layout.custom_spinner, menuData));
            }
        });
        //getting and updating subMenu items list
        homeViewModel.getSubMenuList().observe(this, new Observer<List<SubMenuGetSet>>() {
            @Override
            public void onChanged(@Nullable List<SubMenuGetSet> subMenuGetSets) {
                swapProgress(false);
                subMenuData = subMenuGetSets;
                HomeCustomAdapter lazyAdapter = new HomeCustomAdapter(Home.this, subMenuData);
                list_home.setAdapter(lazyAdapter);
                if (subMenuGetSets != null) {
                    if (subMenuGetSets.size() > 0) {
                        tv_noData.setVisibility(View.GONE);
                    } else {
                        tv_noData.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    class MySpinnerAdapter extends ArrayAdapter<String> {
        final ArrayList<MenuGetSet> data;

        MySpinnerAdapter(Context context, int custom_spinner, List menuGetSets) {
            super(context, custom_spinner, menuGetSets);
            data = (ArrayList<MenuGetSet>) menuGetSets;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, parent);
        }

        @NonNull
        @Override
        public View getView(int pos, View convertView, @NonNull ViewGroup parent) {
            return getCustomView(pos, parent);
        }

        View getCustomView(int position, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View mySpinner = inflater.inflate(R.layout.custom_spinner, parent, false);
            TextView tv_catName = mySpinner.findViewById(R.id.txt_spnr);
            tv_catName.setText(data.get(position).getCategoryName());
            tv_catName.setTypeface(tf_main_italic, Typeface.BOLD);
            ImageView iv_catImage = mySpinner.findViewById(R.id.spnr_img);
            String imageUrl = getString(R.string.link) + "images/menu_cat_icon/" + data.get(position).getCategoryImage();
            Glide.with(Home.this).load(imageUrl).into(iv_catImage);
            return mySpinner;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tv_numberOfItemCart = findViewById(R.id.tv_numberOfItemCart);
        tv_numberOfItemCart.setTypeface(tf_main_bold);
        final int cartCount = cartListViewModel.getCartCount();
        if (cartCount == 0)
            tv_numberOfItemCart.setText("");
        else
            tv_numberOfItemCart.setText(String.format(Locale.ENGLISH, "%d items", cartListViewModel.getCartCount()));
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null;
    }
}
