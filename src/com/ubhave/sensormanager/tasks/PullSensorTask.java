package com.ubhave.sensormanager.tasks;

import com.ubhave.sensormanager.ESException;
import com.ubhave.sensormanager.config.SensorConfig;
import com.ubhave.sensormanager.data.SensorData;
import com.ubhave.sensormanager.logs.ESLogger;
import com.ubhave.sensormanager.sensors.SensorInterface;
import com.ubhave.sensormanager.sensors.SensorUtils;
import com.ubhave.sensormanager.sensors.pull.PullSensor;

public class PullSensorTask extends AbstractSensorTask
{
	private final static String TAG = "PullSensorTask";
	
	public PullSensorTask(SensorInterface sensor)
	{
		super(sensor);
	}
	
	public SensorData getCurrentSensorData() throws ESException
	{
		SensorData sensorData = ((PullSensor) sensor).sense();

		// since this is a one-off query for sensor data, sleep interval
		// is not relevant in this case
		SensorConfig sensorConfig = sensorData.getSensorConfig();
		sensorConfig.removeParameter(SensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS);

		return sensorData;
	}

	public void run()
	{
		synchronized (syncObject)
		{
			while (true)
			{
				try
				{
					try
					{
						

						if ((state == PAUSED) || (state == STOPPED))
						{
							if (state == PAUSED)
							{
								syncObject.wait(pauseTime);
							}
							else if (state == STOPPED)
							{
								syncObject.wait();
							}
							state = RUNNING;
							continue;
						}
						
						// SENSE
						// sense() is a blocking call and returns when
						// the sensing is complete, the sensorConfig object
						// will have the sampling window, cycle information
						ESLogger.log(getLogTag(), "Pulling from: " + SensorUtils.getSensorName(sensor.getSensorType()));
						SensorData sensorData = getCurrentSensorData();
						// publish sensed data
						publishData(sensorData);
						
						// SLEEP
						long samplingInterval = (Long) sensor.getSensorConfig(SensorConfig.POST_SENSE_SLEEP_LENGTH_MILLIS);
						syncObject.wait(samplingInterval);
					}
					catch (InterruptedException exp)
					{
						// ignore
					}
				}
				catch (ESException e)
				{
					ESLogger.error(TAG, e);
					try
					{
						Thread.sleep(30000);
					}
					catch (Exception exp)
					{
						exp.printStackTrace();
					}
				}
			}
		}
	}
}
