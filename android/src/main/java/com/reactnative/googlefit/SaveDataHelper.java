package com.reactnative.googlefit;

import android.os.AsyncTask;

import com.facebook.react.bridge.Promise;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataSet;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

class SaveDataHelper extends AsyncTask<Void, Void, Void> {
  private ArrayList<DataSet> dataSets;
  private GoogleFitManager googleFitManager;
  private Promise promise;

  SaveDataHelper(ArrayList<DataSet> dataSets, GoogleFitManager googleFitManager, Promise promise) {
    this.dataSets = dataSets;
    this.googleFitManager = googleFitManager;
    this.promise = promise;
  }

  @Override
  protected Void doInBackground(Void... params) {
    DataSet dataSet;
    com.google.android.gms.common.api.Status   status;
    for (int index = 0 ; index < this.dataSets.size() ; index++) {
      dataSet = this.dataSets.get(index);
      status = Fitness.HistoryApi.insertData(googleFitManager.getGoogleApiClient(), dataSet)
              .await(1, TimeUnit.MINUTES);
      if (!status.isSuccess()) {
        // Abort rest
        promise.reject(status.toString());
        return null;
      }
    }
    promise.resolve(true);
    return null;
  }
}
