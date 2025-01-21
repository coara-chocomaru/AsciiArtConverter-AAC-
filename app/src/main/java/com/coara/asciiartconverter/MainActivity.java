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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_SELECT_FILE = 1;
    private static final int REQUEST_CODE_SELECT_COLOR_DAT = 2;
    private static final int REQUEST_CODE_PERMISSION = 3;

    private TextView filePathView;
    private String selectedFilePath;
    private String selectedColorFilePath;

    private Button convertButton;
    private Button convertWithColorButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button selectFileButton = findViewById(R.id.selectFileButton);
        Button selectColorFileButton = findViewById(R.id.selectColorFileButton);
        convertButton = findViewById(R.id.convertButton);
        convertWithColorButton = findViewById(R.id.convertWithColorButton);
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

        selectColorFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/octet-stream");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_CODE_SELECT_COLOR_DAT);
        });

        convertButton.setOnClickListener(v -> {
            if (selectedFilePath != null) {
                convertAsciiToImage(selectedFilePath);
            } else {
                Toast.makeText(this, "テキストファイルを選択してください", Toast.LENGTH_SHORT).show();
            }
        });

        convertWithColorButton.setOnClickListener(v -> {
            if (selectedFilePath != null && selectedColorFilePath != null) {
                convertAsciiToImageWithColor(selectedFilePath, selectedColorFilePath);
            } else {
                Toast.makeText(this, "テキストファイルとカラーDATファイルを選択してください", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (requestCode == REQUEST_CODE_SELECT_FILE) {
                selectedFilePath = uri.toString();
                filePathView.setText("選択されたテキストファイル: " + selectedFilePath);
            } else if (requestCode == REQUEST_CODE_SELECT_COLOR_DAT) {
                selectedColorFilePath = uri.toString();
                filePathView.setText("選択されたカラーDATファイル: " + selectedColorFilePath);
            }

            updateConvertButtonState();
        }
    }

    private void updateConvertButtonState() {
        if (selectedFilePath != null && selectedColorFilePath == null) {
            convertButton.setEnabled(true);
            convertWithColorButton.setEnabled(false);
        } else if (selectedFilePath != null && selectedColorFilePath != null) {
            convertButton.setEnabled(false);
            convertWithColorButton.setEnabled(true);
        } else {
            convertButton.setEnabled(false);
            convertWithColorButton.setEnabled(false);
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

            saveBitmapAsPng(bitmap);

            Toast.makeText(this, "変換と保存が完了しました", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "変換中にエラーが発生しました", Toast.LENGTH_SHORT).show();
        }
    }

    private void convertAsciiToImageWithColor(String txtFilePath, String colorFilePath) {
        try {
            Uri txtUri = Uri.parse(txtFilePath);
            BufferedReader txtReader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(txtUri)));
            StringBuilder asciiArt = new StringBuilder();
            String line;
            int maxWidth = 0;
            int lineCount = 0;

            Paint paint = new Paint();
            paint.setTextSize(40);
            paint.setTypeface(Typeface.MONOSPACE);

            while ((line = txtReader.readLine()) != null) {
                asciiArt.append(line).append("\n");
                maxWidth = Math.max(maxWidth, (int) paint.measureText(line));
                lineCount++;
            }
            txtReader.close();

            int charHeight = (int) (paint.getTextSize() + 10);
            int width = maxWidth + 20;
            int height = lineCount * charHeight + 20;

            Bitmap bitmap = createBitmapFromAscii(asciiArt.toString(), width, height, paint, charHeight);

            // カラーDATが存在する場合は画像をリサイズし、カラーを適用
            if (colorFilePath != null) {
                applyColorFromDatFile(bitmap, colorFilePath, width, height);
            }

            saveBitmapAsPng(bitmap);

            Toast.makeText(this, "カラー変換と保存が完了しました", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "変換中にエラーが発生しました", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyColorFromDatFile(Bitmap bitmap, String colorFilePath, int width, int height) {
        try {
            BufferedReader colorReader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(Uri.parse(colorFilePath))));
            String line;

            // カラーDATの幅と高さを動的に取得
            int originalWidth = 0;
            int originalHeight = 0;
            while ((line = colorReader.readLine()) != null) {
                originalHeight++;
                String[] parts = line.split(":");
                String[] coords = parts[0].split(",");
                originalWidth = Math.max(originalWidth, Integer.parseInt(coords[0]) + 1);
            }
            colorReader.close();

            // 拡大率を計算
            float scaleX = (float) width / originalWidth;
            float scaleY = (float) height / originalHeight;

            // 再度カラーDATを読み込み、カラーを適用
            colorReader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(Uri.parse(colorFilePath))));
            while ((line = colorReader.readLine()) != null) {
                String[] parts = line.split(":");
                String[] coords = parts[0].split(",");
                int originalX = Integer.parseInt(coords[0]);
                int originalY = Integer.parseInt(coords[1]);
                String[] rgb = parts[1].split(",");
                int red = Integer.parseInt(rgb[0]);
                int green = Integer.parseInt(rgb[1]);
                int blue = Integer.parseInt(rgb[2]);

                // 新しい画像サイズに合わせて座標をスケーリング
                float newX = originalX * scaleX;
                float newY = originalY * scaleY;

                // カラーを適用
                int color = Color.rgb(red, green, blue);
                bitmap.setPixel((int) newX, (int) newY, color);
            }
            colorReader.close();
        } catch (Exception e) {
            Toast.makeText(this, "カラーDATファイルの処理中にエラーが発生しました", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private Bitmap createBitmapFromAscii(String asciiArt, int width, int height, Paint paint, int charHeight) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        int y = charHeight;
        String[] lines = asciiArt.split("\n");
        for (String line : lines) {
            canvas.drawText(line, 10, y, paint);
            y += charHeight;
        }

        return bitmap;
    }

    private void saveBitmapAsPng(Bitmap bitmap) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());
            File file = new File(getExternalFilesDir(null), "ascii_" + timestamp + ".png");

            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            Toast.makeText(this, "画像を保存しました: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "画像の保存に失敗しました", Toast.LENGTH_SHORT).show();
        }
    }
}
