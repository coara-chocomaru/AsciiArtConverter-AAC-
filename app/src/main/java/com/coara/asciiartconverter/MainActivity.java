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
import java.util.Calendar;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_SELECT_FILE = 1;
    private static final int REQUEST_CODE_PERMISSION = 2;
    private static final String TAG = "ASCIIArtConverter";

    private TextView filePathView;
    private String selectedFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button selectTxtFileButton = findViewById(R.id.selectTxtFileButton);
        Button selectColorFileButton = findViewById(R.id.selectColorFileButton);
        Button convertTxtButton = findViewById(R.id.convertTxtButton);
        Button convertRgbButton = findViewById(R.id.convertRgbButton);
        filePathView = findViewById(R.id.filePathView);

        // パーミッション確認
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
        }

        // TXTファイル選択
        selectTxtFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("text/plain");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
        });

        // RGBカラーDATファイル選択
        selectColorFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/octet-stream"); // .datファイルを選択する
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
        });

        // TXTから画像変換
        convertTxtButton.setOnClickListener(v -> {
            if (selectedFilePath != null) {
                convertAsciiToImage(selectedFilePath);
            } else {
                Toast.makeText(this, "テキストファイルを選択してください", Toast.LENGTH_SHORT).show();
            }
        });

        // RGBカラーDATから画像復元
        convertRgbButton.setOnClickListener(v -> {
            if (selectedFilePath != null) {
                convertRgbToImage(selectedFilePath);
            } else {
                Toast.makeText(this, "RGBカラーDATファイルを選択してください", Toast.LENGTH_SHORT).show();
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

    // TXTから画像に変換する処理
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

            // テキストの幅と高さを計算
            int charHeight = (int) (paint.getTextSize() + 10); // 行間を含めた高さ
            int width = maxWidth + 20; // 左右の余白を追加
            int height = lineCount * charHeight + 20; // 上下の余白を追加

            // 画像生成
            Bitmap bitmap = createBitmapFromAscii(asciiArt.toString(), width, height, paint, charHeight);
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

    
    private void convertRgbToImage(String filePath) {
        try {
            Uri uri = Uri.parse(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            StringBuilder colorData = new StringBuilder();
            String line;
            int width = 0;
            int height = 0;
            while ((line = reader.readLine()) != null) {
                colorData.append(line).append("\n");
                width = line.length(); // 仮で1行の幅を画像の幅に設定
                height++;
            }
            reader.close();

            // 画像作成
            Bitmap bitmap = createBitmapFromRgb(colorData.toString(), width, height);
            saveBitmapAsPng(bitmap, uri);

            Toast.makeText(this, "RGBカラーDATから画像に復元しました", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "RGBカラーDATから画像変換中にエラーが発生しました", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap createBitmapFromRgb(String colorData, int width, int height) {
        String[] lines = colorData.split("\n");
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < lines.length; y++) {
            String line = lines[y];
            for (int x = 0; x < line.length(); x++) {
                char ch = line.charAt(x);
                int color = getColorFromChar(ch);
                bitmap.setPixel(x, y, color);
            }
        }
        return bitmap;
    }

    private int getColorFromChar(char ch) {
        // 色の設定：ここでは簡単な例として、ASCIIコードを色に変換
        int colorValue = (ch * 1000) % 0xFFFFFF; // 適当なカラーコードに変換
        return colorValue | 0xFF000000; // アルファ値を255に設定
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
