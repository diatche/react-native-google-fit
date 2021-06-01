package com.reactnative.googlefit;

import android.os.AsyncTask;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataDeleteRequest;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

class DeleteDataHelper extends AsyncTask<Void, Void, Void> {
  private long startTime;
  private long endTime;
  private DataType dataType;
  private GoogleFitManager googleFitManager;
  public com.google.android.gms.common.api.Status status;

  DeleteDataHelper(
    long startTime,
    long endTime,
    DataType dataType,
    GoogleFitManager googleFitManager
  ) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.dataType = dataType;
    this.googleFitManager = googleFitManager;
  }

  @Override
  protected Void doInBackground(Void... params) {
    DataDeleteRequest request = new DataDeleteRequest.Builder()
      .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
      .addDataType(this.dataType)
      .build();

    status =
      Fitness.HistoryApi.deleteData(googleFitManager.getGoogleApiClient(), request)
        .await(1, TimeUnit.MINUTES);

    return null;
  }
}
