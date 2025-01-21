package com.coara.asciiartconverter;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_SELECT_FILE = 1;
    private static final int REQUEST_CODE_SELECT_DAT_FILE = 2;
    private static final int REQUEST_CODE_PERMISSION = 3;

    private TextView filePathView;
    private String selectedFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button selectFileButton = findViewById(R.id.selectFileButton);
        Button convertTxtToImageButton = findViewById(R.id.convertTxtToImageButton);
        Button selectDatFileButton = findViewById(R.id.selectDatFileButton);
        Button convertDatToImageButton = findViewById(R.id.convertDatToImageButton);
        filePathView = findViewById(R.id.filePathView);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
        }

        selectFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("text/plain");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
        });

        convertTxtToImageButton.setOnClickListener(v -> {
            if (selectedFilePath != null) {
                convertAsciiToImage(selectedFilePath);
            } else {
                Toast.makeText(this, "テキストファイルを選択してください", Toast.LENGTH_SHORT).show();
            }
        });

        selectDatFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/octet-stream");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_CODE_SELECT_DAT_FILE);
        });

        convertDatToImageButton.setOnClickListener(v -> {
            if (selectedFilePath != null) {
                convertDatToImage(selectedFilePath);
            } else {
                Toast.makeText(this, "DATファイルを選択してください", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                selectedFilePath = uri.toString();
                filePathView.setText(selectedFilePath);
            }
        }
    }

    private void convertAsciiToImage(String filePath) {
        // TXT -> 画像への変換処理
        // （既存のアスキーアート変換処理を利用）
    }

    private void convertDatToImage(String filePath) {
        // DAT -> 画像への変換処理
        try {
            Uri uri = Uri.parse(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String line;
            int width = 0, height = 0;
            // Read lines to determine width and height
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String[] coordinates = parts[0].split(",");
                    String[] rgb = parts[1].split(",");
                    if (Integer.parseInt(coordinates[0]) > width) {
                        width = Integer.parseInt(coordinates[0]) + 1;
                    }
                    if (Integer.parseInt(coordinates[1]) > height) {
                        height = Integer.parseInt(coordinates[1]) + 1;
                    }
                }
            }
            reader.close();

            // Create bitmap from width and height
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE); // 背景を白に設定

            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String[] coordinates = parts[0].split(",");
                    String[] rgb = parts[1].split(",");
                    int x = Integer.parseInt(coordinates[0]);
                    int y = Integer.parseInt(coordinates[1]);
                    int r = Integer.parseInt(rgb[0]);
                    int g = Integer.parseInt(rgb[1]);
                    int b = Integer.parseInt(rgb[2]);

                    // Set pixel
                    bitmap.setPixel(x, y, Color.rgb(r, g, b));
                }
            }
            reader.close();

            // Save the bitmap as a PNG
            saveBitmapAsPng(bitmap, uri);
            Toast.makeText(this, "DATから画像への変換が完了しました", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "DAT変換中にエラーが発生しました", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBitmapAsPng(Bitmap bitmap, Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);
            File outputDir = getExternalFilesDir(null);
            if (outputDir != null) {
                File outputFile = new File(outputDir, fileName + ".png");
                FileOutputStream out = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();

                Toast.makeText(this, "画像が保存されました: " + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "画像の保存中にエラーが発生しました", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "";
        ContentResolver contentResolver = getContentResolver();
        try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                fileName = cursor.getString(nameIndex);
            }
        }
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }
}
