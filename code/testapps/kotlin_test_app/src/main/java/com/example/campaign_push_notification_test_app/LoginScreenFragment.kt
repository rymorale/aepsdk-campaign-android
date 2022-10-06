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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.campaign_push_notification_test_app.databinding.FragmentLoginScreenBinding
import com.example.campaign_push_notification_test_app.storage.PreferenceManager
import java.lang.ref.WeakReference


class LoginScreenFragment : Fragment() {

    private lateinit var eventHandler: EventHandler

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val dataBinding: FragmentLoginScreenBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_login_screen, container, false)
        eventHandler = EventHandler(dataBinding, context as LoginSuccessCallback)
        dataBinding.eventHandler = eventHandler
        return dataBinding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() = LoginScreenFragment()
    }

    class EventHandler(dataBinding: FragmentLoginScreenBinding, loginCallBack: LoginSuccessCallback) {
        private val dataBinding: WeakReference<FragmentLoginScreenBinding> = WeakReference(dataBinding)
        private val loginCallBack = WeakReference<LoginSuccessCallback>(loginCallBack)

        fun onLoginButtonClick(view: View) {
            if (dataBinding.get()?.editTextUsername?.text.toString() == PreferenceManager.getUserName(view.context)
                    && dataBinding.get()?.editTextPassword?.text.toString() == PreferenceManager.getPassword(view.context)) {
                loginCallBack.get()?.onSuccessFulLogin()
            } else {
                Toast.makeText(view.context, "Incorrect credentials", Toast.LENGTH_LONG).show()
            }
        }

        fun onTextChanged(charSequence: CharSequence, before: Int, after: Int, count: Int) {
            val dataBinding: FragmentLoginScreenBinding? = this.dataBinding.get()
            dataBinding?.btnLogin?.isEnabled = isNotEmpty(dataBinding?.editTextPassword) && isNotEmpty(dataBinding?.editTextUsername)
        }

        companion object {
            @JvmStatic
            fun isNotEmpty(editText: EditText?): Boolean = editText?.let {
                it.text.toString() != null && it.text.toString() != ""
            } ?: false
        }
    }

}
