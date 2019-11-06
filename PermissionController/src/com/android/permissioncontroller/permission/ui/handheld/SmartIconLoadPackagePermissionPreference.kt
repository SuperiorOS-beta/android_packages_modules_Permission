/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.permissioncontroller.permission.ui.handheld

import android.app.Application
import android.content.Intent
import android.os.UserHandle
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.AndroidResources
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.permissioncontroller.Constants.EXTRA_SESSION_ID
import com.android.permissioncontroller.R
import com.android.permissioncontroller.permission.ui.AppPermissionActivity
import com.android.permissioncontroller.permission.ui.LocationProviderInterceptDialog
import com.android.permissioncontroller.permission.utils.KotlinUtils
import com.android.permissioncontroller.permission.utils.LocationUtils
import com.android.permissioncontroller.permission.utils.Utils

/**
 * A Preference representing a package for a user, which loads and displays its icon only upon
 * being bound to a viewHolder. This lets us synchronously load package icons and labels, while
 * still displaying the PermissionAppsFragment instantly.
 *
 * @param fragment: The current fragment
 * @param packageName: The name of the package whose icon this preference will retrieve
 * @param user: The user whose package icon will be retrieved
 * @param groupName: The name of the permission group this Preference is showing for
 * @param caller: The name of the caller of this constructor. See
 * @see AppPermissionActivity.EXTRA_CALLER_NAME
 * @param sessionId: An int representing the current session
 */
open class SmartIconLoadPackagePermissionPreference @JvmOverloads constructor(
    fragment: Fragment,
    private val packageName: String,
    private val user: UserHandle,
    groupName: String,
    caller: String,
    sessionId: Long = 0
) : Preference(fragment.context) {

    private val app: Application = fragment.activity!!.application!!

    init {
        setOnPreferenceClickListener {
            val userContext = Utils.getUserContext(app, user)
            if (LocationUtils.isLocationGroupAndProvider(userContext, groupName,
                    packageName)) {
                val intent = Intent(userContext, LocationProviderInterceptDialog::class.java)
                intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
                fragment.activity!!.startActivityAsUser(intent, user)
            } else if (LocationUtils.isLocationGroupAndControllerExtraPackage(
                    userContext, groupName, packageName)) {
                // Redirect to location controller extra package settings.
                LocationUtils.startLocationControllerExtraPackageSettings(fragment.activity!!, user)
            } else {
                val intent = Intent(Intent.ACTION_MANAGE_APP_PERMISSION)
                intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
                intent.putExtra(Intent.EXTRA_PERMISSION_GROUP_NAME, groupName)
                intent.putExtra(Intent.EXTRA_USER, user)
                intent.putExtra(AppPermissionActivity.EXTRA_CALLER_NAME, caller)
                intent.putExtra(EXTRA_SESSION_ID, sessionId)
                fragment.context!!.startActivity(intent)
            }
            true
        }
    }

    /**
     * Loads the package's badged icon upon being bound to a viewholder. This allows us to load
     * icons synchronously, because we only load the icons that are visible on the screen.
     */
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val title = holder.findViewById(android.R.id.title) as TextView
        title.maxLines = 1
        title.ellipsize = TextUtils.TruncateAt.END

        val imageView = holder.findViewById(android.R.id.icon) as ImageView

        imageView.maxWidth =
            context.resources.getDimensionPixelSize(R.dimen.secondary_app_icon_size)
        imageView.maxHeight =
            context.resources.getDimensionPixelSize(R.dimen.secondary_app_icon_size)
        imageView.setImageDrawable(KotlinUtils.getBadgedPackageIcon(app, packageName, user))
        imageView.visibility = View.VISIBLE

        var imageFrame: View? = holder.findViewById(R.id.icon_frame)
        if (imageFrame == null) {
            imageFrame = holder.findViewById(AndroidResources.ANDROID_R_ICON_FRAME)
        }
        if (imageFrame != null) {
            imageFrame.visibility = View.VISIBLE
        }
    }
}
