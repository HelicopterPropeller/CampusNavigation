package com.example.campusnavigation;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class EditDialogFragment extends androidx.fragment.app.DialogFragment {

    public interface OnConfirmListener {
        void onConfirmed(String name);
    }

    private OnConfirmListener listener;

    public void setOnConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
    }

    @Override
    public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = requireActivity()
                .getLayoutInflater()
                .inflate(R.layout.fragment_edit, null);

        EditText editName = view.findViewById(R.id.editName);

        return new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(view)
                .setPositiveButton("确定", (dialog, which) -> {
                    if (listener != null) {
                        listener.onConfirmed(
                                editName.getText().toString().trim()
                        );
                    }
                })
                .setNegativeButton("取消", null)
                .create();
    }
}
