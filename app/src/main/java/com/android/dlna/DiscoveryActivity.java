package com.android.dlna;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.dlna.manager.DLNADeviceManager;

import org.cybergarage.upnp.Device;

import java.util.ArrayList;
import java.util.List;

import static com.android.dlna.manager.DLNADeviceManager.getInstance;

public class DiscoveryActivity extends AppCompatActivity {
    private SwipeRefreshLayout mRefreshLayout;
    private View mEmptyView;
    private List<Device> mDeviceList = new ArrayList<>();
    private DeviceAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mRefreshLayout = findViewById(R.id.refresh_layout);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        mEmptyView = findViewById(R.id.empty_view);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mAdapter = new DeviceAdapter();
        recyclerView.setAdapter(mAdapter);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                startDiscovery(false);
            }
        });
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                setResult(RESULT_OK);
                getInstance().setCurrentDevice(mDeviceList.get(position));
                finish();
            }
        });
    }

    private void startDiscovery(boolean auto) {
        if (auto) {
            mRefreshLayout.setRefreshing(true);
        }
        getInstance().startDiscovery(mListener);
    }

    private final DLNADeviceManager.MediaRenderDeviceChangeListener mListener = new DLNADeviceManager.MediaRenderDeviceChangeListener() {
        @Override
        public void onStarted() {
            updateEmptyView(false);
        }

        @Override
        public void onDeviceListChanged(List<Device> list) {
            mDeviceList = list;
            updateEmptyView(mDeviceList.isEmpty());
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onFinished() {
            mRefreshLayout.setRefreshing(false);
            updateEmptyView(mDeviceList.isEmpty());
        }
    };

    private void updateEmptyView(boolean visible) {
        if (visible && mEmptyView.getVisibility() != View.VISIBLE) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else if (!visible && mEmptyView.getVisibility() == View.VISIBLE) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    private void stopDiscovery() {
        getInstance().stopDiscovery();
        mRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDeviceList.isEmpty()) {
            stopDiscovery();
            startDiscovery(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopDiscovery();
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    private class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.Holder> {
        private OnItemClickListener mListener;

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(DiscoveryActivity.this).inflate(R.layout.list_item_device, parent, false));
        }

        @Override
        public void onBindViewHolder(final Holder holder, int position) {
            Device device = mDeviceList.get(position);
            holder.deviceName.setText(device.getFriendlyName());
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
            return mDeviceList.size();
        }

        public void setOnItemClickListener(OnItemClickListener mListener) {
            this.mListener = mListener;
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView deviceName;
            View contentView;

            Holder(View itemView) {
                super(itemView);
                contentView = itemView;
                deviceName = itemView.findViewById(R.id.tv_name_item);
            }
        }
    }
}
