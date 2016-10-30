/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Tobias Fleig (tobifleig gmail com)
 *
 * All rights reserved.
 *
 * This file is part of LanXchange.
 *
 * LanXchange is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LanXchange is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LanXchange. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tobifleig.lxc.plaf.swing;

import de.tobifleig.lxc.data.LXCJob;
import de.tobifleig.lxc.plaf.ProgressIndicator;

/**
 * Replaces the ProgressIndicator of managed Jobs,
 * computes a global average (arithmetic mean) of all managed jobs
 * and offers notifications for changes in both global and
 * single (per-job) percentages.
 * <p>
 * Thread-safe (concurrent percentage updates), fast, all operations run in constant time.
 */
public abstract class OverallProgressManager {

    private float overallProgress = 0;
    private int numberOfTrackedProgresses = 0;

    private int lastOverallProgressInt = 0;

    public void handleNewJob(final LXCJob job) {
        job.getTrans().setProgressIndicator(new ProgressIndicator() {

            int lastPercentage = 0;

            @Override
            public void update(int percentage) {
                if (percentage != lastPercentage) {
                    synchronized (OverallProgressManager.this) {
                        overallProgress -= (1f * lastPercentage / numberOfTrackedProgresses);
                        overallProgress += (1f * percentage / numberOfTrackedProgresses);
                        updateLastOverallInt();
                    }
                    lastPercentage = percentage;
                    notifySingleProgressChanged();
                }
            }
        });
        synchronized (this) {
            numberOfTrackedProgresses++;
        }
    }

    public void removeJob(LXCJob job) {
        synchronized (this) {
            // first, remove this job's part from the global average
            job.getTrans().getProgressIndicator().update(0);
            numberOfTrackedProgresses--;
        }
    }

    public int getOverallProgress() {
        return (int) overallProgress;
    }

    private void updateLastOverallInt() {
        if (lastOverallProgressInt != ((int) overallProgress)) {
            lastOverallProgressInt = (int) overallProgress;
            notifyOverallProgressChanged(lastOverallProgressInt);
        }
    }

    public abstract void notifyOverallProgressChanged(int percentage);

    public abstract void notifySingleProgressChanged();

}
