package com.example.gpslaptimer.ui.map;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MapViewModel extends ViewModel {
    private final MutableLiveData<String> fileName = new MutableLiveData<>("");

    public void setFileName(String fileName) {
        this.fileName.postValue(fileName);
    }
    public MutableLiveData<String> getFileName() {
        return fileName;
    }
}
