package com.sformica.torchlight.shaker.camera;

import android.content.Context;

/**
 * Created by Chris on 11.12.2016.
 */

public interface ICamera {
    void init(Context context);
    boolean toggle(boolean enable);
    void release();
}
