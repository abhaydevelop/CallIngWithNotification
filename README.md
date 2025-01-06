# MainActivity and AdapterUserList Documentation

## README

### Steps to Run the App:
1. **Clone or Download the Project**:
   - Clone the repository to your local machine or download the zip file and extract it.

2. **Open the Project**:
   - Open the project in Android Studio (recommended version Arctic Fox or later).

3. **Sync Gradle**:
   - Allow Gradle to sync and ensure all dependencies are resolved.

4. **Enable Edge-to-Edge Display**:
   - Enable the feature in developer options on your device/emulator if necessary.

5. **Run the App**:
   - Connect a physical device or start an emulator.
   - Click the **Run** button in Android Studio or use the shortcut `Shift + F10`.

6. **Grant Permissions**:
   - When prompted, grant the app permissions to access the camera and audio recording.

7. **Start Testing**:
   - Interact with the user list to initiate audio, video, or incoming calls.

### Summary of Implemented Features:

#### MainActivity:
1. **Call States Management**:
   - The app manages various states like IDLE, RINGING, IN_CALL, and CALL_ENDED to reflect the current call status.

2. **Call Timer**:
   - Displays a live timer for active calls.

3. **Permissions Handling**:
   - Prompts for camera and audio recording permissions.

4. **CameraX Integration**:
   - Utilizes CameraX to enable the front camera for video calls.

5. **Notification Support**:
   - Sends actionable notifications for incoming calls with Accept and Decline options.

6. **UI Interactions**:
   - Buttons for accepting/rejecting calls, muting audio, switching cameras, and ending calls.

#### AdapterUserList:
1. **RecyclerView Adapter**:
   - Displays a list of users.

2. **Custom ViewHolder**:
   - Binds user data to the layout using `ItemUserListDataBinding`.

3. **Callbacks**:
   - Handles button clicks for initiating audio calls, video calls, and incoming calls via a callback interface.

#### Data Models:
- **ListModelClass**:
  - Represents user data with properties like `userName`.

#### Additional Notes:
- This implementation relies on CameraX for video preview and lifecycle binding.
- Notifications for incoming calls are customizable in the `showIncomingCallNotification()` method.
