package com.example.gpslaptimer.ui.add;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AddViewModel extends ViewModel {
    private final MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);

    public MutableLiveData<Boolean> getIsRunning() {
        return isRunning;
    }
    public void setIsRunning(Boolean isRunning) { this.isRunning.postValue(isRunning); }
}