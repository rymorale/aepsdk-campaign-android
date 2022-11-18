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

package com.example.campaign_push_notification_test_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adobe.marketing.mobile.*
import com.example.campaign_push_notification_test_app.models.ShoppingItem
import com.example.campaign_push_notification_test_app.storage.PreferenceManager
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private var progressBar: ProgressBar? = null
    private var shoppingItemList = ArrayList<ShoppingItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.also { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setDisplayShowTitleEnabled(false)
        }

        findViewById<Button>(R.id.checkout).setOnClickListener {
            val selectedItemsList = arrayListOf<ShoppingItem>()
            for (shoppingItem in shoppingItemList) {
                if (shoppingItem.isChecked) {
                    selectedItemsList.add(shoppingItem)
                }
            }
            val checkOutIntent = Intent(this, CheckoutActivity::class.java)
            checkOutIntent.putParcelableArrayListExtra("selectedItems", selectedItemsList)
            startActivity(checkOutIntent)
        }

        progressBar = ProgressBar(this)
        showLoader()
        Thread {
            val json = parseShoppingListDataFromAssets()
            var jsonObject = JSONObject(json)
            var jsonArr = jsonObject.getJSONArray("details")
            val len = jsonArr.length()
            shoppingItemList.clear()
            for (i in 0 until len) {
                shoppingItemList.add(ShoppingItem(jsonArr.getJSONObject(i).getString("title"), jsonArr.getJSONObject(i).getString("description")))
            }

            runOnUiThread {
                hideLoader()
                setUpRecyclerViewData()
            }
        }.start()

        //AndroidGriffonBridge.startSession("com.campaign://?adb_validation_sessionid=53103065-ce8c-470e-a514-95d68808a0a6")
        var linkageField = mapOf("cusEmail" to PreferenceManager.getUserName(this), "triggerKey" to "collectPIIIOS")
        Campaign.setLinkageFields(linkageField)

        //PlacesMonitor.start()

    }

    private fun showLoader() {

        progressBar?.setVisibility(View.VISIBLE)
    }

    private fun hideLoader() {

        progressBar?.setVisibility(View.INVISIBLE)
    }

    private fun parseShoppingListDataFromAssets(): String? {

        var inputStream: InputStream? = null
        var inputStreamReader: BufferedInputStream? = null
        try {
            inputStream = assets.open("details.json")
            inputStreamReader = BufferedInputStream(inputStream)
            var arr = inputStreamReader.readBytes()
            return String(arr)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStreamReader?.close()
            inputStream?.close()
        }
        return ""
    }

    private fun setUpRecyclerViewData() {

        val recyclerViewShoppingList = findViewById<RecyclerView>(R.id.recyclerViewShoppingList)
        recyclerViewShoppingList.addItemDecoration(
                DividerItemDecoration(this, (recyclerViewShoppingList.layoutManager as LinearLayoutManager).orientation).let {
                    ContextCompat.getDrawable(this, R.drawable.recycler_view_divider)?.apply {
                        it.setDrawable(this)
                    }
                    return@let it
                }
        )

        var shoppingListAdapter = ShoppingListAdapter(shoppingItemList)
        recyclerViewShoppingList.adapter = shoppingListAdapter
    }

    override fun onResume() {
        MobileCore.lifecycleStart(null)
        super.onResume()
    }

    override fun onPause() {
        MobileCore.lifecyclePause()
        super.onPause()
    }

    override fun onDestroy() {
        //AndroidGriffonBridge.endSession()
        super.onDestroy()
    }

}
