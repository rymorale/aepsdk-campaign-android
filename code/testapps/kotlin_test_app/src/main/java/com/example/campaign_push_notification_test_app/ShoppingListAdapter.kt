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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campaign_push_notification_test_app.models.ShoppingItem
import com.example.campaign_push_notification_test_app.utils.BitmapCache

class ShoppingListAdapter(val data: List<ShoppingItem>?) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingItemViewHolder>(){

    override fun getItemCount() = data?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder =
        ShoppingItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.shopping_list_row, parent, false))

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        holder.selectionCheckBox.isChecked = data?.get(position)?.isChecked!!
        holder.itemImage.setImageBitmap(BitmapCache.getBitmap("image${position + 1}.jpg", holder.itemView.context))
        holder.detailsTextView.text = "${data?.get(position)?.title}\n${data?.get(position)?.description}"
    }

    inner class ShoppingItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var selectionCheckBox: CheckBox = view.findViewById(R.id.selectionCheckBox)
        var itemImage: ImageView = view.findViewById(R.id.itemImage)
        var detailsTextView: TextView = view.findViewById(R.id.itemDetailsTextView)

        init {
            selectionCheckBox.setOnCheckedChangeListener{ _, isChecked ->
                data?.get(adapterPosition)?.isChecked = isChecked
            }
        }
    }
}