package com.qmuiteam.qmui.arch;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.qmuiteam.qmui.util.QMUILogger;
import com.qmuiteam.qmui.util.QMUIViewHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 基础 Fragment 类，提供各种基础功能。
 * Created by cgspine on 15/9/14.
 */
public abstract class QMUIFragment extends Fragment {
    private static final String SWIPE_BACK_VIEW = "swipe_back_view";
    private static final String TAG = QMUIFragment.class.getSimpleName();

    /**
     * Edge flag indicating that the left edge should be affected.
     */
    public static final int EDGE_LEFT = SwipeBackLayout.EDGE_LEFT;

    /**
     * Edge flag indicating that the right edge should be affected.
     */
    public static final int EDGE_RIGHT = SwipeBackLayout.EDGE_RIGHT;

    /**
     * Edge flag indicating that the bottom edge should be affected.
     */
    public static final int EDGE_BOTTOM = SwipeBackLayout.EDGE_BOTTOM;

    // === 提供两种默认的进入退出动画 ===
    protected static final TransitionConfig SLIDE_TRANSITION_CONFIG = new TransitionConfig(
            R.anim.slide_in_right, R.anim.slide_out_left,
            R.anim.slide_in_left, R.anim.slide_out_right);

    protected static final TransitionConfig SCALE_TRANSITION_CONFIG = new TransitionConfig(
            R.anim.scale_enter, R.anim.slide_still,
            R.anim.slide_still, R.anim.scale_exit);


    //Transition animation status(not start,start,end...)
    public static final int ANIMATION_ENTER_STATUS_NOT_START = -1;
    public static final int ANIMATION_ENTER_STATUS_STARTED = 0;
    public static final int ANIMATION_ENTER_STATUS_END = 1;

    private View mBaseView;
    private SwipeBackLayout mCachedSwipeBackView;
    private boolean mIsCreateForSwipeBack = false;
    private int mBackStackIndex = 0;

    private int mEnterAnimationStatus = ANIMATION_ENTER_STATUS_NOT_START;
    private boolean mCalled = true;
    /**
     * List of render action runnable,handle this after transition animation done
     */
    private ArrayList<Runnable> mDelayRenderRunnableList = new ArrayList<>();

    public QMUIFragment() {
        super();
    }

    public final QMUIFragmentActivity getBaseFragmentActivity() {
        FragmentActivity activity = getActivity();
        return activity != null ? (QMUIFragmentActivity) activity : null;
    }

    public boolean isAttachedToActivity() {
        return !isRemoving() && mBaseView != null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBaseView = null;
    }

    /**
     * Start a fragment that extends {@link QMUIFragment} after check current state
     * through {@link QMUIFragmentActivity#startFragment(QMUIFragment)}
     *
     * @param fragment pending start fragment
     */
    protected void startFragment(QMUIFragment fragment) {
        QMUIFragmentActivity baseFragmentActivity = this.getBaseFragmentActivity();
        if (baseFragmentActivity != null) {
            if (this.isAttachedToActivity()) {
                baseFragmentActivity.startFragment(fragment);
            } else {
                QMUILogger.e("QMUIFragment", "fragment not attached:" + this);
            }
        } else {
            QMUILogger.e("QMUIFragment", "startFragment null:" + this);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            int backStackEntryCount = fragmentManager.getBackStackEntryCount();
            for (int i = backStackEntryCount - 1; i >= 0; i--) {
                FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(i);
                if (getClass().getSimpleName().equals(entry.getName())) {
                    mBackStackIndex = i;
                    break;
                }
            }
        }
    }

