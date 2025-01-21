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
import java.util.Calendar;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_SELECT_FILE = 1;
    private static final int REQUEST_CODE_PERMISSION = 2;
    private String selectedFilePath;
    private boolean isTxtFile = false;
    private boolean isDatFile = false;
    
    private TextView filePathView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button selectTxtFileButton = findViewById(R.id.selectTxtFileButton);
        Button selectColorFileButton = findViewById(R.id.selectColorFileButton);
        Button convertTxtButton = findViewById(R.id.convertTxtButton);
        Button convertRgbButton = findViewById(R.id.convertRgbButton);
        filePathView = findViewById(R.id.filePathView);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
        }

        selectTxtFileButton.setOnClickListener(v -> {
            isTxtFile = true;
            isDatFile = false;
            selectFile();
        });

        selectColorFileButton.setOnClickListener(v -> {
            isTxtFile = false;
            isDatFile = true;
            selectFile();
        });

        convertTxtButton.setOnClickListener(v -> {
            if (selectedFilePath != null && isTxtFile) {
                convertAsciiToImage(selectedFilePath);
            } else {
                Toast.makeText(this, "TXTファイルを選択してください", Toast.LENGTH_SHORT).show();
            }
        });

        convertRgbButton.setOnClickListener(v -> {
            if (selectedFilePath != null && isDatFile) {
                convertDatToImage(selectedFilePath);
            } else {
                Toast.makeText(this, "DATファイルを選択してください", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(isTxtFile ? "text/plain" : "application/octet-stream");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                selectedFilePath = uri.toString();
                filePathView.setText(selectedFilePath);

                // ボタンの有効化
                Button convertTxtButton = findViewById(R.id.convertTxtButton);
                Button convertRgbButton = findViewById(R.id.convertRgbButton);
                convertTxtButton.setEnabled(isTxtFile);
                convertRgbButton.setEnabled(isDatFile);
            }
        }
    }

    private void convertAsciiToImage(String filePath) {
        
    }

    private void convertDatToImage(String filePath) {
        try {
            Uri uri = Uri.parse(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            StringBuilder datContent = new StringBuilder();
            String line;
            int maxWidth = 0;
            int lineCount = 0;

            
            while ((line = reader.readLine()) != null) {
                datContent.append(line).append("\n");
                maxWidth = Math.max(maxWidth, line.length());
                lineCount++;
            }
            reader.close();

            int charHeight = 40;  // 仮の高さ。DATの内容に合わせて動的に調整
            int width = maxWidth + 20;
            int height = lineCount * charHeight + 20;

            Bitmap bitmap = createBitmapFromDat(datContent.toString(), width, height, charHeight);
            saveBitmapAsPng(bitmap, uri);

            Toast.makeText(this, "変換と保存が完了しました", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "変換中にエラーが発生しました", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap createBitmapFromDat(String datContent, int width, int height, int charHeight) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);  // 背景を白に設定

        Paint paint = new Paint();
        paint.setTextSize(40);  

        String[] lines = datContent.split("\n");
        int y = charHeight;
        for (String line : lines) {
            canvas.drawText(line, 10, y, paint);
            y += charHeight;
        }

        return bitmap;
    }

    private void saveBitmapAsPng(Bitmap bitmap, Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);
            File outputDir = getExternalFilesDir(null);
            if (outputDir != null) {
                File outputFile = new File(outputDir, fileName + "_" + getCurrentTime() + ".png");
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

    private String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return String.format("%02d%02d%02d%02d%02d", month, day, hour, minute, second);
    }
}
