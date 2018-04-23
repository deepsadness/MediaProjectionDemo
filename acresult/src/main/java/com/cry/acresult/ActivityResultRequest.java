package com.cry.acresult;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.MainThreadDisposable;

/**
 * Created by a2957 on 4/21/2018.
 */

public class ActivityResultRequest {

    public static OnActivityResultDispatcherFragment getDispatcherFragment(FragmentActivity activity) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        Fragment dispatcherFragment = fragmentManager.findFragmentByTag(OnActivityResultDispatcherFragment.TAG);
        if (dispatcherFragment == null) {
            dispatcherFragment = new OnActivityResultDispatcherFragment();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(dispatcherFragment, OnActivityResultDispatcherFragment.TAG);
            fragmentTransaction.commitNowAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
        return (OnActivityResultDispatcherFragment) dispatcherFragment;
    }

    public static Observable<OnActivityResultDispatcherFragment> rxOf(FragmentActivity activity) {
        return Observable
                .just(activity)
                .map(ActivityResultRequest::getDispatcherFragment);
    }

    public static Observable<ResultEvent> rxQuest(FragmentActivity activity, Intent intent) {
        return Observable
                .just(activity)
                .map(ActivityResultRequest::getDispatcherFragment)
                .flatMap(dispatcherFragment -> new OnResultObservable(dispatcherFragment, intent));
    }

    private final static class OnResultObservable extends Observable<ResultEvent> {
        private final int requestCode = OnActivityResultDispatcherFragment.AUTO_REQ_CODE.incrementAndGet();
        private final OnActivityResultDispatcherFragment dispatcherFragment;
        private final Intent intent;

        public OnResultObservable(OnActivityResultDispatcherFragment dispatcherFragment, Intent intent) {
            this.dispatcherFragment = dispatcherFragment;
            this.intent = intent;
        }

        @Override
        protected void subscribeActual(Observer<? super ResultEvent> observer) {
            Listener listener = new Listener(dispatcherFragment, observer, requestCode);
            observer.onSubscribe(listener);
            dispatcherFragment.startIntentForResult(intent, listener, requestCode);
        }

        private static final class Listener extends MainThreadDisposable implements OnActivityResultDispatcherFragment.OnResultListener {

            private final OnActivityResultDispatcherFragment dispatch;
            private final Observer<? super ResultEvent> observer;
            private int requestCode;

            public Listener(OnActivityResultDispatcherFragment dispatcherFragment, Observer<? super ResultEvent> observer, int requestCode) {
                this.dispatch = dispatcherFragment;
                this.observer = observer;
                this.requestCode = requestCode;
            }

            @Override
            public void onActivityResult(int resultCode, Intent data) {
                if (!isDisposed()) {
                    observer.onNext(new ResultEvent(resultCode, data));
                }
            }

            @Override
            protected void onDispose() {
                dispatch.remove(requestCode);
            }
        }
    }
}