    private SwipeBackLayout newSwipeBackLayout() {
        View rootView = onCreateView();
        //fit system window or not
        rootView.setFitsSystemWindows(obtainsFitsSystemWindows());
        final SwipeBackLayout swipeBackLayout = SwipeBackLayout.wrap(rootView, dragBackEdge());
        swipeBackLayout.setEnableGesture(false);
        if (canDragBack()) {
            //enable gesture after animation is done.
            runAfterAnimation(new Runnable() {
                @Override
                public void run() {
                    swipeBackLayout.setEnableGesture(true);
                }
            }, true);
        }
        swipeBackLayout.addSwipeListener(new SwipeBackLayout.SwipeListener() {
            @Override
            public void onScrollStateChange(int state, float scrollPercent) {
                QMUILogger.i(TAG, "SwipeListener:onScrollStateChange: state = " + state + " ;scrollPercent = " + scrollPercent);
                if (getBaseFragmentActivity() == null) return;
                //activity base container..
                ViewGroup container = getBaseFragmentActivity().getFragmentContainer();
                int childCount = container.getChildCount();
                if (state == SwipeBackLayout.STATE_IDLE) {
                    if (scrollPercent <= 0.0F) {
                        for (int i = childCount - 1; i >= 0; i--) {
                            View view = container.getChildAt(i);
                            Object tag = view.getTag(R.id.qmui_arch_swipe_layout_in_back);
                            if (tag != null && SWIPE_BACK_VIEW.equals(tag)) {
                                container.removeView(view);
                            }
                        }
                    } else if (scrollPercent >= 1.0F) {
                        for (int i = childCount - 1; i >= 0; i--) {
                            View view = container.getChildAt(i);
                            Object tag = view.getTag(R.id.qmui_arch_swipe_layout_in_back);
                            if (tag != null && SWIPE_BACK_VIEW.equals(tag)) {
                                container.removeView(view);
                            }
                        }
                        FragmentManager fragmentManager = getFragmentManager();
                        if (fragmentManager == null) {
                            return;
                        }
                        int backstackCount = fragmentManager.getBackStackEntryCount();
                        if (backstackCount > 0) {
                            try {
                                FragmentManager.BackStackEntry backStackEntry = fragmentManager.getBackStackEntryAt(backstackCount - 1);

                                Field opsField = backStackEntry.getClass().getDeclaredField("mOps");
                                opsField.setAccessible(true);
                                Object opsObj = opsField.get(backStackEntry);
                                if (opsObj instanceof List<?>) {
                                    List<?> ops = (List<?>) opsObj;
                                    for (Object op : ops) {
                                        Field cmdField = op.getClass().getDeclaredField("cmd");
                                        cmdField.setAccessible(true);
                                        int cmd = (int) cmdField.get(op);
                                        if (cmd == 1) {//Add -> Remove
                                            Field popEnterAnimField = op.getClass().getDeclaredField("popExitAnim");
                                            popEnterAnimField.setAccessible(true);
                                            popEnterAnimField.set(op, 0);
                                        }
                                    }
                                }
                            } catch (NoSuchFieldException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        popBackStack();
                    }
                }
            }

            @Override
            public void onScroll(int edgeFlag, float scrollPercent) {
                int targetOffset = (int) (Math.abs(backViewInitOffset()) * (1 - scrollPercent));
                if (getBaseFragmentActivity() == null) return;
                ViewGroup container = getBaseFragmentActivity().getFragmentContainer();
                int childCount = container.getChildCount();
                for (int i = childCount - 1; i >= 0; i--) {
                    View view = container.getChildAt(i);
                    Object tag = view.getTag(R.id.qmui_arch_swipe_layout_in_back);
                    if (tag != null && SWIPE_BACK_VIEW.equals(tag)) {
                        if (edgeFlag == EDGE_BOTTOM) {
                            ViewCompat.offsetTopAndBottom(view, targetOffset - view.getTop());
                        } else if (edgeFlag == EDGE_RIGHT) {
                            ViewCompat.offsetLeftAndRight(view, targetOffset - view.getLeft());
                        } else {
                            QMUILogger.i(TAG, "targetOffset = " + targetOffset + " ; view.getLeft() = " + view.getLeft());
                            ViewCompat.offsetLeftAndRight(view, -targetOffset - view.getLeft());
                        }
                    }
                }
            }

            @Override
            public void onEdgeTouch(int edgeFlag) {
                QMUILogger.i(TAG, "SwipeListener:onEdgeTouch: edgeFlag = " + edgeFlag);
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager == null) {
                    return;
                }
                int backstackCount = fragmentManager.getBackStackEntryCount();
                if (backstackCount > 1) {
                    try {
                        FragmentManager.BackStackEntry backStackEntry = fragmentManager.getBackStackEntryAt(backstackCount - 1);

                        Field opsField = backStackEntry.getClass().getDeclaredField("mOps");
                        opsField.setAccessible(true);
                        Object opsObj = opsField.get(backStackEntry);
                        if (opsObj instanceof List<?>) {
                            List<?> ops = (List<?>) opsObj;
                            for (Object op : ops) {
                                Field cmdField = op.getClass().getDeclaredField("cmd");
                                cmdField.setAccessible(true);
                                int cmd = (int) cmdField.get(op);
                                if (cmd == 3) {
                                    /**
                                     * 3 = OP_REMOVE,因为在{@link QMUIFragmentActivity#startFragment(QMUIFragment)}中是采用
                                     * replace的方式替换fragment，而fragment的replace操作 = remove + add ({@link BackStackRecord#executeOps()}可见源码)
                                     * 因此下方的所有操作含义如下：
                                     * 通过反射获取当前{@link BackStackRecord}中保存的fragment操作列表获取即将 OP_REMOVE的fragment,
                                     * 清空当前fragment中的 "popEnterAnim" 属性，并且找到对应fragment示例，重新onCreateView
                                     * 创建出该fragment的的View，此View作为SwipeBackLayout的back view,操作此view的位移达到跟随动画效果
                                     */
                                    Field popEnterAnimField = op.getClass().getDeclaredField("popEnterAnim");
                                    popEnterAnimField.setAccessible(true);
                                    popEnterAnimField.set(op, 0);

                                    Field fragmentField = op.getClass().getDeclaredField("fragment");
                                    fragmentField.setAccessible(true);
                                    Object fragmentObject = fragmentField.get(op);
                                    if (fragmentObject instanceof QMUIFragment && getBaseFragmentActivity() != null) {
                                        QMUIFragment fragment = (QMUIFragment) fragmentObject;
                                        ViewGroup container = getBaseFragmentActivity().getFragmentContainer();
                                        fragment.mIsCreateForSwipeBack = true;
                                        //recreate place holder view by fragment
                                        View baseView = fragment.onCreateView(LayoutInflater.from(getContext()), container, null);
                                        fragment.mIsCreateForSwipeBack = false;
                                        if (baseView != null) {
                                            baseView.setTag(R.id.qmui_arch_swipe_layout_in_back, SWIPE_BACK_VIEW);
                                            //add to activity root view layout.
                                            container.addView(baseView, 0);
                                            //init place holder view offset.
                                            int offset = Math.abs(backViewInitOffset());
                                            if (edgeFlag == EDGE_BOTTOM) {
                                                ViewCompat.offsetTopAndBottom(baseView, offset);
                                            } else if (edgeFlag == EDGE_RIGHT) {
                                                ViewCompat.offsetLeftAndRight(baseView, offset);
                                            } else {
                                                ViewCompat.offsetLeftAndRight(baseView, -1 * offset);
                                            }
                                        }
                                    }
                                }
                            }
                        }


                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().getWindow().getDecorView().setBackgroundColor(0);
                        Utils.convertActivityToTranslucent(getActivity());
                    }
                }

            }

            @Override
            public void onScrollOverThreshold() {
                QMUILogger.i(TAG, "SwipeListener:onEdgeTouch:onScrollOverThreshold");
            }
        });
        return swipeBackLayout;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SwipeBackLayout swipeBackLayout;
        if (mCachedSwipeBackView == null) {
            swipeBackLayout = newSwipeBackLayout();
            mCachedSwipeBackView = swipeBackLayout;
        } else if (mIsCreateForSwipeBack) {
            // in swipe back, exactly not in animation
            swipeBackLayout = mCachedSwipeBackView;
        } else {
            boolean isInRemoving = false;
            try {
                Method method = Fragment.class.getDeclaredMethod("getAnimatingAway");
                method.setAccessible(true);
                Object object = method.invoke(this);
                if (object != null) {
                    isInRemoving = true;
                }
            } catch (NoSuchMethodException e) {
                isInRemoving = true;
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                isInRemoving = true;
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                isInRemoving = true;
                e.printStackTrace();
            }
            if (isInRemoving) {
                swipeBackLayout = newSwipeBackLayout();
                mCachedSwipeBackView = swipeBackLayout;
            } else {
                swipeBackLayout = mCachedSwipeBackView;
            }
        }


        if (!mIsCreateForSwipeBack) {
            mBaseView = swipeBackLayout.getContentView();
            swipeBackLayout.setTag(R.id.qmui_arch_swipe_layout_in_back, null);
        }

        ViewCompat.setTranslationZ(swipeBackLayout, mBackStackIndex);

        swipeBackLayout.setFitsSystemWindows(false);

        if (getActivity() != null) {
            QMUIViewHelper.requestApplyInsets(getActivity().getWindow());
        }

        if (swipeBackLayout.getParent() != null) {
            ViewGroup viewGroup = (ViewGroup) swipeBackLayout.getParent();
            if (viewGroup.indexOfChild(swipeBackLayout) > -1) {
                viewGroup.removeView(swipeBackLayout);
            } else {
                // see https://issuetracker.google.com/issues/71879409
                try {
                    Field parentField = View.class.getDeclaredField("mParent");
                    parentField.setAccessible(true);
                    parentField.set(swipeBackLayout, null);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return swipeBackLayout;
    }

    protected void popBackStack() {
        if (mEnterAnimationStatus != ANIMATION_ENTER_STATUS_END) {
            return;
        }
        if (getBaseFragmentActivity() != null) {
            getBaseFragmentActivity().popBackStack();
        }
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (!enter && getParentFragment() != null && getParentFragment().isRemoving()) {
            // This is a workaround for the bug where child fragments disappear when
            // the parent is removed (as all children are first removed from the parent)
            // See https://code.google.com/p/android/issues/detail?id=55228
            Animation doNothingAnim = new AlphaAnimation(1, 1);
            int duration = getResources().getInteger(R.integer.qmui_anim_duration);
            doNothingAnim.setDuration(duration);
            return doNothingAnim;
        }
        Animation animation = null;
        if (enter) {
            try {
                animation = AnimationUtils.loadAnimation(getContext(), nextAnim);

            } catch (Resources.NotFoundException ignored) {

            } catch (RuntimeException ignored) {

            }
            if (animation != null) {
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        onEnterAnimationStart(animation);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        checkAndCallOnEnterAnimationEnd(animation);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            } else {
                onEnterAnimationStart(null);
                checkAndCallOnEnterAnimationEnd(null);
            }
        }
        return animation;
    }


    private void checkAndCallOnEnterAnimationEnd(@Nullable Animation animation) {
        mCalled = false;
        onEnterAnimationEnd(animation);
        if (!mCalled) {
            throw new RuntimeException("QMUIFragment " + this + " did not call through to super.onEnterAnimationEnd(Animation)");
        }
    }


    /**
     * The real content view hold by {@link SwipeBackLayout}
     */
    protected abstract View onCreateView();

    /**
     * disable or enable drag back
     */
    protected boolean canDragBack() {
        return true;
    }

    /**
     * The default drag back edge is {@link #EDGE_LEFT}
     */
    protected int dragBackEdge() {
        return EDGE_LEFT;
    }

    /**
     * Only work if drag back is enable,
     * default is 0(no offset)
     *
     * @return the offset of back view
     */
    protected int backViewInitOffset() {
        return 0;
    }

    /**
     * 在动画开始前或动画结束后都会被直接执行
     *
     * @param runnable
     */
    public void runAfterAnimation(Runnable runnable) {
        runAfterAnimation(runnable, false);
    }

    /**
     * 异步数据渲染时，调用这个方法可以保证数据是在转场动画结束后进行渲染。
     * 转场动画过程中进行数据渲染时，会造成卡顿，从而影响用户体验
     *
     * @param runnable
     * @param onlyEnd
     */
    public void runAfterAnimation(Runnable runnable, boolean onlyEnd) {
        Utils.assertInMainThread();
        boolean ok = onlyEnd ? mEnterAnimationStatus == ANIMATION_ENTER_STATUS_END :
                mEnterAnimationStatus != ANIMATION_ENTER_STATUS_STARTED;
        if (ok) {
            runnable.run();
        } else {
            mDelayRenderRunnableList.add(runnable);
        }
    }

    //============================= 新流程 ================================

    protected void onEnterAnimationStart(@Nullable Animation animation) {
        mEnterAnimationStatus = ANIMATION_ENTER_STATUS_STARTED;
    }

    protected void onEnterAnimationEnd(@Nullable Animation animation) {
        if (mCalled) {
            throw new IllegalAccessError("don't call #onEnterAnimationEnd() directly");
        }
        mCalled = true;
        mEnterAnimationStatus = ANIMATION_ENTER_STATUS_END;
        if (mDelayRenderRunnableList.size() > 0) {
            for (int i = 0; i < mDelayRenderRunnableList.size(); i++) {
                mDelayRenderRunnableList.get(i).run();
            }
            mDelayRenderRunnableList.clear();
        }
    }

    /**
     * Handle StatusBar immerse mode,false: the content area lay under status bar area.
     * true: the content area has top padding(status bar height) on the top
     */
    protected boolean obtainsFitsSystemWindows() {
        return true;
    }

    /**
     * The fragment transition config
     */
    public TransitionConfig obtainTransitionConfig() {
        return SLIDE_TRANSITION_CONFIG;
    }

    /**
     * 界面跳转动画配置类，主要配置动画 enter,
     * exit,pop enter,pop out四种情况
     */
    public static final class TransitionConfig {
        public final int enter;
        public final int exit;
        public final int popenter;
        public final int popout;

        public TransitionConfig(int enter, int popout) {
            this(enter, 0, 0, popout);
        }

        public TransitionConfig(int enter, int exit, int popenter, int popout) {
            this.enter = enter;
            this.exit = exit;
            this.popenter = popenter;
            this.popout = popout;
        }
    }
}

