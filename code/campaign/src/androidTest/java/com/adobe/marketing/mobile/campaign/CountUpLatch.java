/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.campaign;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CountUpLatch {

    final Lock lock = new ReentrantLock();
    final Condition notFull = lock.newCondition();
    int count = 0;

    public void await(final int expectedCount, final int timeoutInMilli) {
        lock.lock();
        int timeout = 0;

        try {
            while (count < expectedCount && timeout < timeoutInMilli / 100) {
                notFull.await(100, TimeUnit.MILLISECONDS);
                timeout++;
            }
        } catch (InterruptedException e) {

        } finally {
            lock.unlock();
        }
    }

    public void await(final int expectedCount) {
        this.await(expectedCount, 2000);
    }


    public void countUp() {
        lock.lock();

        try {
            count++;
            notFull.signal();
        } finally {
            lock.unlock();
        }


    }

}
