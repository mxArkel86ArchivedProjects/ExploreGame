package util;

import java.util.HashMap;

import main.AppConstants;
import java.awt.image.BufferedImage;

import templates.ImageAsset;
import templates.Size;
import main.entry;

public class AssetLoader {
    public static HashMap<String, Size> LoadAssets() {
        HashMap<String, Size> assetSizes = new HashMap<>();

        for (String name : new String[] { "brick", "grass512", "grass256", "stump", "wood", "img1", "storage_device",
                "dirtygrass" }) {
            String filename = name + ".png";
            BufferedImage img = ImageUtil.getImage(filename);
            if (img == null)
                continue;
            Size size = new Size(img.getWidth() / AppConstants.PIXELS_PER_GRID_IMPORT(),
                    img.getHeight() / AppConstants.PIXELS_PER_GRID_IMPORT());
            assetSizes.put(name,
                    size);
            BufferedImage resize = ImageUtil.resize(img,
                    (int) (img.getWidth() * AppConstants.PIXELS_RESIZE * 1.0f
                            / AppConstants.PIXELS_PER_GRID_IMPORT()),
                    (int) (img.getHeight() * AppConstants.PIXELS_RESIZE * 1.0f
                            / AppConstants.PIXELS_PER_GRID_IMPORT()));
            entry.app.assets.put(name, new ImageAsset(resize, size));
        }
        return assetSizes;
    }
}
