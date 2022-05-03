package gameObjects;

public class LevelTile extends DepthObject {
    String asset;
	
	public LevelTile(double x, double y, double width, double height, double depth, String asset) {
		super(x, y, width, height, depth);
		
		this.asset = asset;
	}

	public String getAsset(){
		return asset;
	}

}
