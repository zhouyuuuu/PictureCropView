package com.example.administrator.cameraproject.module.camera.model;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.widget.ImageView;

import com.example.administrator.cameraproject.module.camera.presenter.IPhotoPresenter;
import com.example.administrator.cameraproject.module.camera.util.StoreUtil;

import java.lang.ref.WeakReference;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 照片Model层
 * Edited by Administrator on 2018/4/10.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PhotoLoader implements IPhotoModel {

    // 主线程Handler
    private final Handler mMainHandler;
    // presenter
    private IPhotoPresenter mPhotoPresenter;
    // 线程池
    private ThreadPoolExecutor mThreadPoolExecutor;

    public PhotoLoader(IPhotoPresenter iPhotoPresenter) {
        this.mPhotoPresenter = iPhotoPresenter;
        mMainHandler = new Handler(Looper.getMainLooper());
        mThreadPoolExecutor = new ThreadPoolExecutor(1, 1, 1000, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());
    }

    /**
     * 加载图片
     *
     * @param fileName  文件名
     * @param imageView imageView
     */
    @Override
    public void loadBitmap(final String fileName, final ImageView imageView) {
        if (fileName != null) {
            mThreadPoolExecutor.execute(new LoadBitmapRunnable(1, fileName, imageView));
        }
    }

    /**
     * 终结线程池，释放引用
     */
    @Override
    public void onDestroy() {
        mThreadPoolExecutor.shutdownNow();
        mPhotoPresenter = null;
    }

    /**
     * 加载图片任务
     */
    private class LoadBitmapRunnable extends BaseLoadRunnable {
        private String fileName;
        private WeakReference<ImageView> imageViewWeakReference;

        LoadBitmapRunnable(int priority, String fileName, ImageView imageView) {
            super(priority);
            this.fileName = fileName;
            this.imageViewWeakReference = new WeakReference<>(imageView);
        }

        @Override
        void call() {
            ImageView imageView = imageViewWeakReference.get();
            if (imageView == null) return;
            final Bitmap bitmap = StoreUtil.loadBitmapThumbnail(fileName, imageView);
            postMainThread(new Runnable() {
                @Override
                public void run() {
                    // 同步回调
                    mPhotoPresenter.photoLoaded(bitmap);
                }
            });
        }
    }

    /**
     * 线程池运行Runnable的基类
     */
    private abstract class BaseLoadRunnable implements Runnable, Comparable<BaseLoadRunnable> {
        // 优先级
        private final int priority;

        BaseLoadRunnable(int priority) {
            this.priority = priority;
        }

        private int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(@NonNull BaseLoadRunnable another) {
            int my = this.getPriority();
            int other = another.getPriority();
            return my < other ? 1 : my > other ? -1 : 0;
        }

        @Override
        public void run() {
            call();
        }

        abstract void call();

        /**
         * post到主线程
         */
        void postMainThread(Runnable runnable) {
            synchronized (mMainHandler) {
                mMainHandler.post(runnable);
            }
        }
    }
}
