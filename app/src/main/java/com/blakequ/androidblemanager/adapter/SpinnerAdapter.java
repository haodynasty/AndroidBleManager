package com.blakequ.androidblemanager.adapter;
/*
******************************* Copyright (c)*********************************\
**
**                 (c) Copyright 2016-2017 All Rights Reserved
**
**                           By(成都凡米科技有限公司)
**                         
**-----------------------------------版本信息------------------------------------
** 版    本: V0.1
** 时    间：2017-10-20 17:59 PLUSUB
**------------------------------------------------------------------------------
********************************End of Head************************************\
*/

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.blakequ.androidblemanager.ConstValue;
import com.blakequ.androidblemanager.R;
import java.util.Set;

public class SpinnerAdapter extends BaseArrayListAdapter<SpinnerAdapter.Record>  {

  public SpinnerAdapter(Context context) {
    super(context);
    initData();
  }

  private void initData(){
    Set<Integer> keys = ConstValue.RECORD_MAP.keySet();
    add(new Record(-1, "NONE"));
    for (Integer value : keys){
      add(new Record(value, ConstValue.RECORD_MAP.get(value)));
    }
  }

  public int getPositionByValue(int value){
    int count = getCount();
    for (int i=0; i<count; i++){
      Record record = (Record) getItem(i);
      if (record.id == value) return i;
    }
    return 0;
  }

  @Override
  public View getView(int i, View view, ViewGroup viewGroup) {
    final ViewHolder viewHolder;
    // General ListView optimization code.
    if (view == null) {
      view = mInflater.inflate(R.layout.list_item_one, null);
      viewHolder = new ViewHolder();
      viewHolder.recordName = (TextView) view.findViewById(R.id.tv_item1);
      view.setTag(viewHolder);
    }else {
      viewHolder = (ViewHolder) view.getTag();
    }
    Record record = (Record) getItem(i);
    viewHolder.recordName.setText(record.name);
    return view;
  }

  static class ViewHolder {
    TextView recordName;
  }

  public static class Record{
    public int id;
    public String name;

    public Record(int id, String name){
      this.id = id;
      this.name = name;
    }
  }
}
