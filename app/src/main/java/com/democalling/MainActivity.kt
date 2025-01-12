package com.democalling

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.democalling.databinding.ActivityMainBinding
import com.democalling.databinding.ItemUserListDataBinding
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity(), AdapterUserList.callback {

    private lateinit var binding: ActivityMainBinding

    // Define call states
    enum class CallState {
        IDLE, RINGING, IN_CALL, CALL_ENDED
    }

    // Current call state
    private var currentState: CallState = CallState.IDLE

    // Timer variables
    private var callDurationInSeconds = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            callDurationInSeconds++
            updateCallTimer()
            timerHandler.postDelayed(this, 1000) // Run every 1 second
        }
    }

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private lateinit var preview: Preview
    private lateinit var camera: Camera

    // Define request codes
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val AUDIO_PERMISSION_REQUEST_CODE = 1002

    var userList = ArrayList<ListModelClass>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Check permissions when the activity is created
        checkPermissions()

        userList.add(ListModelClass("Abhay Gupta"))
        userList.add(ListModelClass("Sahil Kashyap"))
        userList.add(ListModelClass("Ankit Kashyap"))
        userList.add(ListModelClass("Mokul Jaiswal"))

        val qbankHistoryAdapter = AdapterUserList(
            this,
            userList, this
        )
        binding.rvUserList.adapter = qbankHistoryAdapter

        // Incoming call actions
        binding.acceptCallButton.setOnClickListener {
            Toast.makeText(this, "Call Accepted", Toast.LENGTH_SHORT).show()
            startCall()
        }
        binding.rejectCallButton.setOnClickListener {
            Toast.makeText(this, "Call Declined", Toast.LENGTH_SHORT).show()
            updateUI(CallState.CALL_ENDED)
        }

        // In-call actions
        binding.muteButton.setOnClickListener {
            val isMuted = binding.muteButton.tag == "muted"
            binding.muteButton.setImageResource(if (isMuted) R.drawable.ic_mic else R.drawable.baseline_mic_off_24)
            binding.muteButton.tag = if (isMuted) "unmuted" else "muted"
            Toast.makeText(this, if (isMuted) "Unmuted" else "Muted", Toast.LENGTH_SHORT).show()
        }

        binding.switchCameraButton.setOnClickListener {
            val isCameraMuted = binding.switchCameraButton.tag == "camera Off"
            if (isCameraMuted){
                // Set up CameraX when the activity is created
                setUpCamera("FRONT")
            }else{
                // Set up CameraX when the activity is created
                setUpCamera("BACK")
            }
//            binding.switchCameraButton.setImageResource(if (isCameraMuted) R.drawable.camera else R.drawable.cameraoff)
            binding.switchCameraButton.tag = if (isCameraMuted) "camera On" else "camera Off"
            Toast.makeText(this, "Switching Camera...", Toast.LENGTH_SHORT).show()
        }

        binding.endCallButton.setOnClickListener {
            Toast.makeText(this, "Call Ended", Toast.LENGTH_SHORT).show()
            endCall()
        }

        // Initialize UI to Idle state
        updateUI(CallState.IDLE)


        // Make the API call
        RetrofitClient.apiService.getUsers().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (response.isSuccessful) {
                    // Handle successful response
                    val users = response.body()
                    users?.forEach {
                        println("User: ${it.name}")
                    }
                } else {
                    Toast.makeText(applicationContext, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                // Handle failure
                Toast.makeText(applicationContext, "Failure: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })

        // Make the API call
        RetrofitClient.apiService.getPostById(1).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    val post = response.body()
                    // Handle the post data here
                    post?.let {
                        println("Post Title: ${it.title}")
                        println("Post Body: ${it.body}")
                    }
                } else {
                    Toast.makeText(applicationContext, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                // Handle failure
                Toast.makeText(applicationContext, "Failure: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Cancel the notification
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1) // Use the same ID you used to show the notification

        val callVideo = intent.getStringExtra("CALL_VIDEO")
        val callAudio = intent.getStringExtra("CALL_AUDIO")

        Log.d("Checkkkkk", callVideo.toString() + "-" + callAudio.toString())

        // Handle Accept/Decline Actions
        when (intent.action) {
            "ACCEPT_CALL" -> {
                Toast.makeText(this, "Call Accepted", Toast.LENGTH_SHORT).show()

                if (callAudio.toBoolean()) {
                    startCall(isVideo = callVideo.toBoolean(), isAudio = callAudio.toBoolean())

                } else if (callVideo.toBoolean()) {
                    startCall(isVideo = callVideo.toBoolean(), isAudio = callAudio.toBoolean())
                }
            }

            "DECLINE_CALL" -> {
                Toast.makeText(this, "Call Declined", Toast.LENGTH_SHORT).show()
                updateUI(CallState.CALL_ENDED)  // Transition to Call Ended state
            }
        }
    }


    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    // Start a call
    private fun startCall(
        isVideo: Boolean = false,
        isAudio: Boolean = false,
        isIncommingCall: Boolean = false
    ) {
        callDurationInSeconds = 0 // Reset the timer
        if (isAudio || isVideo) {
            updateUI(CallState.IN_CALL, isVideo, isAudio, isIncommingCall)
        } else if (isIncommingCall) {
            updateUI(CallState.RINGING, isVideo, isAudio, isIncommingCall)
        }

        startCallTimer()
    }

    // End a call
    private fun endCall() {
        stopCallTimer()
        updateUI(CallState.CALL_ENDED)
    }

    // Start the call timer
    private fun startCallTimer() {
        timerHandler.post(timerRunnable)
    }

    // Stop the call timer
    private fun stopCallTimer() {
        timerHandler.removeCallbacks(timerRunnable)
    }

    // Update the call timer UI
    private fun updateCallTimer() {
        val hours = callDurationInSeconds / 3600
        val minutes = (callDurationInSeconds % 3600) / 60
        val seconds = callDurationInSeconds % 60

        val timerText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        binding.callTimer.text = timerText // Update the TextView showing the timer
        binding.callTimerAudio.text = timerText // Update the TextView showing the timer
        binding.callTimerVideo.text = timerText // Update the TextView showing the timer
    }

    // Update UI based on state
    private fun updateUI(
        state: CallState,
        isVideo: Boolean = false,
        isAudio: Boolean = false,
        incommingCall: Boolean = false
    ) {
        currentState = state
        binding.ringingLayout.visibility = View.GONE
        binding.inCallLayout.visibility = View.GONE


        when (state) {
            CallState.IDLE -> {
//                binding.idleLayout.visibility = View.VISIBLE
                binding.callTimer.text = "00:00:00" // Reset timer display
            }

            CallState.RINGING -> {
                binding.ringingLayout.visibility = View.VISIBLE
                binding.relativeLayout.visibility = View.VISIBLE
            }

            CallState.IN_CALL -> {
                binding.inCallLayout.visibility = View.VISIBLE
                if (isAudio == true) {
                    binding.audioPlaceholder.visibility = View.VISIBLE
                    binding.videoPlaceholder.visibility = View.GONE
                    binding.switchCameraButton.visibility = View.GONE
                    binding.callTimerAudio.visibility = View.VISIBLE
                    binding.callTimerVideo.visibility = View.GONE
                } else {
                    if (isVideo == true) {
                        binding.callTimerAudio.visibility = View.GONE
                        binding.callTimerVideo.visibility = View.VISIBLE
                        binding.audioPlaceholder.visibility = View.GONE
                        binding.videoPlaceholder.visibility = View.VISIBLE
                        binding.switchCameraButton.visibility = View.VISIBLE
                    }
                }
                binding.relativeLayout.visibility = View.VISIBLE

                // Set up CameraX when the activity is created
                setUpCamera("FRONT")

            }

            CallState.CALL_ENDED -> {
                binding.relativeLayout.visibility = View.GONE
                binding.rvUserList.visibility = View.VISIBLE
                binding.callTimer.text = "00:00:00" // Reset timer display
                Toast.makeText(this, "Returning to Idle State", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setUpCamera(cameraFlip : String) {
        // Initialize CameraX and bind the camera lifecycle
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Unbind any previously bound use cases
            try {
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to unbind previous camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            // Set up Camera Selector
            cameraSelector = if (cameraFlip == "FRONT") {
                CameraSelector.DEFAULT_FRONT_CAMERA // Use front camera
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA // Use back camera
            }

            // Set up Preview use case
            preview = Preview.Builder().build()

            // Bind the camera to lifecycle
            try {
                camera = cameraProvider.bindToLifecycle(
                    this, // LifecycleOwner (e.g., Activity or Fragment)
                    cameraSelector, // CameraSelector (front or back)
                    preview // Preview use case
                )

                // Set the surface provider to the PreviewView in the layout
                val cameraInfo = camera.cameraInfo
                val surfaceProvider = binding.previewView.createSurfaceProvider(cameraInfo)
                preview.setSurfaceProvider(surfaceProvider)

            } catch (e: Exception) {
                Toast.makeText(this, "Camera initialization failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))

    }

    override fun CallBackCalling(audioCall: Boolean, videoCall: Boolean, inComingCall: Boolean) {
        binding.rvUserList.visibility = View.GONE
        if (audioCall) {
            Toast.makeText(this, "Starting Audio Call...", Toast.LENGTH_SHORT).show()
//            startCall(isVideo = videoCall, isAudio = audioCall)
            showIncomingCallNotification(true, false)
        } else if (videoCall) {
            Toast.makeText(this, "Starting Video Call...", Toast.LENGTH_SHORT).show()
//            startCall(isVideo = videoCall, isAudio = audioCall)
            showIncomingCallNotification(false, true)
        } else if (inComingCall) {
//            startCall(isVideo = videoCall, isAudio = audioCall, isIncommingCall = inComingCall)
            showIncomingCallNotification(false, false)
        }
    }

    private fun checkPermissions() {
        // Check if Camera permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }

        // Check if Audio permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Handle the result of permission requests
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted
                    Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Camera permission denied
                    Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }

            AUDIO_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Audio permission granted
                    Toast.makeText(this, "Audio Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    // Audio permission denied
                    Toast.makeText(this, "Audio Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun showIncomingCallNotification(audio: Boolean, video: Boolean) {
        // Create Notification Manager
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel (required for API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "INCOMING_CALL_CHANNEL",
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Create the notification
        val notification: Notification = NotificationCompat.Builder(this, "INCOMING_CALL_CHANNEL")
            .setContentTitle("Incoming Call")
            .setContentText("You have an incoming call")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(
                android.R.drawable.ic_media_play,
                "Accept",
                getAcceptPendingIntent(audio, video)
            )
            .addAction(android.R.drawable.ic_media_pause, "Decline", getDeclinePendingIntent())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Show notification
        notificationManager.notify(1, notification)
    }

    // Accept Pending Intent
    private fun getAcceptPendingIntent(audio: Boolean, video: Boolean): PendingIntent {
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACCEPT_CALL"
            putExtra("CALL_VIDEO", video.toString())
            putExtra("CALL_AUDIO", audio.toString())
        }
        return PendingIntent.getActivity(this, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // Decline Pending Intent
    private fun getDeclinePendingIntent(): PendingIntent {
        val declineIntent = Intent(this, MainActivity::class.java).apply {
            action = "DECLINE_CALL"
        }
        return PendingIntent.getActivity(this, 0, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


}


class AdapterUserList(
    val ctx: Activity,
    val list: ArrayList<ListModelClass>,
    val callBackVar: callback
) :
    RecyclerView.Adapter<AdapterUserList.ViewHolder>() {

    inner class ViewHolder(val itemBinding: ItemUserListDataBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(lists: ListModelClass) {

            itemBinding.userName.text = lists.userName

            itemBinding.audioCall.setOnClickListener {
                callBackVar.CallBackCalling(true, false, false)
            }

            itemBinding.videoCall.setOnClickListener {

                callBackVar.CallBackCalling(false, true, false)
            }

            itemBinding.inCommingCall.setOnClickListener {

                callBackVar.CallBackCalling(false, false, true)
            }

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdapterUserList.ViewHolder {
        val itemBinding = ItemUserListDataBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: AdapterUserList.ViewHolder, position: Int) {
        with(holder) {
            bind(list[position])
        }
    }

    override fun getItemCount(): Int = list.size

    interface callback {
        fun CallBackCalling(audioCall: Boolean, videoCall: Boolean, incommingCall: Boolean)
    }


}