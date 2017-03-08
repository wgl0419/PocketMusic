package com.example.q.pocketmusic.callback;

import android.content.Context;

import com.example.q.pocketmusic.util.MyToast;

import java.util.List;


import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.QueryListListener;

/**
 * Created by Cloud on 2017/1/28.
 */

public abstract class ToastQueryListListener<BatchResult> extends QueryListListener<BatchResult> {
    private Context context;
    private IBaseView baseView;

    public ToastQueryListListener(Context context, IBaseView baseView) {
        this.context = context;
        this.baseView = baseView;
    }

    public abstract void onSuccess(List<BatchResult> list);

    @Override
    final public void done(List<BatchResult> list, BmobException e) {
        if (e == null) {
            onSuccess(list);
        } else {

            onFail(e);
        }
    }

    public void onFail(BmobException e) {
        baseView.showLoading(false);
        MyToast.showToast(context, "发生错误");
        e.printStackTrace();
        //        CrashHandler handler=CrashHandler.getInstance();
//        handler.uncaughtException(Thread.currentThread(),e);
    }
}
