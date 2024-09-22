package com.anwaralqam.playlistsyncer;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText inputBox;
    private Button submitButton;
    private ExecutorService executorService;
    private String finalFilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // Find the views by their ID
        inputBox = findViewById(R.id.inputBox);
        submitButton = findViewById(R.id.submitButton);

        // Initialize ExecutorService with a single thread
        executorService = Executors.newSingleThreadExecutor();

        // Set an OnClickListener for the button
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the text from the input box
                String inputText = inputBox.getText().toString();

                // Disable the button after clicking to prevent multiple submissions
                submitButton.setEnabled(false);

                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        // Get the Python instance
                        Python py = Python.getInstance();

                        // Define the completion callback as a Runnable
                        Runnable completionCallback = new Runnable() {
                            @Override
                            public void run() {
                                // Run on the UI thread to avoid crashes when interacting with UI elements
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Retrieve the final filename from the global variable in Python
                                        PyObject pyFinalFilename = py.getModule("yt-downloader").get("final_filename");
                                        finalFilename = pyFinalFilename.toString();
                                        // Print or update the UI with the final filename

                                        Log.d("Download", "Download completed! Final filename: " + finalFilename);

                                        runOnUiThread(() -> {
                                            submitButton.setEnabled(true);
                                        });
                                    }
                                });
                            }
                        };
                        String folderPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "playlist-syncer").getAbsolutePath();
                        // Call the Python function and pass the URL, path, and the Runnable callback
                        py.getModule("yt-downloader")
                                .callAttr("download_video_audio", inputText, folderPath, completionCallback);
                    }
                });
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}