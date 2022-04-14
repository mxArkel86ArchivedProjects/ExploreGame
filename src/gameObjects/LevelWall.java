package gameObjects;

public class LevelWall extends GameObject {
	String asset;
	
	public LevelWall(double x, double y, double width, double height, double depth, String asset) {
		super(x, y, width, height, depth);
		
		this.asset = asset;
	}

	public String getAsset(){
		return asset;
	}

}
