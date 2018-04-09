package com.qmuiteam.qmuidemo;

import android.os.Bundle;

import com.qmuiteam.qmuidemo.base.BaseFragment;
import com.qmuiteam.qmuidemo.base.BaseFragmentActivity;
import com.qmuiteam.qmuidemo.fragment.home.HomeFragment;

public class QDMainActivity extends BaseFragmentActivity {

	@Override
	protected int contentViewId() {
		return R.id.qmuidemo;
	}

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			BaseFragment fragment = new HomeFragment();
			//TODO 启动fragment是否该更优雅一点？？设计一个接口？
			startFragment(fragment);
		}
	}
}
