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

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import com.adobe.marketing.mobile.MobileCore
import com.example.campaign_push_notification_test_app.models.ShoppingItem


class CheckoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        val selectedItemList: List<ShoppingItem> = intent?.getParcelableArrayListExtra("selectedItems") ?: arrayListOf()
        findViewById<TextView>(R.id.purchasedItemsTextView).text = "You have selected following products:\n\n${getString(selectedItemList)}"

        findViewById<Button>(R.id.buttonBuy).setOnClickListener {
            MobileCore.trackAction("confirm_order", null)
        }

        findViewById<Button>(R.id.buttonCancel).setOnClickListener {
            MobileCore.trackAction("cancel_order", null)
        }
    }

    private fun getString(shoppingList: List<ShoppingItem>): String {
        val sb = StringBuffer()
        repeat(shoppingList.size) {
            sb.append(shoppingList[it].title + "\n")
        }
        return sb.toString()
    }

    override fun onResume() {
        MobileCore.lifecycleStart(null)
        super.onResume()
    }

    override fun onPause() {
        MobileCore.lifecyclePause()
        super.onPause()
    }
}
