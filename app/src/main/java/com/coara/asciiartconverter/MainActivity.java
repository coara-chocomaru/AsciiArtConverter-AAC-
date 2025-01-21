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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_SELECT_FILE = 1;
    private static final int REQUEST_CODE_PERMISSION = 2;
    private static final String TAG = "ASCIIArtConverter";

    private TextView filePathView;
    private String selectedFilePath;
    private Button convertButton, revertButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button selectFileButton = findViewById(R.id.selectFileButton);
        convertButton = findViewById(R.id.convertButton);
        revertButton = findViewById(R.id.revertButton);
        filePathView = findViewById(R.id.filePathView);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
        }

        selectFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
        });

        convertButton.setOnClickListener(v -> {
            if (selectedFilePath != null && selectedFilePath.endsWith(".txt")) {
                convertAsciiToImage(selectedFilePath);
            }
        });

        revertButton.setOnClickListener(v -> {
            if (selectedFilePath != null && selectedFilePath.endsWith(".dat")) {
                convertDatToImage(selectedFilePath);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                selectedFilePath = uri.toString();
                filePathView.setText(selectedFilePath);
                
                if (selectedFilePath.endsWith(".txt")) {
                    convertButton.setEnabled(true);
                    revertButton.setEnabled(false);
                } else if (selectedFilePath.endsWith(".dat")) {
                    convertButton.setEnabled(false);
                    revertButton.setEnabled(true);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "ストレージ権限が許可されました", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "ストレージ権限が必要です", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void convertAsciiToImage(String filePath) {
        try {
            Uri uri = Uri.parse(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            StringBuilder asciiArt = new StringBuilder();
            String line;
            int maxWidth = 0;
            int lineCount = 0;

            Paint paint = new Paint();
            paint.setTextSize(40);
            paint.setTypeface(Typeface.MONOSPACE);

            while ((line = reader.readLine()) != null) {
                asciiArt.append(line).append("\n");
                maxWidth = Math.max(maxWidth, (int) paint.measureText(line));
                lineCount++;
            }
            reader.close();

            int charHeight = (int) (paint.getTextSize() + 10);
            int width = maxWidth + 20;
            int height = lineCount * charHeight + 20;

            Bitmap bitmap = createBitmapFromAscii(asciiArt.toString(), width, height, paint, charHeight);
            saveBitmapAsPng(bitmap, uri);

            Toast.makeText(this, "変換と保存が完了しました", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "変換中にエラーが発生しました", Toast.LENGTH_SHORT).show();
        }
    }

    
    private void convertDatToImage(String filePath) {
        try {
            Uri uri = Uri.parse(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String line;
            List<int[]> pixelData = new ArrayList<>();
            int width = 0;
            int height = 0;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                String[] position = parts[0].split(",");
                String[] rgb = parts[1].split(",");
                int x = Integer.parseInt(position[0]);
                int y = Integer.parseInt(position[1]);
                int r = Integer.parseInt(rgb[0]);
                int g = Integer.parseInt(rgb[1]);
                int b = Integer.parseInt(rgb[2]);
                pixelData.add(new int[]{x, y, r, g, b});
                width = Math.max(width, x);
                height = Math.max(height, y);
            }
            reader.close();

            width += 1; // 0-indexedのため+1
            height += 1;

            Bitmap bitmap = createBitmapFromDat(pixelData, width, height);
            saveBitmapAsPng(bitmap, uri);

            Toast.makeText(this, "変換と保存が完了しました", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "変換中にエラーが発生しました", Toast.LENGTH_SHORT).show();
        }
    }

    
    private Bitmap createBitmapFromAscii(String asciiArt, int width, int height, Paint paint, int charHeight) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        String[] lines = asciiArt.split("\n");
        int y = charHeight;
        for (String line : lines) {
            canvas.drawText(line, 10, y, paint);
            y += charHeight;
        }

        return bitmap;
    }

    /
    private Bitmap createBitmapFromDat(List<int[]> pixelData, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        for (int[] pixel : pixelData) {
            int x = pixel[0];
            int y = pixel[1];
            int r = pixel[2];
            int g = pixel[3];
            int b = pixel[4];
            int color = Color.rgb(r, g, b);
            bitmap.setPixel(x, y, color);
        }

        return bitmap;
    }

    
    private void saveBitmapAsPng(Bitmap bitmap, Uri uri) {
        try {
            String fileName = generateFileName();

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


    private String generateFileName() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMdd_HHmmss");
        java.util.Date now = new java.util.Date();
        return sdf.format(now);
    }
}
