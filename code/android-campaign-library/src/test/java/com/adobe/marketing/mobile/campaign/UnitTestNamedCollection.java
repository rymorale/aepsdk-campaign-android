/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.campaign;

import com.adobe.marketing.mobile.services.NamedCollection;

import java.util.HashMap;
import java.util.Map;

public class UnitTestNamedCollection implements NamedCollection {
    private Map<String, Object> storedValues = new HashMap<>();

    @Override
    public void setInt(String s, int i) {
        // not implemented
    }

    @Override
    public int getInt(String s, int i) {
        // not implemented
        return 0;
    }

    @Override
    public void setString(String s, String s1) {
        storedValues.put(s, s1);
    }

    @Override
    public String getString(String s, String s1) {
        return (storedValues.get(s)) != null ? (String) storedValues.get(s) : s1;
    }

    @Override
    public void setDouble(String s, double v) {
        // not implemented
    }

    @Override
    public double getDouble(String s, double v) {
        // not implemented
        return 0;
    }

    @Override
    public void setLong(String s, long l) {
        storedValues.put(s, l);
    }

    @Override
    public long getLong(String s, long l) {
        return (storedValues.get(s)) != null ? (long) storedValues.get(s) : l;
    }

    @Override
    public void setFloat(String s, float v) {
        // not implemented
    }

    @Override
    public float getFloat(String s, float v) {
        // not implemented
        return 0;
    }

    @Override
    public void setBoolean(String s, boolean b) {
        // not implemented
    }

    @Override
    public boolean getBoolean(String s, boolean b) {
        // not implemented
        return false;
    }

    @Override
    public void setMap(String s, Map<String, String> map) {
        // not implemented
    }

    @Override
    public Map<String, String> getMap(String s) {
        // not implemented
        return null;
    }

    @Override
    public boolean contains(String s) {
        // not implemented
        return false;
    }

    @Override
    public void remove(String s) {
        storedValues.remove(s);
    }

    @Override
    public void removeAll() {
        storedValues = new HashMap<>();
    }
}
