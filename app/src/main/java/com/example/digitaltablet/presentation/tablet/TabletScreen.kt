package com.example.digitaltablet.presentation.tablet

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.example.digitaltablet.R
import com.example.digitaltablet.presentation.Dimens.LargeFontSize
import com.example.digitaltablet.presentation.Dimens.MediumFontSize
import com.example.digitaltablet.presentation.Dimens.SmallFontSize
import com.example.digitaltablet.presentation.Dimens.SmallPadding
import com.example.digitaltablet.presentation.tablet.component.ClickableCanvas
import com.example.digitaltablet.presentation.tablet.component.PlayerCommand
import com.example.digitaltablet.presentation.tablet.component.ScrollableCaption
import com.example.digitaltablet.presentation.tablet.component.YouTubePlayer
import com.example.digitaltablet.util.ToastManager
import com.example.digitaltablet.util.createImageFile
import com.example.digitaltablet.util.getFileName
import com.example.digitaltablet.util.toFile

import com.example.digitalrobot.presentation.robot.RobotState
import com.example.digitalrobot.presentation.robot.RobotEvent
import com.example.digitalrobot.presentation.robot.RobotBodyPart
import com.example.digitalrobot.presentation.robot.RobotScreen
import kotlinx.coroutines.delay
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.digitalrobot.presentation.robot.component.TouchArea
import com.example.digitalrobot.presentation.robot.component.VideoPlayer

