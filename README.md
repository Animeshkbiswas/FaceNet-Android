# On-Device Face Recognition for Android

> A hackathon-ready Android app that performs face recognition fully on device by comparing FaceNet embeddings against a local vector database.

<img src="Screenshot%202026-06-04%20200610.png" width="80%"/>

## Quick Pitch

- Fully on-device face recognition, so no cloud processing is required.
- Face embeddings are stored locally with ObjectBox.
- Supports MLKit or MediaPipe face detection.
- Includes optional spoof detection and latency metrics.

## Demo Link For Judges

Share the GitHub Releases page for your APK: [Releases · Animeshkbiswas/FaceNet-Android](https://github.com/Animeshkbiswas/FaceNet-Android/releases)

If you want a smoother demo flow, add a QR code that points to the same release page.

## How To Test The App

1. Open the GitHub Releases page.
2. Download the latest APK.
3. Install it on an Android device.
4. If Android warns about unknown apps, allow installs for your browser or file manager.
5. Open the app, add face images, and start recognition from the camera screen.

## Build From Source

```bash
git clone --depth=1 <your-repository-url>
```

Then open the project in Android Studio, let Gradle sync finish, and run the app.

## What It Does

1. Uses a face detector to crop faces from selected images and camera frames.
2. Converts each cropped face into a FaceNet embedding.
3. Stores embeddings locally in ObjectBox.
4. Compares the live camera embedding against stored embeddings using cosine similarity.
5. Shows recognition results directly on the device.

## Tech Stack

- TensorFlow Lite for FaceNet inference
- MLKit or MediaPipe for face detection
- ObjectBox for local vector search and storage

## Hackathon Upload Steps For GitHub

1. Build a release APK from Android Studio or with Gradle.
2. Find the APK in `app/build/outputs/apk/release/`.
3. Go to your GitHub repository and open the Releases tab.
4. Click Draft a new release.
5. Create a version tag such as `v1.0.0`.
6. Upload the APK file to the release.
7. Add a short release title and a one-line description.
8. Publish the release.
9. Share the release URL with judges, or place it behind a QR code in your presentation.

## Suggested Hackathon Slide Copy

"An Android app that recognizes faces on-device using FaceNet embeddings and local vector search, with no server round-trip."

## Notes

If you want to experiment with the model or detector settings, the main options are in the app source under FaceNet, ObjectBox search, and the detector module.
