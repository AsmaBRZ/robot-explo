package envStructures;
/**
 * 
 * @author Clara Rigaud 
 * @author Asma
 */
public class Point{
	private Vec2 position;
	public Point(){
		this.position = new Vec2();
	}
	public Point(float x, float y){
		this.position = new Vec2(x,y);
	}
	public Point(Vec2 p){
		this.position = p;
	}
	public void setPosition(float x, float y){
		this.position.x = x;
		this.position.y = y;
	}
	
	public void setPosition(Vec2 v){
		this.position = v;
	}
	
	public Vec2 position(){
		return this.position;
	}
	@Override
	public String toString() {
		return "Point [position=" + position.toString() + "]";
	}
	public Vec2 getPosition() {
		return position;
	}
	
	
}
