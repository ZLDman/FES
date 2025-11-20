package com.example.sco;

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
        void onFinishEditDialog(int amplitude, int frequency, int power, int on, int off);
    }

    private SeekBar ampBar;
    private TextView ampValue;
    private SeekBar freqBar;
    private TextView freqValue;
    private SeekBar powBar;
    private TextView powValue;
    private SeekBar onBar;
    private TextView onValue;
    private SeekBar offBar;
    private TextView offValue;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_config, null);

        // --- Find views from the new dialog layout ---
        ampBar = view.findViewById(R.id.dialogAmpBar);
        ampValue = view.findViewById(R.id.dialogAmpValue);
        freqBar = view.findViewById(R.id.dialogFreqBar);
        freqValue = view.findViewById(R.id.dialogFreqValue);
        powBar = view.findViewById(R.id.dialogPowBar);
        powValue = view.findViewById(R.id.dialogPowValue);
        onBar = view.findViewById(R.id.dialogOnSetBar);
        onValue = view.findViewById(R.id.dialogOnSetValue);
        offBar = view.findViewById(R.id.dialogOffSetBar);
        offValue = view.findViewById(R.id.dialogOffSetValue);

        // --- Set up SeekBar listeners to update the text views ---
        setupSeekBarListeners();

        // --- Retrieve and display current values passed from the activity ---
        Bundle args = getArguments();
        if (args != null) {
            int currentAmplitude = args.getInt("amplitude");
            int currentFrequency = args.getInt("frequency");
            int currentPower = args.getInt("power");
            int currentOn = args.getInt("on");
            int currentOff = args.getInt("off");

            ampBar.setProgress(currentAmplitude);
            ampValue.setText(String.format("%dmA", currentAmplitude));

            freqBar.setProgress(currentFrequency - 30);
            freqValue.setText(String.format("%dHz", currentFrequency));

            powBar.setProgress((currentPower - 250)/5);
            powValue.setText(String.format("%dHz", currentPower));

            onBar.setProgress(currentOn + 5);
            onValue.setText(String.format("%d", currentOn));

            offBar.setProgress(currentOff + 5);
            offValue.setText(String.format("%d", currentOff));
        }

        // --- Build the dialog ---
        builder.setView(view)
                .setTitle("Edit Configuration")
                .setPositiveButton("Save", (dialog, id) -> {
                    // Send the final progress values back to the activity
                    EditConfigDialogListener listener = (EditConfigDialogListener) getActivity();
                    if (listener != null) {
                        listener.onFinishEditDialog(ampBar.getProgress(), freqBar.getProgress(), powBar.getProgress(), onBar.getProgress(), offBar.getProgress());
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> {
                    if (getDialog() != null) {
                        getDialog().cancel();
                    }
                });

        return builder.create();
    }

    private void setupSeekBarListeners() {
        ampBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ampValue.setText(String.format("%dmA", progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Not needed */ }
        });

        freqBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int f = progress + 30;
                freqValue.setText(String.format("%dHz", f));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Not needed */ }
        });

        powBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int p = (progress * 5) + 250;
                powValue.setText(String.format("%dHz", p));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Not needed */ }
        });

        onBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int o = (progress - 5);
                onValue.setText(String.format("%d", o));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Not needed */ }
        });

        offBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int o = (progress - 5);
                offValue.setText(String.format("%d", o));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* Not needed */ }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Not needed */ }
        });
    }
}
