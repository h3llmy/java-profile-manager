package com.example.profilemanager;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.profilemanager.Repository.UserRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private EditText inputUsername;
    private EditText inputEmail;
    private EditText inputOldPassword;
    private EditText inputNewPassword;
    private Button updateButton;
    private Button clearButton;

    private ImageView profileImage;

    private ActivityResultLauncher<String> mGetContent;
    private final UserRepository userRepository = new UserRepository(this);
    private String imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputUsername = findViewById(R.id.inputUsername);
        inputEmail = findViewById(R.id.inputEmail);
        inputOldPassword = findViewById(R.id.inputOldPassword);
        inputNewPassword = findViewById(R.id.inputNewPassword);
        updateButton = findViewById(R.id.addButton);
        clearButton = findViewById(R.id.clearButton);
        profileImage = findViewById(R.id.inputProfileImage);

        loadUserData(1);

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            // Load the original bitmap from the URI
                            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                            // Create a rounded bitmap from the original bitmap
                            Bitmap roundedBitmap = getRoundedBitmap(originalBitmap);

                            // Set the rounded bitmap to the ImageView
                            profileImage.setImageBitmap(roundedBitmap);

                            // Save the profile image to storage
                            imageUrl = saveImageToStorage(originalBitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        profileImage.setImageResource(R.mipmap.ic_launcher_foreground);
                    }
                });

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = inputUsername.getText().toString().trim();
                String email = inputEmail.getText().toString().trim();
                String oldPassword = inputOldPassword.getText().toString().trim();
                String newPassword = inputNewPassword.getText().toString().trim();

                if (!validateForm(username, email, oldPassword, newPassword)) {
                    Toast.makeText(MainActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isValidEmail(email)) {
                    Toast.makeText(MainActivity.this, "Email must be in correct format", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!checkOldPassword(1, oldPassword)) {
                    Toast.makeText(MainActivity.this, "Incorrect old password", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean updated = updateUserInformation(username, email, newPassword);
                if (updated) {
                    Toast.makeText(MainActivity.this, "User Updated", Toast.LENGTH_SHORT).show();
                    clearFields();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to update user", Toast.LENGTH_SHORT).show();
                }
            }
        });


        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean updated = userRepository.updateUserInfo(1, "", "", "", "");
                profileImage.setImageResource(R.mipmap.ic_launcher_foreground);
                inputUsername.setText("");
                inputEmail.setText("");
                Toast.makeText(MainActivity.this, "Clearing User Data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
        return email.matches(emailPattern);
    }

    private boolean updateUserInformation(String username, String email, String newPassword) {
        return userRepository.updateUserInfo(1, username, email, newPassword, imageUrl);
    }

    private void clearFields() {
        inputOldPassword.setText("");
        inputNewPassword.setText("");
    }

    private boolean validateForm(String username, String email, String oldPassword, String newPassword) {
        return !username.isEmpty() && !email.isEmpty() && !oldPassword.isEmpty() && !newPassword.isEmpty();
    }

    private Bitmap getRoundedBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int minEdge = Math.min(width, height);

        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(shader);

        Bitmap output = Bitmap.createBitmap(minEdge, minEdge, bitmap.getConfig());
        Canvas canvas = new Canvas(output);
        float radius = minEdge / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        return output;
    }

    private void openImagePicker() {
        mGetContent.launch("image/*");
    }

    private String saveImageToStorage(Bitmap bitmap) {
        File directory = new File(getExternalFilesDir(null), "MyAppImages");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, "profile_image_" + System.currentTimeMillis() + ".png");
        try {
            OutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Failed to save image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void loadUserData(int userId) {
        Cursor userData = userRepository.findOneById(userId);
        if (userData != null && userData.moveToFirst()) {
            String username = userData.getString(userData.getColumnIndexOrThrow(UserRepository.COLUMN_USERNAME));
            String email = userData.getString(userData.getColumnIndexOrThrow(UserRepository.COLUMN_EMAIL));
            String imagePath = userData.getString(userData.getColumnIndexOrThrow(UserRepository.COLUMN_IMAGE));

            inputUsername.setText(username);
            inputEmail.setText(email);

            if (imagePath != null && !imagePath.isEmpty()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                profileImage.setImageBitmap(getRoundedBitmap(bitmap));
            } else {
                profileImage.setImageResource(R.mipmap.ic_launcher_foreground);
            }
        } else {
            Log.d("USER DATA", "IS EMPTY");
            inputUsername.setText("");
            inputEmail.setText("");
            // Use default image bitmap
            profileImage.setImageResource(R.mipmap.ic_launcher_foreground);
        }
        if (userData != null) {
            userData.close();
        }
    }

    private boolean checkOldPassword(int userId, String oldPassword) {
        Cursor userData = userRepository.findOneById(userId);
        if (userData != null && userData.moveToFirst()) {
            String storedPassword = userData.getString(userData.getColumnIndexOrThrow(UserRepository.COLUMN_PASSWORD));
            userData.close();
            return storedPassword.equals(oldPassword);
        } else {
            return true;
        }
    }
}
