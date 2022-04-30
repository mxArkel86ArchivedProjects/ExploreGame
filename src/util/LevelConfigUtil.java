package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.awt.Color;

import gameObjects.Collider;
import gameObjects.ColorRect;
import gameObjects.GameObject;
import gameObjects.LevelTile;
import gameObjects.LevelWall;
import gameObjects.ResetBox;
import main.entry;

public class LevelConfigUtil {
    public static void saveLevel(){
        try {
            
            BufferedWriter w = new BufferedWriter(new FileWriter("output.txt"));
            w.write("-=-=-=-= output =-=-=-=-=-\n\n");
            w.write("[collision]\n");
            for(Collider c : entry.app.newColliders){
                w.write(String.format("%d,%d,%d,%d\n",(int)c.getX1(), (int)c.getY1(), (int)(c.getX2()),(int)(c.getY2())));
            }
            w.write("\n[walls]\n");
            for (LevelWall p : entry.app.newWalls) {
                    w.write(String.format("%s,%d,%d,%.3f\n", p.getAsset(), (int) p.left(), (int) p.top(), p.getZ()));
            }
            w.write("\n[tiles]\n");
            for(LevelTile p : entry.app.newTiles){
                    w.write(String.format("%s,%d,%d,%.3f\n",p.getAsset(), (int)p.left(), (int)p.top(), p.getZ()));
                
            }
            

            
            entry.app.newColliders.clear();
            entry.app.newWalls.clear();
            entry.app.newTiles.clear();
            w.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadLevel(Map<String, Size> assetsizes){
        entry.app.colliders.clear();
        entry.app.colors.clear();
        //entry.app.objects.clear();
        entry.app.checkpoints.clear();

        BufferedReader reader;
        String category = "";
		try {
			reader = new BufferedReader(new FileReader("level.txt"));
			String line = reader.readLine();
            
            while (line != null) {
                String ln = line;
                
                line = reader.readLine();
                if(ln.startsWith("[")){
                    category = ln.substring(1, ln.length()-1);
                    continue;
                }
                if(category.length()==0)
                    continue;
                if(ln.length()==0)
                    continue;
                if(ln.startsWith("#"))
                    continue;
                
                String[] args = ln.split(",");
                if(category.contentEquals("collision")){
                   double x1 = Double.parseDouble(args[0]);
                   double y1 = Double.parseDouble(args[1]);
                   double x2 = Double.parseDouble(args[2]);
                   double y2 = Double.parseDouble(args[3]);

                   double x_1 = Math.min(x1, x2);
                   double y_1 = Math.min(y1, y2);
                   double x_2 = Math.max(x1, x2);
                   double y_2 = Math.max(y1, y2);
                    
                   Collider c = new Collider(x_1, y_1, x_2, y_2);
                   entry.app.colliders.add(c);
                // }else if(category.contentEquals("colorrect")){
                //     String color = args[0];
                //     double x1 = Double.parseDouble(args[1]);
                //     double y1 = Double.parseDouble(args[2]);
                //     double x2 = Double.parseDouble(args[3]);
                //     double y2 = Double.parseDouble(args[4]);
                //     double z = Double.parseDouble(args[5]);
 
                //     double x = Math.min(x1, x2);
                //     double y = Math.min(y1, y2);
                //     double w = Math.abs(x1-x2);
                //     double h = Math.abs(y1-y2);
                //     ColorRect c = new ColorRect(x, y, w, h, z, color);
                //     entry.app.add(c);
                 }else if(category.contentEquals("color")){
                    String color = args[0];
                    String hex = "#"+ args[1];

                    Color c = Color.decode(hex);
                    entry.app.colors.put(color, c);
                } else if (category.contentEquals("walls")) {
                    String name = args[0];
                    double x = Double.parseDouble(args[1]);
                    double y = Double.parseDouble(args[2]);
                    double z = Double.parseDouble(args[3]);
                    Size s = assetsizes.get(name);

                    LevelWall c = new LevelWall(x, y, s.getWidth(), s.getHeight(), z, name);
                    entry.app.walls.add(c);
                } else if (category.contentEquals("tiles")) {
                        String name = args[0];
                        double x = Double.parseDouble(args[1]);
                        double y = Double.parseDouble(args[2]);
                        double z = Double.parseDouble(args[3]);
                        Size s = assetsizes.get(name);
                        
                        LevelTile c = new LevelTile(x, y, s.getWidth(), s.getHeight(), z, name);
                        entry.app.tiles.add(c);
                 } else if(category.contentEquals("checkpoint")){
                    String name = args[0];
                    double x1 = Double.parseDouble(args[1]);
                    double y1 = Double.parseDouble(args[2]);
                    Point p =  new Point(x1, y1);
                    entry.app.checkpoints.put(name, p);
                 }else if(category.contentEquals("resetbox")){
                    String name = args[0];
                    double x1 = Double.parseDouble(args[1]);
                    double y1 = Double.parseDouble(args[2]);
                    double x2 = Double.parseDouble(args[3]);
                    double y2 = Double.parseDouble(args[4]);
                    
                    double x = Math.min(x1, x2);
                    double y = Math.min(y1, y2);
                    double w = Math.abs(x1-x2);
                    double h = Math.abs(y1-y2);
                    ResetBox b = new ResetBox(x, y, w, h, name);
                    entry.app.resetboxes.add(b);
                 }


				
			}
            reader.close();

        }catch(IOException e){
            e.printStackTrace();
        }
        
    }
}
