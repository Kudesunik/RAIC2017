
/**
 * Integer 2-dimensional vector
 * <br>Code Wars contest 2017
 * 
 * @since 2017
 * @author Kunik
 */

public class Vector2i implements Cloneable {
	
	public int x;
	public int y;
	
	public Vector2i() {
		this(0, 0);
	}
	
	public Vector2i(int x, int y) {
		set(x, y);
	}
	
	public Vector2i set(Vector2i v) {
		return set(v.x, v.y);
	}
	
	public Vector2i set(int x, int y) {
		this.x = x;
		this.y = y;
		return this;
	}
	
	public Vector2i add(int x, int y) {
		return new Vector2i(this.x + x, this.y + y);
	}
	
	public Vector2i subtract(int x, int y) {
		return new Vector2i(this.x - x, this.y - y);
	}
	
	public Vector2i addLocal(int x, int y) {
		this.x += x;
		this.y += y;
		return this;
	}
	
	public Vector2i subtractLocal(int x, int y) {
		this.x -= x;
		this.y -= y;
		return this;
	}
	
	public double length() {
		return Math.sqrt(x * x + y * y);
	}
	
	public double distanceSquared(Vector2i v) {
		double dx = x - v.x;
		double dy = y - v.y;
		return dx * dx + dy * dy;
	}
	
	public double distance(Vector2i vector2i) {
		return StrictMath.sqrt(distanceSquared(vector2i));
	}
	
	@Override
	public String toString() {
		return String.format("(%d, %d)", x, y);
	}
	
	@Override
	public Vector2i clone() {
		return new Vector2i(x, y);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
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
		Vector2i other = (Vector2i) obj;
		if(x != other.x)
			return false;
		if(y != other.y)
			return false;
		return true;
	}
}
