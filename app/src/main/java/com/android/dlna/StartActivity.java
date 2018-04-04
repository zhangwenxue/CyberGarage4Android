package com.android.dlna;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class StartActivity extends AppCompatActivity {
    private static final String[] URLS = {"http://mvvideo10.meitudata.com/5785a7fc4fc2a8429.mp4", "http://live.hkstv.hk.lxdns.com/live/hks/playlist.m3u8"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        UrlAdapter adapter = new UrlAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                chooseDevice(URLS[position]);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_control, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSubmitButtonEnabled(true);//显示提交按钮
        searchView.setQueryHint("输入视频完整地址");
        SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(R.id.search_src_text);
        searchAutoComplete.setHintTextColor(getResources().getColor(android.R.color.white));//设置提示文字颜色
        searchAutoComplete.setTextColor(getResources().getColor(android.R.color.white));//设置内容文字颜色
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (TextUtils.isEmpty(query) || !query.contains(".") || query.indexOf(".") == query.length() - 1) {
                    Toast.makeText(StartActivity.this, "请检查视频地址是否正确", Toast.LENGTH_SHORT).show();
                    return false;
                } else {
                    chooseDevice(query);
                }
                //提交按钮的点击事件
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //当输入框内容改变的时候回调
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private class UrlAdapter extends RecyclerView.Adapter<UrlAdapter.Holder> {
        private OnItemClickListener mListener;

        @Override
        public UrlAdapter.Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new UrlAdapter.Holder(LayoutInflater.from(StartActivity.this).inflate(R.layout.list_item_device, parent, false));
        }

        @Override
        public void onBindViewHolder(final UrlAdapter.Holder holder, int position) {
            String url = URLS[position];
            int pos = url.lastIndexOf(".");
            String format = "";
            if (pos >= 0 && pos < url.length()) {
                format = url.substring(pos, url.length());
            }

            holder.name.setText(String.format("视频%d%s", position, format));
            holder.contentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onItemClick(v, holder.getAdapterPosition());
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return URLS.length;
        }

        void setOnItemClickListener(OnItemClickListener mListener) {
            this.mListener = mListener;
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView name;
            View contentView;

            Holder(View itemView) {
                super(itemView);
                contentView = itemView;
                name = itemView.findViewById(R.id.tv_name_item);
            }
        }
    }

    private void chooseDevice(String url) {
        Intent intent = new Intent(this, ControlPointActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
    }
}
