package com.example.zhan.qiche;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.MapView;
import com.example.zhan.qiche.Activities.BaseActivity;
import com.example.zhan.qiche.Activities.ChatActivity;
import com.example.zhan.qiche.Beans.User;
import com.example.zhan.qiche.Fragment.HomeFragment;
import com.example.zhan.qiche.Fragment.MineFragment;
import com.example.zhan.qiche.Internet.OKHttp;
import com.example.zhan.qiche.Net.Net;
import com.example.zhan.qiche.Server.Dao.UserDao;
import com.example.zhan.qiche.Server.DaoImp.UserDaoImp;
import com.example.zhan.qiche.Util.SPUtils;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.EaseUI;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.ui.EaseConversationListFragment;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.hyphenate.easeui.utils.EaseUserUtils.getUserInfo;


public class MainActivity extends BaseActivity implements View.OnClickListener{
    //UserDao
    UserDao userDao = new UserDaoImp(this);
    //三个碎片
    private MineFragment mineFragment;
    private HomeFragment homeFragment;
    private EaseConversationListFragment conversationListFragment;

    //底部Fragment的LinearLayout
    @BindView(R.id.home_ll)LinearLayout home_ll;
    @BindView(R.id.message_ll)LinearLayout message_ll;
    @BindView(R.id.mine_ll)LinearLayout mine_ll;

    //底部Fragment的LinearLayout中对应的image
    @BindView(R.id.home_lv)ImageView home_lv;
    @BindView(R.id.message_lv)ImageView message_lv;
    @BindView(R.id.mine_lv)ImageView mine_lv;

    //底部Fragment的LinearLayout中对应的TextView
    @BindView(R.id.home_tv)TextView home_tv;
    @BindView(R.id.mine_tv)TextView mine_tv;

    List<User>  userList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userDao.getUser();
        initEvent();//点击事件
        select(1);
    }
    private void initEvent(){
        home_ll.setOnClickListener(this);
        mine_ll.setOnClickListener(this);
        message_ll.setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        resetImg();
        switch (v.getId()){
            case R.id.home_ll:
                home_lv.setImageResource(R.drawable.click_home);//使得图标显目
                home_tv.setTextColor(Color.parseColor("#77c6e2"));
                select(1);
                break;
            case R.id.message_ll:
                select(2);
                break;
            case R.id.mine_ll:
                mine_lv.setImageResource(R.drawable.click_mine);
                mine_tv.setTextColor(Color.parseColor("#77c6e2"));
                select(3);
                break;
        }
    }

    public void select(int i){
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        hidenFragment(fragmentTransaction);//隐藏所有的Fragment
        switch (i){
            case 1:
                if (homeFragment==null){
                    homeFragment = new HomeFragment();
                    fragmentTransaction.add(R.id.main_fg,homeFragment);
                }else{
                    fragmentTransaction.show(homeFragment);
                }
                break;
            case 2:
                getList();
                EaseUI.getInstance().setUserProfileProvider(new EaseUI.EaseUserProfileProvider() {
                    @Override
                    public EaseUser getUser(String username) {
                        for (int i =0;i<userList.size();i++){
                            if (username.equals(userList.get(i).getUserPhone())){
                                EaseUser easeUser = new EaseUser(userList.get(i).getUserPhone());
                                easeUser.setNickname(userList.get(i).getUserNickName());
                                try {
                                    easeUser.setAvatar(userList.get(i).getUserPicture());
                                }catch (Exception e){}
                                return easeUser;
                            }
                        }
                        return null;
                    }
                });
                if (conversationListFragment == null){
                    conversationListFragment = new EaseConversationListFragment();
                    conversationListFragment.setConversationListItemClickListener(new com.hyphenate.easeui.ui.EaseConversationListFragment.EaseConversationListItemClickListener() {

                        @Override
                        public void onListItemClicked(EMConversation conversation) {
                            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                            intent.putExtra("Id",conversation.conversationId());
                            startActivity(intent);
                        }
                    });
                    getSupportFragmentManager().beginTransaction().add(R.id.main_fg, conversationListFragment).commit();

                }else {

                    conversationListFragment.setConversationListItemClickListener(new com.hyphenate.easeui.ui.EaseConversationListFragment.EaseConversationListItemClickListener() {

                        @Override
                        public void onListItemClicked(EMConversation conversation) {
                            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                            intent.putExtra("Id",conversation.conversationId());
                            startActivity(intent);
                        }
                    });
                    getSupportFragmentManager().beginTransaction().show(conversationListFragment).commit();
                }
                break;
            case 3:
                if (mineFragment==null){
                    mineFragment = new MineFragment();
                    fragmentTransaction.add(R.id.main_fg,mineFragment);
                }else{
                    fragmentTransaction.show(mineFragment);
                }
                break;
        }
        fragmentTransaction.commit();
    }
   private void getList(){
        try {
                    Map<String, EMConversation> conversations = EMClient.getInstance().chatManager().getAllConversations();
                    final List<String> list = new ArrayList<>();
                    for (EMConversation e:conversations.values()){
                        list.add(e.conversationId());
                    }
                    for (int i = 0;i<list.size();i++){
                        String URl = Net.GetUser +"?UserPhone="+list.get(i);
                        OKHttp.sendOkhttpGetRequest(URl, new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Toast.makeText(MainActivity.this,"无法访问服务器",Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                 User user = parseJSONtoGetUser(response.body().string());
                                 userList.add(user);
                            }
                        });
                    }

                }catch (Exception e){ }
   }
    //获取用户所有信息JSON
    private User parseJSONtoGetUser(String responseData){
        User user = new User();
        try {
            JSONObject jsonObject =new JSONObject(responseData);
            JSONObject jsonArray = jsonObject.getJSONObject("FirstLogin");
            String UserNickName = jsonArray.getString("UserNickName");
            String UserPhone = jsonArray.getString("UserPhoneNumber");
            String UserSex = jsonArray.getString("UserSex");
            String UserPicture = jsonArray.getString("UserPicture");
            user.setUserPhone(UserPhone);
            user.setUserNickName(UserNickName);
            user.setUserPicture(UserPicture);
            user.setUserSex(UserSex);
        }catch (JSONException e){
        }
        return user;
    }
    //隐藏所有Fragment
    private void hidenFragment(FragmentTransaction tf) {
        if (homeFragment != null)
        {
            tf.hide(homeFragment);
        }
        if (conversationListFragment != null)
        {
            getSupportFragmentManager().beginTransaction().hide(conversationListFragment).commit();
        }
        if (mineFragment != null)
        {
            tf.hide(mineFragment);
        }
    }
    //重置所有图标
    private void resetImg(){
        //重置图标
        home_lv.setImageResource(R.drawable.home);
        mine_lv.setImageResource(R.drawable.mine);
        //重置文字颜色
        home_tv.setTextColor(Color.parseColor("#272727"));
        mine_tv.setTextColor(Color.parseColor("#272727"));
    }

   public  void getUsercallback(User user){
        String NickName="";
        String Id = user.getUserPhone();
        String Picture="";
        String Sex = user.getUserSex();
        if (user.getUserNickName() != "null"){
            NickName=user.getUserNickName();
        }else {
            NickName = user.getUserPhone();
        }
        if (user.getUserPicture()!= "null"){
            Picture = user.getUserPicture();
        }else {
            Picture = "";
        }
        MyApplication.setUserSex(Sex);
        MyApplication.setUserNickName(NickName);
        MyApplication.setUserPicture(Picture);
   }
}
