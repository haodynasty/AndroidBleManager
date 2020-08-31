/*
 * FileName: FragmentPagerAdapter.java
 * Copyright (C) 2014 Plusub Tech. Co. Ltd. All Rights Reserved <admin@plusub.com>
 * 
 * Licensed under the Plusub License, Version 1.0 (the "License");
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * author  : service@plusub.com
 * date     : 2015-4-16 下午3:06:55
 * last modify author :
 * version : 1.0
 */
package com.blakequ.androidblemanager.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager+Fragment的适配器
 * <br>在使用的时候注意几个问题：
 * <br>1.让ViewPager不自动销毁，一直保持在内存setOffscreenPageLimit(保持的个数)
 * <br>2.禁止ViewPager滑动，使用库ScrollViewPager调用setLocked方法(此时使用setCurrentItem切换)
 * @ClassName: FragmentPagerAdapter
 * @Description: TODO
 * @author qh@plusub.com
 * @date： 
 *     <b>文件创建时间：</b>2015-4-16 下午3:06:55<br>
 *     <b>最后修改时间：</b>2015-4-16 下午3:06:55
 * @version v1.0
 */
public class FragmentPageAdapter extends FragmentPagerAdapter {

	private List<Fragment> list = null;
	private String[] title;
	
	public FragmentPageAdapter(FragmentManager fm, List<Fragment> list) {
		super(fm);
		// TODO Auto-generated constructor stub
		if (list == null) {
			this.list = new ArrayList<Fragment>();
		}
		this.list = list;
	}

	/**
	 * 设置页面title
	 * <p>Title: setPageTitle
	 * <p>Description: 
	 * @param title
	 */
	public void setPageTitle(String[] title){
		this.title = title;
	}
	
	public void setData(ArrayList<Fragment> list){
		this.list = list;
	}
	
	public void addData(Fragment fragment){
		this.list.add(fragment);
	}
	
	public void addDataHead(Fragment fragment){
		this.list.add(0, fragment);
	}

	@Override
	public Fragment getItem(int arg0) {
		// TODO Auto-generated method stub
		return list.get(arg0);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return list.size();
	}

	@Override
	public CharSequence getPageTitle(int position) {
		// TODO Auto-generated method stub
		if (title != null && title.length > position) {
			return title[position];
		}
		return super.getPageTitle(position);
	}
}
