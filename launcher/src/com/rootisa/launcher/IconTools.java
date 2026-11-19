package com.rootisa.launcher;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;

/** Icon helpers: rasterize, squircle-mask (iOS-like), and load custom icons. */
public final class IconTools {

    private IconTools() {}

    public static Bitmap toBitmap(Drawable d, int size) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, size, size);
        d.draw(c);
        return bmp;
    }

    /** Mask any drawable into a rounded "squircle" tile of the given size. */
    public static Drawable squircle(Resources res, Drawable src, int size) {
        if (src == null) return null;
        Bitmap scaled = toBitmap(src, size);
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFFFFFFF);
        float r = size * 0.28f; // iOS-ish corner radius
        c.drawRoundRect(new RectF(0, 0, size, size), r, r, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        c.drawBitmap(scaled, 0, 0, p);
        return new BitmapDrawable(res, out);
    }

    /** Load a user-chosen icon file, sized and (optionally) squircled. */
    public static Drawable fromFile(Resources res, String path, int size, boolean squircle) {
        try {
            File f = new File(path);
            if (!f.exists()) return null;
            Bitmap bmp = BitmapFactory.decodeFile(path);
            if (bmp == null) return null;
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, size, size, true);
            Drawable d = new BitmapDrawable(res, scaled);
            return squircle ? squircle(res, d, size) : d;
        } catch (Throwable t) {
            return null;
        }
    }
}
