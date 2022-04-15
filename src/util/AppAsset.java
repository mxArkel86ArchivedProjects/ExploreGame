package util;

import java.awt.image.BufferedImage;

public class AppAsset {
    public AppAsset(BufferedImage resize, Size size) {
        this.source = resize;
        this.size = size;
    }
    public BufferedImage source;
    public Size size;
}
