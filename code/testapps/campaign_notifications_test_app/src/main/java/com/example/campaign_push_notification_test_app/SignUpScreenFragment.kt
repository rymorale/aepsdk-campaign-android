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

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.adobe.marketing.mobile.MobileCore
import com.example.campaign_push_notification_test_app.databinding.FragmentSignUpScreenBinding
import com.example.campaign_push_notification_test_app.storage.PreferenceManager
import java.lang.ref.WeakReference


class SignUpScreenFragment : Fragment() {

    private lateinit var eventHandler: EventHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        var dataBinding = DataBindingUtil.inflate<FragmentSignUpScreenBinding>(inflater, R.layout.fragment_sign_up_screen, container, false)
        eventHandler = EventHandler(dataBinding, context as LoginSuccessCallback)
        dataBinding.eventHandler = eventHandler
        return dataBinding.root
    }

    private fun sendPIIData(){

    }

    private fun saveUserNamePassword(){

    }

    companion object {

        @JvmStatic
        fun newInstance() = SignUpScreenFragment()
    }

    class EventHandler(fragmentSignUpScreenBinding: FragmentSignUpScreenBinding, loginCallback: LoginSuccessCallback) {

        private val dataBinding = WeakReference(fragmentSignUpScreenBinding)
        private val loginCallBack = WeakReference(loginCallback)

        fun onSignUpButtonClick(view: View) {

            PreferenceManager.saveUserNamePassword(view.context, dataBinding.get()?.userNameEditText?.text.toString(), dataBinding.get()?.passwordEditText?.text.toString())
            //Send PII data to mobile core.
            val piiData = mapOf("cusEmail" to (dataBinding.get()?.userNameEditText?.text.toString()), "cusFirstName" to (dataBinding.get()?.firstNameEditText?.text.toString()), "cusLastName" to (dataBinding.get()?.lastNameEditText?.text.toString()), "triggerKey" to "collectPIIIOS")
            MobileCore.collectPii(piiData)
            loginCallBack.get()?.onSuccessFulLogin()
        }

        fun onTextChanged(charSequence: CharSequence, before: Int, after: Int, count: Int) {
            val dataBinding = this.dataBinding.get()
            dataBinding?.buttonSignUp?.isEnabled = isNotEmpty(dataBinding?.firstNameEditText) && isNotEmpty(dataBinding?.lastNameEditText) && isNotEmpty(dataBinding?.userNameEditText) && isNotEmpty(dataBinding?.passwordEditText)
        }

        companion object{
            @JvmStatic
            fun isNotEmpty(editText: EditText?): Boolean = editText?.let {
                it.text.toString() != null && it.text.toString() != ""
            } ?: false
        }
    }
}
