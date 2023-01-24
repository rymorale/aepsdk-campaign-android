/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.campaign;

import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheExpiry;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Test helper for functional testing to read, write, reset and assert against eventhub events, shared states and persistence data.
 */
public class TestHelper {
    private static final String LOG_TAG = "TestHelper";
    static final int WAIT_TIMEOUT_MS = 1000;
    static final int WAIT_EVENT_TIMEOUT_MS = 2000;
    private static final String REMOTE_URL = "https://www.adobe.com/adobe.png";
    private static final int STREAM_WRITE_BUFFER_SIZE = 4096;
    private static final String CHARSET_UTF_8 = "UTF-8";
    private static final int STREAM_READ_BUFFER_SIZE = 1024;
    // List of threads to wait for after test execution
    private static final List<String> knownThreads = new ArrayList<String>();
    static Application defaultApplication;

    {
        knownThreads.add("pool"); // used for threads that execute the listeners code
        knownThreads.add("ADB"); // module internal threads
    }

    /**
     * Waits for all the {@code #knownThreads} to finish or fails the test after timeoutMillis if some of them are still running
     * when the timer expires. If timeoutMillis is 0, a default timeout will be set = 1000ms
     *
     * @param timeoutMillis max waiting time
     */
    public static void waitForThreads(final int timeoutMillis) {
        int TEST_DEFAULT_TIMEOUT_MS = 1000;
        int TEST_DEFAULT_SLEEP_MS = 50;
        int TEST_INITIAL_SLEEP_MS = 100;

        long startTime = System.currentTimeMillis();
        int timeoutTestMillis = timeoutMillis > 0 ? timeoutMillis : TEST_DEFAULT_TIMEOUT_MS;
        int sleepTime = Math.min(timeoutTestMillis, TEST_DEFAULT_SLEEP_MS);

        sleep(TEST_INITIAL_SLEEP_MS);
        Set<Thread> threadSet = getEligibleThreads();

        while (threadSet.size() > 0 && ((System.currentTimeMillis() - startTime) < timeoutTestMillis)) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "waitForThreads - Still waiting for " + threadSet.size() + " thread(s)");

            for (Thread t : threadSet) {

                MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "waitForThreads - Waiting for thread " + t.getName() + " (" + t.getId() + ")");
                boolean done = false;
                boolean timedOut = false;

                while (!done && !timedOut) {
                    if (t.getState().equals(Thread.State.TERMINATED)
                            || t.getState().equals(Thread.State.TIMED_WAITING)
                            || t.getState().equals(Thread.State.WAITING)) {
                        //Cannot use the join() API since we use a cached thread pool, which
                        //means that we keep idle threads around for 60secs (default timeout).
                        done = true;
                    } else {
                        //blocking
                        sleep(sleepTime);
                        timedOut = (System.currentTimeMillis() - startTime) > timeoutTestMillis;
                    }
                }

