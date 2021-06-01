package com.dualfie.maindirs.ui.frag;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.cameraview.fragment.camera.CameraFragment;
import com.dualfie.maindirs.R;
import com.dualfie.maindirs.listener.ShutterMessageControl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CamFragment extends CameraFragment {
    private static final String TAG = "CameraSample";
    ShutterMessageControl mreceiver;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return View.inflate(getContext(), R.layout.fragment_main, container);

    }

   public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initReceiver();
        super.onViewCreated(view, savedInstanceState);

    }
    private void initReceiver() {
        mreceiver = new ShutterMessageControl();
        mreceiver.start(this.getContext());
    }
    private void sendMessage(String string1) throws IOException {
        mreceiver.sendMessage(0, string1, this.getContext());
    }
    private void sendImage(File file) throws IOException {
        mreceiver.sendMessage( 1, file.toString(), this.getContext());
    }


    @SuppressLint("StaticFieldLeak")
    @Override
    public void onImageCaptured(final File file) {
        new FileTransferAsyncTask() {
            @Override
            protected void onPostExecute(File file) {
                report("Picture saved to " + file.getAbsolutePath());
                try {
                    sendImage(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.execute(file, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onVideoCaptured(final File file) {
        new FileTransferAsyncTask() {
            @Override
            protected void onPostExecute(File file) {
                report("Video saved to " + file.getAbsolutePath());
            }
        }.execute(file, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
    }

    @Override
    protected void onRecordStart() {
        report("Recording");
    }

    @Override
    public void onFailure() {
        report("Failure");
    }

    private void report(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }


    private static class FileTransferAsyncTask extends AsyncTask<File, Void, File> {
        @Override
        protected File doInBackground(File... params) {
            try {
                return moveFile(params[0], params[1]);
            } catch (IOException e) {
                e.printStackTrace();
                cancel(true);
            }
            return null;
        }




        private static File moveFile(File file, File dir) throws IOException {
            File newFile = new File(dir, file.getName());
            FileChannel outputChannel = null;
            FileChannel inputChannel = null;
            try {
                if (newFile.exists()) {
                    Log.w(TAG, "File " + newFile + " already exists. Replacing.");
                    if (!newFile.delete()) {
                        throw new IOException("Failed to delete destination file.");
                    }
                }
                outputChannel = new FileOutputStream(newFile).getChannel();
                inputChannel = new FileInputStream(file).getChannel();
                inputChannel.transferTo(0, inputChannel.size(), outputChannel);
                inputChannel.close();
                if (!file.delete()) {
                    throw new IOException("Failed to delete original file.");
                }
            } finally {
                if (inputChannel != null) inputChannel.close();
                if (outputChannel != null) outputChannel.close();
            }
            return newFile;
        }
    }
}
