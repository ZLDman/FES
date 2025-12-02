package com.example.sco;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class EditConfigDialogFragment extends DialogFragment {

    // Keep the interface the same
    public interface EditConfigDialogListener {
        void onFinishEditDialog(int stepTiltThreshold, int stepTiltRateThreshold, int lockTiltThreshold, int lockKneeAngleThreshold, int lockKneeAngleRateThreshold, int lockTime);
    }

    private SeekBar stepTiltThreshold;
    private TextView stepTiltThresholdValue;
    private SeekBar stepTiltRateThreshold;
    private TextView stepTiltRateThresholdValue;
    private SeekBar lockTiltThreshold;
    private TextView lockTiltThresholdValue;
    private SeekBar lockKneeAngleThreshold;
    private TextView lockKneeAngleThresholdValue;
    private SeekBar lockKneeAngleRateThreshold;
    private TextView lockKneeAngleRateThresholdValue;
    private SeekBar lockTime;
    private TextView lockTimeValue;

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_config, null);

        //Step Initiaion Thresholds
        stepTiltThreshold = view.findViewById(R.id.dialogStepTiltThreshold);
        stepTiltThresholdValue = view.findViewById(R.id.dialogStepTiltThresholdValue);
        stepTiltRateThreshold = view.findViewById(R.id.dialogStepTiltRateThreshold);
        stepTiltRateThresholdValue = view.findViewById(R.id.dialogStepTiltRateThresholdValue);

        //Knee Lock Thresholds
        lockTiltThreshold = view.findViewById(R.id.dialogLockTiltThreshold);
        lockTiltThresholdValue = view.findViewById(R.id.dialogLockTiltThresholdValue);
        lockKneeAngleThreshold = view.findViewById(R.id.dialogLockKneeAngleThreshold);
        lockKneeAngleThresholdValue = view.findViewById(R.id.dialogLockKneeAngleThresholdValue);
        lockKneeAngleRateThreshold = view.findViewById(R.id.dialogLockKneeAngleRateThreshold);
        lockKneeAngleRateThresholdValue = view.findViewById(R.id.dialogLockKneeAngleRateThresholdValue);
        lockTime = view.findViewById(R.id.dialogLockTime);
        lockTimeValue = view.findViewById(R.id.dialogLockTimeValue);


        // --- Set up SeekBar listeners to update the text views ---
        setupSeekBarListeners();

        // --- Retrieve and display current values passed from the activity ---
        Bundle args = getArguments();
        if (args != null) {
            int currentStepTiltThreshold = args.getInt("stepTiltThreshold");
            int currentStepTiltRateThreshold = args.getInt("stepTiltRateThreshold");

            int currentLockTiltThreshold = args.getInt("lockTiltThreshold");
            int currentLockKneeAngleThreshold = args.getInt("lockKneeAngleThreshold");
            int currentLockKneeAngleRateThreshold = args.getInt("lockKneeAngleRateThreshold");
            int currentLockTime = args.getInt("lockTime");
//TODO Write Equations for these values
            stepTiltThreshold.setProgress(currentStepTiltThreshold);
            stepTiltThresholdValue.setText(String.format("%ddeg", currentStepTiltThreshold));

            stepTiltRateThreshold.setProgress(currentStepTiltRateThreshold);
            stepTiltRateThresholdValue.setText(String.format("%ddeg/sec", currentStepTiltRateThreshold));

            lockTiltThreshold.setProgress(currentLockTiltThreshold + 20);
            lockTiltThresholdValue.setText(String.format("%ddeg", currentLockTiltThreshold));

            lockKneeAngleThreshold.setProgress(currentLockKneeAngleThreshold);
            lockKneeAngleThresholdValue.setText(String.format("%ddeg", currentLockKneeAngleThreshold));

            lockKneeAngleRateThreshold.setProgress(currentLockKneeAngleRateThreshold);
            lockKneeAngleRateThresholdValue.setText(String.format("%ddeg/sec", currentLockKneeAngleRateThreshold));

            lockTime.setProgress(currentLockTime - 500);
            lockTimeValue.setText(String.format("%dms", currentLockTime));
        }

        // --- Build the dialog ---
        builder.setView(view)
                .setTitle("Edit Configuration")
                .setPositiveButton("Save", (dialog, id) -> {
                    // Send the final progress values back to the activity
                    EditConfigDialogListener listener = (EditConfigDialogListener) getActivity();
                    if (listener != null) {
                        listener.onFinishEditDialog(stepTiltThreshold.getProgress(), stepTiltRateThreshold.getProgress(), lockTiltThreshold.getProgress(), lockKneeAngleThreshold.getProgress(), lockKneeAngleRateThreshold.getProgress(), lockTime.getProgress());
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    if (getDialog() != null) {
                        getDialog().cancel();
                    }
                });

        return builder.create();
    }
//TODO equations for these functions
    private void setupSeekBarListeners() {
        stepTiltThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                stepTiltThresholdValue.setText(String.format("%ddeg", progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Not needed */ }
        });

        stepTiltRateThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int f = progress;
                stepTiltRateThresholdValue.setText(String.format("%ddeg/sec", f));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Not needed */ }
        });

        lockTiltThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int p = progress - 20;
                lockTiltThresholdValue.setText(String.format("%ddeg", p));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Not needed */ }
        });

        lockKneeAngleThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int o = (progress);
                lockKneeAngleThresholdValue.setText(String.format("%ddeg", o));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Not needed */ }
        });

        lockKneeAngleRateThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int o = (progress);
                lockKneeAngleRateThresholdValue.setText(String.format("%ddeg/sec", o));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Not needed */ }
        });

        lockTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int o = progress + 500;
                lockTimeValue.setText(String.format("%dms", o));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Not needed */ }
        });
    }
}
