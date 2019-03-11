package com.codyy.robinsdk;

/**
 * Created by zhaisongqing on 2016/10/20.
 */
public class RBSize
{
    public RBSize()
    {
        mWidth = 0;
        mHeight = 0;
    }
    public RBSize(int width, int height)
    {
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setWidth(int width) {mWidth = width; }

    public void setHeight(int height) {mHeight = height; }

    private int mWidth;
    private int mHeight;
}
