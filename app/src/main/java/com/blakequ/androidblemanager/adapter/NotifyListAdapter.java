package com.blakequ.androidblemanager.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.blakequ.androidblemanager.R;


/**
 * Copyright (C) BlakeQu All Rights Reserved <blakequ@gmail.com>
 * <p/>
 * Licensed under the blakequ.com License, Version 1.0 (the "License");
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * author  : quhao <blakequ@gmail.com> <br>
 * date     : 2016/8/24 20:44 <br>
 * last modify author : <br>
 * version : 1.0 <br>
 * description:
 */
public class NotifyListAdapter extends BaseArrayListAdapter<String>{

    public NotifyListAdapter(Context context) {
        super(context);
    }

    @Override
    public void addHead(String datas) {
        int size = getCount();
        if (getCount() >= 10){
            delete(size - 1);
        }
        super.addHead(datas);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflater.inflate(R.layout.list_item_one, null);
            viewHolder = new ViewHolder();
            viewHolder.descValue = (TextView) view.findViewById(R.id.tv_item1);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.descValue.setText((String)getItem(position));
        return view;
    }

    static class ViewHolder {
        TextView descValue;
    }
}
