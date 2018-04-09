package com.qmuiteam.qmui.arch;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import com.qmuiteam.qmui.util.QMUILogger;
import com.qmuiteam.qmui.util.QMUIStatusBarHelper;
import com.qmuiteam.qmui.widget.QMUIWindowInsetLayout;

/**
 * 基础的 Activity，配合 {@link QMUIFragment} 使用。
 * TODO 考虑将 {@link AppCompatActivity} v7,替换成 v4 {@link android.support.v4.app.FragmentActivity}
 * Created by cgspine on 15/9/14.
 */
public abstract class QMUIFragmentActivity extends AppCompatActivity {
    private static final String TAG = "QMUIFragmentActivity";
    private QMUIWindowInsetLayout mFragmentContainer;

    /**
     * Fragment装置需要的id，该id是基布局{@linkplain #mFragmentContainer}
     *
     * @return 返回id
     */
    protected abstract @IdRes
    int contentViewId();

    /**
     * Check if need translucent status bar,default is true
     *
     * @return translucent
     */
    protected boolean translucent() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO 考虑做成注解配置
        if (translucent()) {
            QMUIStatusBarHelper.translucent(this);
        }
        //InsertLayout处理布局占用系统区域问题
        mFragmentContainer = new QMUIWindowInsetLayout(this);
        //固定ID
        mFragmentContainer.setId(contentViewId());
        //默认用insert layout作为基布局
        setContentView(mFragmentContainer);
    }

    public FrameLayout getFragmentContainer() {
        return mFragmentContainer;
    }

    @Override
    public void onBackPressed() {
        QMUIFragment fragment = getCurrentFragment();
        if (fragment != null) {
            fragment.popBackStack();
        }
    }

    /**
     * 获取当前的 Fragment。
     */
    public QMUIFragment getCurrentFragment() {
        return (QMUIFragment) getSupportFragmentManager().findFragmentById(contentViewId());
    }

    /**
     * 提供基类方法，用于自定义统一启动fragment的各种设置
     * 如:动画统一，回退栈处理等
     *
     * @param fragment 需要状态的fragment
     */
    public void startFragment(QMUIFragment fragment) {
        QMUILogger.i(TAG, "startFragment");
        QMUIFragment.TransitionConfig transitionConfig = fragment.obtainTransitionConfig();
        String tagName = fragment.getClass().getSimpleName();
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(transitionConfig.enter, transitionConfig.exit, transitionConfig.popenter, transitionConfig.popout)
                .replace(contentViewId(), fragment, tagName)
                .addToBackStack(tagName)
                .commitAllowingStateLoss();//in case the wrong state cause exceptions
    }

    /**
     * Exit the current Fragment。
     */
    public void popBackStack() {
        QMUILogger.i(TAG, "popBackStack: getSupportFragmentManager().getBackStackEntryCount() = " + getSupportFragmentManager().getBackStackEntryCount());
        if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
            QMUIFragment fragment = getCurrentFragment();
            //no fragment exists
            if (fragment == null) {
                finish();
                return;
            }
            QMUIFragment.TransitionConfig transitionConfig = fragment.obtainTransitionConfig();
            //when reach the last finished fragment,we handle by different return value
            finish();
            overridePendingTransition(transitionConfig.popenter, transitionConfig.popout);
        } else {//More than one fragment in this activity,just pop back stack immediately
            getSupportFragmentManager().popBackStackImmediate();
        }
    }

    /**
     * <pre>
     * 返回到clazz类型的Fragment，
     * 如 A --> B --> C，
     * popBackStack(A.class),it's A(Fragment) now.
     *
     * 如果堆栈没有clazz或者就是当前的clazz（如上例的popBackStack(C.class)），就相当于popBackStack()
     * </pre>
     */
    public void popBackStack(Class<? extends QMUIFragment> clazz) {
        getSupportFragmentManager().popBackStack(clazz.getSimpleName(), 0);
    }

    /**
     * <pre>
     * 返回到非clazz类型的Fragment
     *
     * 如果上一个是目标clazz，则会继续pop，直到上一个不是clazz。
     * 此api理论上用的比较少.
     * </pre>
     */
    public void popBackStackInclusive(Class<? extends QMUIFragment> clazz) {
        getSupportFragmentManager().popBackStack(clazz.getSimpleName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
}