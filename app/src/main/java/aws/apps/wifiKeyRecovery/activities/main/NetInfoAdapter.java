/**
 * ****************************************************************************
 * Copyright 2011 Alexandros Schillings
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package aws.apps.wifiKeyRecovery.activities.main;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import aws.apps.wifiKeyRecovery.R;
import uk.co.alt236.wifipasswordaccess.container.WifiNetworkInfo;
import uk.co.alt236.wifipasswordaccess.container.WifiProtectedNetworkInfo;

/*package*/ class NetInfoAdapter extends BaseAdapter implements Filterable {
    private final int COLOR_RED = Color.parseColor("#F44336");
    private final int COLOR_ORANGE = Color.parseColor("#FFC107");
    private final int COLOR_GREEN = Color.parseColor("#4CAF50");

    private final Map<String, Integer> mAlphaIndexer;
    private final LayoutInflater mInflater;
    private final List<WifiNetworkInfo> mAllItems;
    private List<WifiNetworkInfo> mSubItems;
    private String[] mSections;
    private Filter mFilter;

    public NetInfoAdapter(final Context context, final List<WifiNetworkInfo> appsList) {
        super();
        mSubItems = appsList;
        mAllItems = this.mSubItems;

        mAlphaIndexer = new HashMap<>();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        prepareIndexer();
    }

    @Override
    public int getCount() {
        return mSubItems.size();
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ProoferFilter();
        }
        return mFilter;
    }

    @Override
    public WifiNetworkInfo getItem(final int position) {
        return mSubItems.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    public int getPositionForSection(final int section) {
        return mAlphaIndexer.get(mSections[section]);
    }

    public int getSectionForPosition(final int position) {
        return 0;
    }

    public Object[] getSections() {
        return mSections;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final WifiNetworkInfo netInfo = mSubItems.get(position);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_network_info, null);
            convertView.setTag(new ViewHolder(convertView));
        }

        ((ViewHolder) convertView.getTag()).populate(netInfo);

        return convertView;
    }

    private void prepareIndexer() {
        final int size = mSubItems.size();
        String title;
        String c;

        for (int i = size - 1; i >= 0; i--) {
            title = mSubItems.get(i).getSsid();

            try {
                // This will throw an exception if the value is not a number.
                //noinspection ResultOfMethodCallIgnored
                Integer.valueOf(title.substring(0, 1));
                c = "#";
            } catch (final NumberFormatException e) {
                c = title.toUpperCase(Locale.US).substring(0, 1);
            }

            mAlphaIndexer.put(c, i);
        }

        final Set<String> keys = mAlphaIndexer.keySet();
        final Iterator<String> it = keys.iterator();
        final List<String> keyList = new ArrayList<>();

        while (it.hasNext()) {
            keyList.add(it.next());
        }

        Collections.sort(keyList);

        mSections = new String[keyList.size()];
        keyList.toArray(mSections);
    }

    /**
     * Custom Filter implementation for the items adapter.
     */
    private class ProoferFilter extends Filter {

        @Override
        protected FilterResults performFiltering(final CharSequence filterString) {
            // NOTE: this function is *always* called from a background thread,
            // and not the UI thread.

            final FilterResults results = new FilterResults();
            final List<WifiNetworkInfo> i = new ArrayList<>();

            if (!TextUtils.isEmpty(filterString)) {

                for (int index = 0; index < mAllItems.size(); index++) {
                    final WifiNetworkInfo item = mAllItems.get(index);
                    if (item.getSsid().toLowerCase(Locale.US).contains(filterString.toString().toLowerCase())) {
                        i.add(item);
                    }
                }

                results.values = i;
                results.count = i.size();
            } else {
                synchronized (mAllItems) {
                    results.values = mAllItems;
                    results.count = mAllItems.size();
                }
            }

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(final CharSequence prefix, final FilterResults results) {
            // NOTE: this function is *always* called from the UI thread.
            mSubItems = (ArrayList<WifiNetworkInfo>) results.values;

            notifyDataSetChanged();
        }
    }

    protected class ViewHolder {
        final TextView ssid;
        final TextView additional;
        final ImageView icon;

        public ViewHolder(final View root) {
            ssid = (TextView) root.findViewById(R.id.ssid);
            additional = (TextView) root.findViewById(R.id.additional);
            icon = (ImageView) root.findViewById(R.id.icon);
        }

        private String formatAdditionalInfo(final WifiNetworkInfo netInfo) {
            if(netInfo instanceof WifiProtectedNetworkInfo){
                return ((WifiProtectedNetworkInfo) netInfo).getPassword();
            } else {
                return "Open network";
            }
        }

        public void populate(final WifiNetworkInfo netInfo) {
            ssid.setText(netInfo.getSsid());
            additional.setText(formatAdditionalInfo(netInfo));
            setIcon(netInfo);
        }

        private void setIcon(final WifiNetworkInfo netInfo) {
            if (netInfo instanceof WifiProtectedNetworkInfo) {
                icon.setImageResource(R.drawable.ic_list_wifi_protected);
            } else {
                icon.setImageResource(R.drawable.ic_list_wifi_open);
            }

            final int color;
            switch (netInfo.getNetType()) {

                case UNKNOWN:
                    color = COLOR_RED;
                    break;
                case NO_ENCRYPTION:
                    color = COLOR_RED;
                    break;
                case WEP:
                    color = COLOR_ORANGE;
                    break;
                case WPA:
                    color = COLOR_GREEN;
                    break;
                default:
                    color = COLOR_RED;
                    break;
            }

            icon.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        }
    }
}