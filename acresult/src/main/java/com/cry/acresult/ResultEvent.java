package com.cry.acresult;

import android.content.Intent;

/**
 * Created by a2957 on 4/21/2018.
 */

public class ResultEvent {
    //    public int requestCode;
    public int resultCode;
    public Intent data;

    public ResultEvent() {
    }

    public ResultEvent(int resultCode, Intent data) {
        this.resultCode = resultCode;
        this.data = data;
    }


    @Override
    public String toString() {
        return "ResultEvent{" +
//                "requestCode=" + requestCode +
                ", resultCode=" + resultCode +
                ", data=" + data +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResultEvent that = (ResultEvent) o;

//        if (requestCode != that.requestCode) return false;
        if (resultCode != that.resultCode) return false;
        return data != null ? data.equals(that.data) : that.data == null;
    }

    @Override
    public int hashCode() {

        int result = resultCode;
        result = 31 * result + resultCode;
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }
}
