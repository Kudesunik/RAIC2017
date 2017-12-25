
/**
 * Double 2-dimensional vector
 * <br>Code Wars contest 2017
 * 
 * @since 2017
 * @author Kunik
 */

public class Vector2d implements Cloneable {
	
	public double x;
	public double y;
	
	public Vector2d() {
		this(0, 0);
	}
	
	public Vector2d(double x, double y) {
		set(x, y);
	}
	
	public Vector2d(Vector2d v) {
		set(v.x, v.y);
	}
	
	public Vector2d set(double x, double y) {
		this.x = x;
		this.y = y;
		return this;
	}
	
	public double length() {
		return Math.sqrt(x * x + y * y);
	}
	
	public double distanceSquared(Vector2d v) {
		double dx = x - v.x;
		double dy = y - v.y;
		return dx * dx + dy * dy;
	}
	
	public double distance(Vector2d v) {
		return StrictMath.sqrt(distanceSquared(v));
	}
	
	public Vector2d add(Vector2d v) {
		return new Vector2d(x + v.x, y + v.y);
	}
	
	public Vector2d subtract(Vector2d v) {
		return new Vector2d(x - v.x, y - v.y);
	}
	
	public Vector2d addLocal(int x, int y) {
		this.x += x;
		this.y += y;
		return this;
	}
	
	public Vector2d subtractLocal(int x, int y) {
		this.x -= x;
		this.y -= y;
		return this;
	}
	
	@Override
	public Vector2d clone() {
		return new Vector2d(x, y);
	}
	
	@Override
	public String toString() {
		return String.format("(%4.10f, %4.10f)", x, y);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Vector2d other = (Vector2d) obj;
		if(Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if(Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}
}