                if (timedOut) {
                    MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
                            "waitForThreads - Timeout out waiting for thread " + t.getName() + " (" + t.getId() + ")");
                } else {
                    MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
                            "waitForThreads - Done waiting for thread " + t.getName() + " (" + t.getId() + ")");
                }
            }

            threadSet = getEligibleThreads();
        }

        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "waitForThreads - All known threads are terminated.");
    }

    /**
     * Retrieves all the known threads that are still running
     *
     * @return set of running tests
     */
    private static Set<Thread> getEligibleThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Set<Thread> eligibleThreads = new HashSet<Thread>();

        for (Thread t : threadSet) {
            if (isAppThread(t) && !t.getState().equals(Thread.State.WAITING) && !t.getState().equals(Thread.State.TERMINATED)
                    && !t.getState().equals(Thread.State.TIMED_WAITING)) {
                eligibleThreads.add(t);
            }
        }

        return eligibleThreads;
    }

    /**
     * Checks if current thread is not a daemon and its name starts with one of the known thread names specified here
     * {@link #knownThreads}
     *
     * @param t current thread to verify
     * @return true if it is a known thread, false otherwise
     */
    private static boolean isAppThread(final Thread t) {
        if (t.isDaemon()) {
            return false;
        }

        for (String prefix : knownThreads) {
            if (t.getName().startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Resets the network and event test expectations.
     */
    public static void resetTestExpectations() {
        MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "Resetting functional test expectations for events");
        MonitorExtension.reset();
    }

    /**
     * Returns the {@code Event}(s) dispatched through the Event Hub, or empty if none was found.
     *
     * @param type   the event type as in the expectation
     * @param source the event source as in the expectation
     * @return list of events with the provided {@code type} and {@code source}, or empty if none was dispatched
     * @throws InterruptedException
     * @throws IllegalArgumentException if {@code type} or {@code source} are null or empty strings
     */
    public static List<Event> getDispatchedEventsWith(final String type, final String source) throws InterruptedException {
        return getDispatchedEventsWith(type, source, WAIT_EVENT_TIMEOUT_MS);
    }

    /**
     * Returns the {@code Event}(s) dispatched through the Event Hub, or empty if none was found.
     *
     * @param source  the event source as in the expectation
     * @param timeout how long should this method wait for the expected event, in milliseconds.
     * @return list of events with the provided {@code type} and {@code source}, or empty if none was dispatched
     * @throws InterruptedException
     * @throws IllegalArgumentException if {@code type} or {@code source} are null or empty strings
     */
    public static List<Event> getDispatchedEventsWith(final String type, final String source,
                                                      int timeout) throws InterruptedException {
        MonitorExtension.EventSpec eventSpec = new MonitorExtension.EventSpec(source, type);

        Map<MonitorExtension.EventSpec, List<Event>> receivedEvents = MonitorExtension.getReceivedEvents();
        Map<MonitorExtension.EventSpec, ADBCountDownLatch> expectedEvents = MonitorExtension.getExpectedEvents();

        ADBCountDownLatch expectedEventLatch = expectedEvents.get(eventSpec);

        if (expectedEventLatch != null) {
            boolean awaitResult = expectedEventLatch.await(timeout, TimeUnit.MILLISECONDS);
            assertTrue("Timed out waiting for event type " + eventSpec.type + " and source " + eventSpec.source, awaitResult);
        } else {
            sleep(WAIT_TIMEOUT_MS);
        }

        return receivedEvents.containsKey(eventSpec) ? receivedEvents.get(eventSpec) : Collections.emptyList();
    }

    // ---------------------------------------------------------------------------------------------
    // Event Test Helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * Synchronous call to get the shared state for the specified {@code stateOwner}.
     * This API throws an assertion failure in case of timeout.
     *
     * @param stateOwner the owner extension of the shared state (typically the name of the extension)
     * @param timeout    how long should this method wait for the requested shared state, in milliseconds
     * @return latest shared state of the given {@code stateOwner} or null if no shared state was found
     * @throws InterruptedException
     */
    public static Map<String, Object> getSharedStateFor(final String stateOwner, int timeout) throws InterruptedException {
        Event event = new Event.Builder("Get Shared State Request", TestConstants.EventType.MONITOR,
                TestConstants.EventSource.SHARED_STATE_REQUEST)
                .setEventData(new HashMap<String, Object>() {
                    {
                        put(TestConstants.EventDataKey.STATE_OWNER, stateOwner);
                    }
                })
                .build();

        final ADBCountDownLatch latch = new ADBCountDownLatch(1);
        final Map<String, Object> sharedState = new HashMap<>();
        MobileCore.dispatchEventWithResponseCallback(event,
                (long) timeout,
                new AdobeCallbackWithError<Event>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        MobileCore.log(LoggingMode.ERROR, LOG_TAG, "Failed to get shared state for " + stateOwner + ": " + adobeError);
                        latch.countDown();
                    }

                    @Override
                    public void call(Event event) {
                        if (event.getEventData() != null) {
                            sharedState.putAll(event.getEventData());
                            latch.countDown();
                        }
                    }
                });

        latch.await(timeout, TimeUnit.MILLISECONDS);
        return sharedState.isEmpty() ? null : sharedState;
    }

    /**
     * Pause test execution for the given {@code milliseconds}
     *
     * @param milliseconds the time to sleep the current thread.
     */
    public static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@code TestRule} which sets up the MobileCore for testing before each test execution, and
     * tearsdown the MobileCore after test execution.
     * <p>
     * To use, add the following to your test class:
     * <pre>
     *    @Rule
     *    public TestHelper.SetupCoreRule coreRule = new TestHelper.SetupCoreRule();
     * </pre>
     */
    public static class SetupCoreRule implements TestRule {

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (defaultApplication == null) {
                        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
                        defaultApplication = Instrumentation.newApplication(CustomApplication.class, context);
                    }

                    MobileCore.setLogLevel(LoggingMode.VERBOSE);
                    MobileCore.setApplication(defaultApplication);

                    try {
                        base.evaluate();
                    } catch (Throwable e) {
                        MobileCore.log(LoggingMode.DEBUG, "SetupCoreRule", "Wait after test failure.");
                        throw e; // rethrow test failure
                    } finally {
                        // After test execution
                        MobileCore.log(LoggingMode.DEBUG, "SetupCoreRule", "Finished '" + description.getMethodName() + "'");
                        waitForThreads(5000); // wait to allow thread to run after test execution
                        resetTestExpectations();
                    }
                }
            };
        }
    }

    /**
     * {@code TestRule} which registers the {@code MonitorExtension}, allowing test cases to assert
     * events passing through the {@code EventHub}. This {@code TestRule} must be applied after
     * the {@link SetupCoreRule} to ensure the {@code MobileCore} is setup for testing first.
     * <p>
     * To use, add the following to your test class:
     * <pre>
     *  @Rule
     *    public RuleChain rule = RuleChain.outerRule(new SetupCoreRule())
     * 							.around(new RegisterMonitorExtensionRule());
     * </pre>
     */
    public static class RegisterMonitorExtensionRule implements TestRule {

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    MonitorExtension.registerExtension();

                    try {
                        base.evaluate();
                    } finally {
                        MonitorExtension.reset();
                    }
                }
            };
        }
    }

    /**
     * Dummy Application for the test instrumentation
     */
    public static class CustomApplication extends Application {
        public CustomApplication() {
        }
    }

    public static SimpleDateFormat createRFC2822Formatter() {
        final String pattern = "EEE, dd MMM yyyy HH:mm:ss z";
        final SimpleDateFormat rfc2822formatter = new SimpleDateFormat(pattern, Locale.US);
        rfc2822formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return rfc2822formatter;
    }

    /**
     * Serialize the given {@code map} to a JSON Object, then flattens to {@code Map<String, String>}.
     * For example, a JSON such as "{xdm: {stitchId: myID, eventType: myType}}" is flattened
     * to two map elements "xdm.stitchId" = "myID" and "xdm.eventType" = "myType".
     *
     * @param map map with JSON structure to flatten
     * @return new map with flattened structure
     */
    static Map<String, String> flattenMap(final Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            JSONObject jsonObject = new JSONObject(map);
            Map<String, String> payloadMap = new HashMap<>();
            addKeys("", new ObjectMapper().readTree(jsonObject.toString()), payloadMap);
            return payloadMap;
        } catch (IOException e) {
            Log.error(LOG_TAG, LOG_TAG, "Failed to parse JSON object to tree structure.");
        }

        return Collections.emptyMap();
    }

    /**
     * Deserialize {@code JsonNode} and flatten to provided {@code map}.
     * For example, a JSON such as "{xdm: {stitchId: myID, eventType: myType}}" is flattened
     * to two map elements "xdm.stitchId" = "myID" and "xdm.eventType" = "myType".
     * <p>
     * Method is called recursively. To use, call with an empty path such as
     * {@code addKeys("", new ObjectMapper().readTree(JsonNodeAsString), map);}
     *
     * @param currentPath the path in {@code JsonNode} to process
     * @param jsonNode    {@link JsonNode} to deserialize
     * @param map         {@code Map<String, String>} instance to store flattened JSON result
     * @see <a href="https://stackoverflow.com/a/24150263">Stack Overflow post</a>
     */
    private static void addKeys(String currentPath, JsonNode jsonNode, Map<String, String> map) {
        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";

            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                addKeys(pathPrefix + entry.getKey(), entry.getValue(), map);
            }
        } else if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;

            for (int i = 0; i < arrayNode.size(); i++) {
                addKeys(currentPath + "[" + i + "]", arrayNode.get(i), map);
            }
        } else if (jsonNode.isValueNode()) {
            ValueNode valueNode = (ValueNode) jsonNode;
            map.put(currentPath, valueNode.asText());
        }
    }

    /**
     * Converts a file containing a JSON into a {@link Map<String, Object>}.
     *
     * @param fileName the {@code String} name of a file located in the resource directory
     * @return a {@code Map<String, Object>} containing the file's contents
     */
    static Map<String, Object> getMapFromFile(final String fileName) {
        try {
            final JSONObject json = new JSONObject(loadStringFromFile(fileName));
            return JSONUtils.toMap(json);
        } catch (final JSONException jsonException) {
            Log.warning(LOG_TAG, "getMapFromFile() - Exception occurred when creating the JSONObject: %s", jsonException.getMessage());
            return null;
        }
    }

    /**
     * Converts a file into a {@code String}.
     *
     * @param fileName the {@code String} name of a file located in the resource directory
     * @return a {@code String} containing the file's contents
     */
    public static String loadStringFromFile(final String fileName) {
        final InputStream inputStream = convertResourceFileToInputStream(fileName);
        try {
            if (inputStream != null) {
                final String streamContents = streamToString(inputStream);
                return streamContents;
            } else {
                return null;
            }
        } finally {
            try {
                inputStream.close();
            } catch (final IOException ioException) {
                Log.warning(LOG_TAG, "Exception occurred when closing the input stream: %s", ioException.getMessage());
                return null;
            }
        }
    }

    /**
     * Cleans Messaging extension payload and image asset cache files.
     */
    static void cleanCache() {
        final CacheService cacheService = ServiceProvider.getInstance().getCacheService();
        cacheService.remove(CampaignConstants.CACHE_BASE_DIR, CampaignConstants.RULES_CACHE_FOLDER);
        cacheService.remove(CampaignConstants.CACHE_BASE_DIR, CampaignConstants.MESSAGE_CACHE_DIR);
    }

    /**
     * Adds a test image to the Messaging extension image asset cache.
     */
    static void addImageAssetToCache() {
        final File assetDir = new File(ServiceProvider.getInstance().getDeviceInfoService().getApplicationCacheDir() + File.separator + CampaignConstants.AEPSDK_CACHE_BASE_DIR + File.separator + CampaignConstants.CACHE_BASE_DIR + File.separator
                + CampaignConstants.MESSAGE_CACHE_DIR);
        final InputStream adobePng = convertResourceFileToInputStream("adobe.png");
        final CacheEntry mockCachedImage = new CacheEntry(adobePng, CacheExpiry.never(), null);
        ServiceProvider.getInstance().getCacheService().set(assetDir.getAbsolutePath(), REMOTE_URL, mockCachedImage);
    }

    /**
     * Converts a file in the resources directory into an {@link InputStream}.
     *
     * @param filename the {@code String} filename of a file located in the resource directory
     * @return a {@code InputStream} of the specified file
     */
    static InputStream convertResourceFileToInputStream(final String filename) {
        return TestHelper.class.getClassLoader().getResourceAsStream(filename);
    }

    /**
     * Writes the contents of an {@link InputStream} into a file.
     *
     * @param cachedFile  the {@code File} to be written to
     * @param inputStream a {@code InputStream} containing the data to be written
     * @return a {@code boolean} if the write to file was successful
     */
    static boolean writeInputStreamIntoFile(final File cachedFile, final InputStream inputStream, final boolean append) {
        boolean result = false;

        if (cachedFile == null || inputStream == null) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException ioException) {
                    Log.debug(LOG_TAG, "Exception occurred when closing input stream: %s", ioException.getMessage());
                }
            }
            return result;
        }

        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(cachedFile, append);
            final byte[] data = new byte[STREAM_WRITE_BUFFER_SIZE];
            int count;

            while ((count = inputStream.read(data)) != -1) {
                outputStream.write(data, 0, count);
                outputStream.flush();
            }
            result = true;
        } catch (final IOException e) {
            Log.error(LOG_TAG, LOG_TAG, "IOException while attempting to write to file (%s)", e.getLocalizedMessage());
        } catch (final Exception e) {
            Log.error(LOG_TAG, LOG_TAG, "Unexpected exception while attempting to write to file (%s)", e.getLocalizedMessage());
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }

            } catch (final Exception e) {
                Log.error(LOG_TAG, LOG_TAG, "Unable to close the OutputStream (%s) ", e.getLocalizedMessage());
            }
        }

        return result;
    }

    /**
     * Set the persistence data for Identity extension.
     */
    static void setIdentityPersistence(final Map<String, Object> persistedData, final Application application) {
        if (persistedData != null) {
            final JSONObject persistedJSON = new JSONObject(persistedData);
            updatePersistence("com.adobe.identity",
                    "identity.properties", persistedJSON.toString(), application);
        }
    }

    /**
     * Helper method to update the {@link SharedPreferences} data.
     *
     * @param datastore   the name of the datastore to be updated
     * @param key         the persisted data key that has to be updated
     * @param value       the new value
     * @param application the current test application
     */
    public static void updatePersistence(final String datastore, final String key, final String value, final Application application) {
        if (application == null) {
            Assert.fail("Unable to updatePersistence by TestPersistenceHelper. Application is null, fast failing the test case.");
        }

        final Context context = application.getApplicationContext();

        if (context == null) {
            Assert.fail("Unable to updatePersistence by TestPersistenceHelper. Context is null, fast failing the test case.");
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(datastore, Context.MODE_PRIVATE);

        if (sharedPreferences == null) {
            Assert.fail("Unable to updatePersistence by TestPersistenceHelper. sharedPreferences is null, fast failing the test case.");
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    static Map<String, Object> createIdentityMap(final String namespace, final String id) {
        Map<String, Object> namespaceObj = new HashMap<>();
        namespaceObj.put("authenticationState", "ambiguous");
        namespaceObj.put("id", id);
        namespaceObj.put("primary", false);

        List<Map<String, Object>> namespaceIds = new ArrayList<>();
        namespaceIds.add(namespaceObj);

        Map<String, List<Map<String, Object>>> identityMap = new HashMap<>();
        identityMap.put(namespace, namespaceIds);

        Map<String, Object> xdmMap = new HashMap<>();
        xdmMap.put("identityMap", identityMap);

        return xdmMap;
    }

    static String streamToString(final InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] data = new byte[STREAM_READ_BUFFER_SIZE];
        int bytesRead;

        try {
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }

            return buffer.toString(CHARSET_UTF_8);
        } catch (final IOException ex) {
            Log.debug(LOG_TAG, LOG_TAG, "Unable to convert InputStream to String, %s", ex.getLocalizedMessage());
            return null;
        }
    }

}