package cn.tillusory.fancy.model;

import android.app.Activity;
import androidx.annotation.NonNull;


public class HomePageItem {

 private final String title;

 private final Class<? extends Activity> activity;

 public HomePageItem(@NonNull final String title,@NonNull final Class<? extends Activity> activity) {
  this.title = title;
  this.activity = activity;
 }

 public String getTitle() {
  return title;
 }

 public Class<? extends Activity> getActivity() {
  return activity;
 }
}
