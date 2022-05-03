package templates;

import java.awt.image.BufferedImage;

public class ImageAsset {
    public ImageAsset(BufferedImage resize, Size size) {
        this.source = resize;
        this.size = size;
    }
    public BufferedImage source;
    public Size size;
}
