package com.reactnative.googlefit;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.HealthDataTypes;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.HealthFields;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class BloodGlucoseHistory {
  private ReactContext mReactContext;
  private GoogleFitManager googleFitManager;

  private static final String TAG = "BloodGlucoseHistory";
  private static final int MAX_DATAPOINTS_PER_SINGLE_REQUEST = 900;
  private DataType dataType = HealthDataTypes.TYPE_BLOOD_GLUCOSE;

  public BloodGlucoseHistory(ReactContext reactContext, GoogleFitManager googleFitManager) {
    this.mReactContext = reactContext;
    this.googleFitManager = googleFitManager;
  }

  private DataSource getDataSource() {
    return new DataSource.Builder()
      .setAppPackageName(GoogleFitPackage.PACKAGE_NAME)
      .setDataType(this.dataType)
      .setStreamName("bloodGlucoseSource")
      .setType(DataSource.TYPE_RAW)
      .build();
  }

  public ReadableArray getHistory(long startTime, long endTime) {
    DateFormat dateFormat = DateFormat.getDateInstance();

    DataReadRequest readRequest = new DataReadRequest.Builder()
      .read(this.dataType)
      .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS).build();

    DataReadResult dataReadResult = Fitness.HistoryApi.readData(googleFitManager.getGoogleApiClient(), readRequest)
      .await(1, TimeUnit.MINUTES);

    WritableArray map = Arguments.createArray();

    if (dataReadResult.getDataSets().size() > 0) {
      for (DataSet dataSet : dataReadResult.getDataSets()) {
        processDataSet(dataSet, map);
      }
    }

    return map;
  }

  private void processDataSet(DataSet dataSet, WritableArray map) {
    for (DataPoint dp : dataSet.getDataPoints()) {
      WritableMap bloodGlucoseMap = Arguments.createMap();
      Value bloodGlucoseLevel = dp.getValue((HealthFields.FIELD_BLOOD_GLUCOSE_LEVEL));
      Value temporalRelationToMeal = dp.getValue((HealthFields.FIELD_TEMPORAL_RELATION_TO_MEAL));
      Value mealType = dp.getValue((Field.FIELD_MEAL_TYPE));
      Value temporalRelationToSleep = dp.getValue((HealthFields.FIELD_TEMPORAL_RELATION_TO_SLEEP));
      Value bloodGlucoseSpecimenSource = dp.getValue((HealthFields.FIELD_BLOOD_GLUCOSE_SPECIMEN_SOURCE));

      bloodGlucoseMap.putDouble("date", dp.getEndTime(TimeUnit.MILLISECONDS));
      bloodGlucoseMap.putDouble("bloodGlucoseLevel", bloodGlucoseLevel.asFloat());
      bloodGlucoseMap.putInt("temporalRelationToMeal", temporalRelationToMeal.asInt());
      bloodGlucoseMap.putInt("mealType", mealType.asInt());
      bloodGlucoseMap.putInt("temporalRelationToSleep", temporalRelationToSleep.asInt());
      bloodGlucoseMap.putInt("bloodGlucoseSpecimenSource", bloodGlucoseSpecimenSource.asInt());
      bloodGlucoseMap.putString("addedBy", dp.getOriginalDataSource().getAppPackageName());

      map.pushMap(bloodGlucoseMap);
    }
  }

  public boolean save(ReadableArray bloodGlucoseArray) {
    DataSource bloodGlucoseSource = this.getDataSource();
    ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();
    ArrayList<DataSet> dataSets = new ArrayList<DataSet>();
    for (int index = 0 ; index < bloodGlucoseArray.size() ; index++) {
      ReadableMap bloodGlucoseSample = bloodGlucoseArray.getMap(index);
      if (bloodGlucoseSample != null) {
        dataPoints.add(DataPoint.builder(bloodGlucoseSource)
          .setTimestamp((long) bloodGlucoseSample.getDouble("date"), TimeUnit.MILLISECONDS)
          .setField(HealthFields.FIELD_BLOOD_GLUCOSE_LEVEL, (float) bloodGlucoseSample.getDouble("bloodGlucoseLevel"))
          .setField(HealthFields.FIELD_TEMPORAL_RELATION_TO_MEAL, (int) bloodGlucoseSample.getInt("temporalRelationToMeal"))
          .setField(Field.FIELD_MEAL_TYPE, (int) bloodGlucoseSample.getInt("mealType"))
          .setField(HealthFields.FIELD_TEMPORAL_RELATION_TO_SLEEP, (int) bloodGlucoseSample.getInt("temporalRelationToSleep"))
          .setField(HealthFields.FIELD_BLOOD_GLUCOSE_SPECIMEN_SOURCE, (int) bloodGlucoseSample.getInt("bloodGlucoseSpecimenSource"))
          .build());
      }
      if (dataPoints.size() % MAX_DATAPOINTS_PER_SINGLE_REQUEST == 0) {
        // Be sure to limit each individual request to 1000 datapoints. Exceeding this limit could result in an error.
        // https://developers.google.com/fit/android/history#insert_data
        dataSets.add(DataSet.builder(bloodGlucoseSource).addAll(dataPoints).build());
        dataPoints.clear();
      }
    }
    if (dataPoints.size() > 0) {
      dataSets.add(DataSet.builder(bloodGlucoseSource).addAll(dataPoints).build());
    }
    new SaveDataHelper(dataSets, googleFitManager).execute();

    return true;
  }

  public boolean delete(ReadableMap options) {
    long endTime = (long) options.getDouble("endDate");
    long startTime = (long) options.getDouble("startDate");
    new DeleteDataHelper(startTime, endTime, this.dataType, googleFitManager).execute();
    return true;
  }
}
