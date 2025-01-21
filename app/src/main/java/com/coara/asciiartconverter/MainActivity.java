private void convertAsciiToImage(String filePath) {
    try {
        Uri uri = Uri.parse(filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
        StringBuilder asciiArt = new StringBuilder();
        String line;
        int maxWidth = 0;
        int lineCount = 0;

        Paint paint = new Paint();
        paint.setTextSize(40);  // 適切なフォントサイズに調整
        paint.setTypeface(Typeface.MONOSPACE);

        // TXTファイルからアスキーアートを読み込む
        while ((line = reader.readLine()) != null) {
            asciiArt.append(line).append("\n");
            maxWidth = Math.max(maxWidth, (int) paint.measureText(line));
            lineCount++;
        }
        reader.close();

        // アスキーアートの幅と高さを計算
        int charHeight = (int) (paint.getTextSize() + 10); // 行間を含めた高さ
        int width = maxWidth + 20; // 左右の余白を追加
        int height = lineCount * charHeight + 20; // 上下の余白を追加

        // アスキーアートからBitmapを作成
        Bitmap bitmap = createBitmapFromAscii(asciiArt.toString(), width, height, paint, charHeight);

        // 画像として保存
        saveBitmapAsPng(bitmap, uri);

        Toast.makeText(this, "変換と保存が完了しました", Toast.LENGTH_SHORT).show();

    } catch (Exception e) {
        Toast.makeText(this, "変換中にエラーが発生しました", Toast.LENGTH_SHORT).show();
    }
}

// アスキーアートをBitmapに変換するメソッド
private Bitmap createBitmapFromAscii(String asciiArt, int width, int height, Paint paint, int charHeight) {
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawColor(Color.WHITE); // 背景を白に設定

    String[] lines = asciiArt.split("\n");
    int y = charHeight;
    for (String line : lines) {
        canvas.drawText(line, 10, y, paint);
        y += charHeight;
    }

    return bitmap;
}
