package com.wechat.mm.qbar.camera

import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

@ExperimentalPermissionsApi
@Composable
fun PermissionRequired(
    permissionState: PermissionState,
    permissionNotGrantedContent: @Composable (() -> Unit),
    permissionNotAvailableContent: @Composable (() -> Unit),
    content: @Composable (() -> Unit),
) {
    when {
        permissionState.status.isGranted -> {
            content()
        }

        permissionState.status.shouldShowRationale -> {
            permissionNotGrantedContent()
        }

        else -> {
            permissionNotAvailableContent()
        }
    }
}

/**
 * Composable that exercises the permissions flows as described in the
 * [documentation](https://developer.android.com/training/permissions/requesting#workflow_for_requesting_permissions)
 * when multiple permissions are *required* to be granted for [content].
 *
 * If any permission is not granted and a rationale should be shown, or the user hasn't been
 * presented with the permissions yet, [permissionsNotGrantedContent] will be added to Composition.
 * If the user doesn't want to be asked for permissions again, [permissionsNotAvailableContent]
 * will be added instead.
 *
 * @param multiplePermissionsState required permissions to be granted.
 * @param permissionsNotGrantedContent content to show when the user hasn't granted all permissions.
 * Requesting the permissions to the user is allowed using
 * [MultiplePermissionsState.launchMultiplePermissionRequest] in a side-effect or
 * non-Composable lambda.
 * @param permissionsNotAvailableContent content to show when the permissions are not available.
 * This could be because the user doesn't want to be asked again for these permissions or the
 * permissions are blocked in the device. Attempting to request the permission to the user in
 * this part of Composition has no effect.
 * @param content content to show when all permissions are granted.
 */
@ExperimentalPermissionsApi
@Composable
fun PermissionsRequired(
    multiplePermissionsState: MultiplePermissionsState,
    permissionsNotGrantedContent: @Composable (() -> Unit),
    permissionsNotAvailableContent: @Composable (() -> Unit),
    content: @Composable (() -> Unit),
) {
    when {
        multiplePermissionsState.allPermissionsGranted -> {
            content()
        }

        multiplePermissionsState.shouldShowRationale -> {
            permissionsNotGrantedContent()
        }

        else -> {
            permissionsNotAvailableContent()
        }
    }
}