@Composable
fun TabletScreen(
    state: TabletState,
    robotState: RobotState, // add `RobotViewModel` state
    onEvent: (TabletEvent) -> Unit,
    onRobotEvent: (RobotEvent) -> Unit, // let `TabletScreen` can trigger `RobotEvent`
    navigateToScanner: () -> Unit,
    navigateUp: () -> Unit
) {
    val context = LocalContext.current

    val tabletDeviceId = state.deviceId // Tablet deviceId
    val robotDeviceId = robotState.deviceId // Robot deviceId

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    // Image Upload
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { it ->
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(it)
            if ( mimeType?.startsWith("image/") == true) {
                onEvent(TabletEvent.UploadImage(it.toFile(context)))
            } else {
                onEvent(TabletEvent.UploadImage(null))
            }
        }
    }

    // Camera & Photo Upload
    var hasCameraPermission by remember { mutableStateOf(false) }
    val photoUri = remember {
        context.contentResolver.createImageFile("temp_photo_${System.currentTimeMillis()}.jpg")
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ){ success ->
        if (success) {
            onEvent(TabletEvent.UploadImage(photoUri?.toFile(context)) { file ->
                file.delete()
            })
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            onEvent(TabletEvent.UploadFile(uri.toFile(context)))
        }
    }

    // grid touch event
    var showRobotDialog by remember { mutableStateOf(false) }

    state.toastMessages.let {
        if (it.isNotEmpty()) {
            for (msg in it){
                ToastManager.showToast(context, msg)
            }
            onEvent(TabletEvent.ClearToastMsg)
        }
    }

    BackHandler {
        navigateUp()
    }

    LaunchedEffect(Unit) {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        launcher.launch(Manifest.permission.RECORD_AUDIO)
//        onEvent(TabletEvent.ConnectMqttBroker)
        onRobotEvent(RobotEvent.InitTTS(context))
    }

    LaunchedEffect(tabletDeviceId) {
        if (tabletDeviceId.isNotEmpty() && tabletDeviceId != robotDeviceId) {
            onRobotEvent(RobotEvent.SetConnectInfos(deviceId = tabletDeviceId))
            onRobotEvent(RobotEvent.ConnectMqttBroker)
            delay(2000)
            onEvent(TabletEvent.ConnectMqttBroker)
        }
    }

    Column (
        modifier = Modifier
            .padding(SmallPadding)
            .fillMaxSize()
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .weight(7f)
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(SmallPadding)
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                IconButton(
                    modifier = Modifier
                        .padding(SmallPadding)
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = { navigateUp() }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_home),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    modifier = Modifier
                        .padding(SmallPadding)
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = { imagePickerLauncher.launch("image/*") }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_image),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    modifier = Modifier
                        .padding(SmallPadding)
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = {
                        if (hasCameraPermission) {
                            photoUri?.let { cameraLauncher.launch(it) }
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_camera),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    modifier = Modifier
                        .padding(SmallPadding)
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = { onEvent(TabletEvent.ShowDialog) }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_keyboard),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    modifier = Modifier
                        .padding(SmallPadding)
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_attachment),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IconButton(
                    modifier = Modifier
                        .padding(SmallPadding)
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = {
                        if (hasCameraPermission) {
                            navigateToScanner()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_qrcode),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                //simulate robot touch
                IconButton(
                    modifier = Modifier
                        .padding(SmallPadding)
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = {
                        showRobotDialog = true
                    }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_kebbi),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (state.isCaptionVisible) {
                Column (
                    modifier = Modifier
                        .padding(SmallPadding)
                        .fillMaxHeight()
                        .weight(
                            if (state.isImageVisible) 3f
                            else 8f
                        )
                ) {
                    ScrollableCaption(caption = state.caption)
                }
            }

            if (state.isImageVisible) {
                Column (
                    modifier = Modifier
                        .padding(SmallPadding)
                        .fillMaxHeight()
                        .weight(
                            if (state.isCaptionVisible) 5f
                            else 8f
                        )
                ) {
                    Text(
                        text = if (state.mediaIdx != null) {
                            val text = state.mediaSources[state.mediaIdx]
                            if (text.contains("http")) text
                            else getFileName(context.contentResolver, text.toUri()) ?: "Unknown"
                        } else "",
                        fontSize = SmallFontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(top = SmallPadding)
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .padding(bottom = SmallPadding)
                            .fillMaxWidth()
                            .weight(9f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.mediaIdx != null &&
                            state.mediaIdx in state.mediaSources.indices
                        ) {
                            val media = state.mediaSources[state.mediaIdx]
                            if (media.contains("youtube") ||
                                media.contains("youtu.be")
                            ) {
                                YouTubePlayer(
                                    videoUrl = media,
                                    playerCommand = state.playerCommand,
                                    modifier = Modifier.fillMaxSize().aspectRatio(16f / 9f)
                                ) { onEvent(TabletEvent.ClearPlayerCommand) }
                            } else {
                                ClickableCanvas(
                                    imageUri = media,
                                    tapPositions = state.canvasTapPositions,
                                    tappable = state.isCanvasTappable,
                                    modifier = Modifier.fillMaxSize(),
                                    onTap = { tapPosition ->
                                        onEvent(TabletEvent.TapOnCanvas(tapPosition))
                                    },
                                    onRatioChanged = { ratio ->
                                        onEvent(TabletEvent.ChangeCanvasRatio(ratio))
                                    }
                                )
                            }
                        }
                    }
                    Row (
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Button(
                            onClick = {
                                onEvent(TabletEvent.SwitchImage(
                                    page = state.mediaIdx?.minus(1) ?: 0)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.large,
                            enabled = state.mediaIdx in 1 until state.mediaSources.size ,
                            modifier = Modifier
                                .padding(end = SmallPadding)
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            Text(text = "<", fontSize = MediumFontSize)
                        }
                        Button(onClick = { onEvent(TabletEvent.ClearCanvas) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.large,
                            enabled = state.isCanvasTappable && state.mediaIdx != null,
                            modifier = Modifier
                                .padding(horizontal = SmallPadding)
                                .fillMaxHeight()
                                .weight(3f)
                        ) {
                            Text(text = "Clear", fontSize = MediumFontSize)
                        }
                        Button(onClick = { onEvent(TabletEvent.SubmitCanvas(context = context)) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.large,
                            enabled = state.isCanvasTappable && state.mediaIdx != null,
                            modifier = Modifier
                                .padding(horizontal = SmallPadding)
                                .fillMaxHeight()
                                .weight(3f)
                        ) {
                            Text(text = "Submit", fontSize = MediumFontSize)
                        }
                        Button(onClick = {
                            onEvent(TabletEvent.SwitchImage(
                                page = state.mediaIdx?.plus(1) ?: 0)
                            )
                        },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.large,
                            enabled = state.mediaIdx in 0 until state.mediaSources.size - 1,
                            modifier = Modifier
                                .padding(start = SmallPadding)
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            Text(text = ">", fontSize = MediumFontSize)
                        }
                    }
                }

            }

        }

        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Button(onClick = { onEvent(TabletEvent.ToggleCaptionVisibility) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.large,
                enabled = state.isImageVisible && state.displayOn,
                modifier = Modifier
                    .padding(SmallPadding)
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                Text(
                    text = if (state.isCaptionVisible) "Hide Textbox" else "Show Textbox",
                    fontSize = MediumFontSize
                )
            }

            Button(onClick = { onEvent(TabletEvent.ToggleImageVisibility) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.large,
                enabled = state.isCaptionVisible,
                modifier = Modifier
                    .padding(SmallPadding)
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                Text(
                    text = if (state.isImageVisible) "Hide Image" else "Show Image",
                    fontSize = MediumFontSize
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.responseCaption,
                fontSize = LargeFontSize,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    // Text input dialog
    if (state.showTextDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(TabletEvent.DismissDialog) },
            title = { Text(text = "Enter response") },
            text = {
                TextField(
                    value = state.dialogTextInput,
                    onValueChange = { onEvent(TabletEvent.ChangeDialogTextInput(it)) },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row (
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onEvent(TabletEvent.ConfirmDialog) }
                    ) {
                        Text(text = "OK")
                    }
                    Button(onClick = { onEvent(TabletEvent.DismissDialog) } ) {
                        Text(text = "Cancel")
                    }
                }
            }
        )
    }

    // display robot dialog
    if (showRobotDialog) {
        Dialog(
            onDismissRequest = { showRobotDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .width(800.dp)
                    .height(500.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val screenWidth = with(density) { constraints.maxWidth.toDp() }
                    val screenHeight = with(density) { constraints.maxHeight.toDp() }

                    VideoPlayer(
                        videoResId = R.raw.m_idle,
                        repeat = false,
                        modifier = Modifier.fillMaxSize()
                    )

                    VideoPlayer(
                        videoResId = R.raw.e_smile,
                        repeat = true,
                        modifier =
                            Modifier
                                .offset(
                                    x = screenWidth * 0.4f,
                                    y = screenHeight * 0.195f
                                )
                                .fillMaxWidth(0.215f)
                                .fillMaxHeight(0.215f)
                    )

                    // touch area
                    Box(modifier = Modifier.fillMaxSize()) {
                        val touchAreaColor = Color.Red.copy(alpha = 0.3f)

                        TouchArea(
                            heightPercent = 0.1f,
                            widthPercent = 0.3f,
                            color = touchAreaColor,
                            offsetXPercent = 0.35f,
                            offsetYPercent = 0.05f
                        ) {
                            onRobotEvent(RobotEvent.TapBodyPart(RobotBodyPart.HEAD))
                            showRobotDialog = false
                        }

                        TouchArea(
                            heightPercent = 0.3f,
                            widthPercent = 0.2f,
                            color = touchAreaColor,
                            offsetXPercent = 0.4f,
                            offsetYPercent = 0.65f
                        ) {
                            onRobotEvent(RobotEvent.TapBodyPart(RobotBodyPart.CHEST))
                            showRobotDialog = false
                        }

                        TouchArea(
                            heightPercent = 0.1f,
                            widthPercent = 0.075f,
                            color = touchAreaColor,
                            offsetXPercent = 0.275f,
                            offsetYPercent = 0.8f
                        ) {
                            onRobotEvent(RobotEvent.TapBodyPart(RobotBodyPart.RIGHT_HAND))
                            showRobotDialog = false
                        }

                        TouchArea(
                            heightPercent = 0.1f,
                            widthPercent = 0.075f,
                            color = touchAreaColor,
                            offsetXPercent = 0.675f,
                            offsetYPercent = 0.8f
                        ) {
                            onRobotEvent(RobotEvent.TapBodyPart(RobotBodyPart.LEFT_HAND))
                            showRobotDialog = false
                        }

                        TouchArea(
                            heightPercent = 0.25f,
                            widthPercent = 0.05f,
                            color = touchAreaColor,
                            offsetXPercent = 0.62f,
                            offsetYPercent = 0.2f
                        ) {
                            onRobotEvent(RobotEvent.TapBodyPart(RobotBodyPart.LEFT_FACE))
                            showRobotDialog = false
                        }

                        TouchArea(
                            heightPercent = 0.25f,
                            widthPercent = 0.05f,
                            color = touchAreaColor,
                            offsetXPercent = 0.35f,
                            offsetYPercent = 0.2f
                        ) {
                            onRobotEvent(RobotEvent.TapBodyPart(RobotBodyPart.RIGHT_FACE))
                            showRobotDialog = false
                        }

                        TouchArea(
                            heightPercent = 0.1f,
                            widthPercent = 0.2f,
                            color = Color.Transparent,
                            offsetXPercent = 0.4f,
                            offsetYPercent = 0.45f
                        ) {
                            onRobotEvent(RobotEvent.ToggleTouchAreaDisplay)
                            showRobotDialog = false
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewScreen() {
    TabletScreen(state = TabletState(), robotState = RobotState(), {}, {}, {}) {

    }
}