package envStructures;
/**
 * 
 * @author Clara Rigaud
 *
 */
public class Vec2 {

	public float x;
	public float y;
	/**
	 * Constructor
	 */
	public Vec2(){
		this.x = 0.0f;
		this.y = 0.0f;
	}
	public Vec2(float d, float e ){
		this.x =d;
		this.y =e;
	}
/**
 * 	
 * @param b Vector to add
 * @return Returns the addition of the two vectors
 */
	public Vec2 add(Vec2 b){
		return new Vec2(this.x+b.x, this.y+b.y);
	}
	
	/**
	 * 	
	 * @param b Vector to substract
	 * @return Returns the substraction of the two vectors
	 */
	public Vec2 sub(Vec2 b){
		return new Vec2(this.x-b.x, this.y-b.y);
	}
	/**
	 * 	
	 * @param b Vector to make the scalar product with
	 * @return Returns the scalar product
	 */
	public float dot(Vec2 b){
		return this.x*b.x + this.y*b.y;
	}
	/**
	 * 	
	 * @param dist Scalar to multiply
	 * @return Returns the product of the scalar and the vector
	 */
	public Vec2 mul(float dist){
		return new Vec2(this.x*dist, this.y*dist);
	}
	
	/**
	 * 
	 * @param p Point to project on line
	 * @param a First point on the line
	 * @param b Second point on the line
	 * @return Return the coordinates of the orthogonal project of the point p on the line defined by a and b
	 */
	public static Vec2 projectPointOnLine(Vec2 p, Vec2 a, Vec2 b){
		float U = (p.sub(a)).dot(b.sub(a));
		U/= (Math.pow(b.x-a.x, 2)+Math.pow(b.y-a.y, 2));
		Vec2 R = a.add(b.sub(a).mul(U));
		return R;
	}
	/**
	 * 
	 * @return Gives the orthogonal vector
	 */
	public Vec2 normale(){
		return new Vec2(this.y, -this.x);
	}
	/**
	 * 
	 * @return the normalized vector
	 */
	public Vec2 normalize(){
		return new Vec2(this.x/this.length(), this.y/this.length());
	}
	/**
	 * 
	 * @param a first vector
	 * @param b second vector
	 * @return Computes the angle between vector a and vector b return a value between 0 and 2*PI
	 */
	public static float angle(Vec2 a, Vec2 b){
		double at1 =  Math.atan2((double) a.x, (double) a.y);
		double at2 =  Math.atan2((double) b.x, (double) b.y);
		return (float) ((float)((at2-at1)+(2*Math.PI))%(2*Math.PI));
	}
	
	/**
	 * 
	 * @return the length of the vector
	 */
	public float length(){
		return (float) Math.sqrt(Math.pow(this.x,2)+  Math.pow(this.y,2));
	}
	public double getX() {
		return x;
	}
	public void setX(float x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(float y) {
		this.y = y;
	}
	@Override
	public String toString() {
		return "Vec2 [x=" + x + ", y=" + y + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(x);
		result = prime * result + Float.floatToIntBits(y);
		return result;
	}
	public boolean equals(Vec2 v) {
		if(this.x==v.getX() && this.y==v.getY()) {
			return true;
		}
		return false;
	}
	
}
