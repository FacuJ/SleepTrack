package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {
    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private var tonight = MutableLiveData<SleepNight?>()
    private val nights = database.getAllNights()

    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()

    val navigateToSleepQuality: LiveData<SleepNight>
    get() = _navigateToSleepQuality

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun getTonightFromDatabase(): SleepNight? {
        return withContext(Dispatchers.IO){
            var night = database.getTonight()
            if(night?.endTimeMilli != night?.startTimeMilli) night = null
            night
        }
    }

    fun onStartTracking(){
        uiScope.launch {
            val newNight = SleepNight()
            insertNewNight(newNight)
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insertNewNight(newNight: SleepNight) {
        withContext(Dispatchers.IO){
            database.insert(newNight)
        }
    }

    fun onStopTracking() {
        uiScope.launch {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()
            updateNight(oldNight)
            _navigateToSleepQuality.value = oldNight
        }
    }

    private suspend fun updateNight(oldNight: SleepNight) {
        withContext(Dispatchers.IO){
            database.update(oldNight)
        }
    }

    fun onClearData() {
        uiScope.launch {
            clearNights()
            tonight.value = null
        }
    }

    private suspend fun clearNights() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }

    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}

